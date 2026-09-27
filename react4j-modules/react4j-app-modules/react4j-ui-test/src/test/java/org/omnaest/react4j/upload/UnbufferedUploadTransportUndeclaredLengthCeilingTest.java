package org.omnaest.react4j.upload;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Iterator;

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
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.test.context.TestPropertySource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * S3-AC-R8: the raw transport enforces its global ceiling TWICE - a {@code Content-Length} pre-check <em>and</em> a {@code BoundedInputStream} stream-wrap -
 * and correctly skips the pre-check when {@code getContentLengthLong()} returns {@code -1}. {@link UnbufferedUploadTransportGlobalCeilingTest} exercises
 * only the pre-check limb, because {@code TestRestTemplate}/{@code RestTemplate} always sets {@code Content-Length}. This class proves the STREAM-WRAP limb
 * by sending a request with NO declared {@code Content-Length} - exactly the shape a browser {@code fetch} with a streamed request body produces - via
 * {@link HttpURLConnection#setChunkedStreamingMode(int)} (JDK-only, no new dependency: {@code Transfer-Encoding: chunked}, no {@code Content-Length} header
 * at all), and asserts the response is still {@code 413}.
 * <p>
 * The channel's own bound is deliberately generous (1 MB, same discipline as {@link UnbufferedUploadTransportGlobalCeilingTest}) so a 413 here can only be
 * the controller's global-ceiling stream wrap acting, not {@code AbstractUploadChannel}'s own bound.
 */
@SpringBootTest(classes = UnbufferedUploadTransportUndeclaredLengthCeilingTest.TestApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {"react4j.upload.max-file-size=2KB", "react4j.upload.max-request-size=2KB"})
public class UnbufferedUploadTransportUndeclaredLengthCeilingTest
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

    @Test
    public void testUndeclaredLengthBodyExceedingTheGlobalCeilingIsRejectedWith413() throws Exception
    {
        ByteArrayChannel channel = ByteArrayChannel.create()
                                                   .withMaxSize(1024 * 1024);
        String uploadId = this.registerFormWithChannel(channel);

        byte[] payload = new byte[5 * 1024]; // 5 KB > the 2 KB configured global ceiling, well under the channel's own 1 MB bound

        int status = this.postRawWithNoDeclaredLength(uploadId, payload);

        assertEquals(413, status, "An undeclared-length body exceeding the global ceiling must be rejected with 413 (the stream-wrap limb)");
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

    /**
     * Posts via {@link HttpURLConnection} with chunked streaming mode, which forces {@code Transfer-Encoding: chunked} and OMITS the {@code Content-Length}
     * header entirely - so {@code request.getContentLengthLong()} on the server returns {@code -1}, the exact case the controller's {@code Content-Length}
     * pre-check is documented to skip, and the exact shape a browser {@code fetch} with a streamed request body produces.
     * <p>
     * The server stops reading (and returns 413) once the ceiling is crossed, well before this method has written the whole 5 KB payload, so the client's
     * OWN {@code write} calls may legitimately fail with a broken-pipe/connection-reset {@link IOException} once the server-side socket closes its read
     * side - that is an EXPECTED consequence of the guard doing its job, not a test defect, so it is swallowed here; the response status, read afterward, is
     * the actual assertion.
     */
    private int postRawWithNoDeclaredLength(String uploadId, byte[] payload) throws IOException
    {
        URL url = URI.create("http://localhost:" + this.port + "/ui/upload/raw")
                     .toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        try
        {
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setChunkedStreamingMode(1024);
            connection.setRequestProperty("X-Upload-Id", uploadId);
            connection.setRequestProperty("Content-Type", "application/octet-stream");
            try (OutputStream out = connection.getOutputStream())
            {
                out.write(payload);
                out.flush();
            }
            catch (IOException e)
            {
                // Expected: the server may close its read side as soon as the ceiling is crossed, before this
                // method has finished writing the full (deliberately oversized) payload - see javadoc above.
            }
            return connection.getResponseCode();
        }
        finally
        {
            connection.disconnect();
        }
    }
}
