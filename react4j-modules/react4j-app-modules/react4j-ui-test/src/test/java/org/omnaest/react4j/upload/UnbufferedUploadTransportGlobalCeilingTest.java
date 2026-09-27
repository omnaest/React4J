package org.omnaest.react4j.upload;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.omnaest.react4j.EnableReactUI;
import org.omnaest.react4j.component.form.upload.ByteArrayChannel;
import org.omnaest.react4j.component.form.upload.UploadChannel;
import org.omnaest.react4j.data.annotations.EnableReactUIInMemoryRepository;
import org.omnaest.react4j.security.WebSecurityConfiguration;
import org.omnaest.react4j.service.ReactUIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * S3-AC-R4's "global ceiling" limb, isolated into its own test class so it can run under a deliberately SMALL
 * {@code react4j.upload.max-file-size} - the exact same configuration property {@code FileUploadConfiguration}'s
 * {@code MultipartConfigElement} bean is built from for the default (multipart) transport. Proves the unbuffered
 * transport enforces the coarse global ceiling itself, since the servlet container's own multipart-parsing
 * enforcement of that ceiling does not run on a non-multipart request (plan-154 section 3.3).
 */
@SpringBootTest(classes = UnbufferedUploadTransportGlobalCeilingTest.TestApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {"react4j.upload.max-file-size=2KB", "react4j.upload.max-request-size=2KB"})
public class UnbufferedUploadTransportGlobalCeilingTest
{
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @LocalServerPort
    private int                       port;

    @Autowired
    private TestRestTemplate          restTemplate;

    @Autowired
    private ReactUIService            reactUIService;

    @SpringBootApplication
    @EnableReactUI
    @EnableReactUIInMemoryRepository
    @Import(WebSecurityConfiguration.class)
    public static class TestApplication
    {
    }

    @BeforeAll
    static void ensureTheRedirectedJavaIoTmpdirExists() throws IOException
    {
        // Defensive, idempotent: the directory is normally created by UnbufferedUploadTransportEndToEndTest's own
        // @BeforeAll, but this class must not depend on running after it - Surefire's discovery order is not a
        // contract. Files.createDirectories on an explicit path never touches the JDK's temp-file caches (see the
        // pom.xml <argLine> comment), so it is always safe here regardless of ordering.
        Files.createDirectories(Paths.get(System.getProperty("java.io.tmpdir")));
    }

    @Test
    public void testGlobalCeilingRejectsAPayloadBelowTheChannelsOwnMaxSizeButAboveTheConfiguredGlobalCeiling() throws Exception
    {
        // The channel's OWN bound is deliberately generous (1 MB) - if this request is rejected, it can only be the
        // GLOBAL ceiling (react4j.upload.max-file-size=2KB, set above) doing the rejecting, not the channel.
        ByteArrayChannel channel = ByteArrayChannel.create()
                                                   .withMaxSize(1024 * 1024);
        String uploadId = this.registerFormWithChannel(channel);

        byte[] payload = new byte[5 * 1024]; // 5 KB > the 2 KB configured ceiling, well under the channel's 1 MB bound

        ResponseEntity<String> response = this.postRaw(uploadId, payload);

        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, response.getStatusCode());
        assertFalse(channel.getContent()
                           .isPresent(),
                    "The channel must never receive bytes once the global ceiling refuses the request");
    }

    private String registerFormWithChannel(UploadChannel channel) throws Exception
    {
        this.reactUIService.createDefaultRoot(reactUI -> reactUI.addNewComponent(factory -> factory.newForm()
                                                                                                   .withUIContext((form, context) ->
                                                                                                   {
                                                                                                       form.attachTo(context.getFirstDocument());
                                                                                                       form.addFileUpload(fileUpload -> fileUpload.withUploadChannel(channel)
                                                                                                                                                  .withUnbufferedTransport());
                                                                                                   })));
        // /ui's mapping only ever produces JSON; a real HTTP client's default Accept header prefers text/html, which
        // - with no HTML representation registered for /ui - yields an HTML error page instead, so this must be explicit.
        RequestEntity<Void> request = RequestEntity.get(URI.create("http://localhost:" + this.port + "/ui"))
                                                   .accept(MediaType.APPLICATION_JSON)
                                                   .build();
        String json = this.restTemplate.exchange(request, String.class)
                                       .getBody();
        JsonNode fileUploadNode = this.findFileUploadNode(OBJECT_MAPPER.readTree(json));
        return fileUploadNode.get("uploadId")
                             .asText();
    }

    private JsonNode findFileUploadNode(JsonNode node)
    {
        if (node == null)
        {
            return null;
        }
        if (node.isObject())
        {
            if (node.has("fileUpload") && node.get("fileUpload")
                                              .isObject())
            {
                return node.get("fileUpload");
            }
            Iterator<String> fieldNames = node.fieldNames();
            while (fieldNames.hasNext())
            {
                JsonNode result = this.findFileUploadNode(node.get(fieldNames.next()));
                if (result != null)
                {
                    return result;
                }
            }
        }
        else if (node.isArray())
        {
            for (JsonNode child : node)
            {
                JsonNode result = this.findFileUploadNode(child);
                if (result != null)
                {
                    return result;
                }
            }
        }
        return null;
    }

    private ResponseEntity<String> postRaw(String uploadId, byte[] payload) throws Exception
    {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Upload-Id", uploadId);
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        RequestEntity<byte[]> request = new RequestEntity<>(payload, headers, HttpMethod.POST, new URI("http://localhost:" + this.port + "/ui/upload/raw"));
        return this.restTemplate.exchange(request, String.class);
    }
}
