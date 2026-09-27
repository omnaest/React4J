package org.omnaest.react4j.upload;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.omnaest.react4j.EnableReactUI;
import org.omnaest.react4j.component.form.upload.ByteArrayChannel;
import org.omnaest.react4j.component.form.upload.FileChannel;
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
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Proves the unbuffered upload transport (S3-AC-R3/R4/R5 of plan-154) against a REAL embedded servlet container -
 * {@code webEnvironment = RANDOM_PORT} with a real HTTP client ({@link TestRestTemplate}), never {@code MockMvc}
 * (which runs no servlet container and therefore cannot produce, or fail to produce, a container-level temp-file
 * spill). This test lives in {@code react4j-ui-test} rather than {@code react4j-core} specifically because this
 * module already carries an embedded servlet container transitively (via {@code CommonsSpringBootParent}), so no
 * new dependency is needed anywhere - {@code react4j-core} itself is a plain library module with none.
 * <p>
 * The test JVM's {@code java.io.tmpdir} is redirected to a fresh, empty directory before the Spring context (and
 * therefore embedded Tomcat) starts, so a 1 ms in-process sampler can watch the whole candidate tree cheaply
 * (measured: ~1 ms per walk over a fresh tree versus 3.3-3.6 s over the real system temp). The redirection is
 * self-verifying: the positive control below can only pass if Tomcat's own spill file actually lands inside the
 * watched tree, which happens only if the redirection took effect before the embedded container booted.
 */
@SpringBootTest(classes = UnbufferedUploadTransportEndToEndTest.TestApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
public class UnbufferedUploadTransportEndToEndTest
{
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static Path               watchedTempRoot;

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
    static void resolveTheRedirectedWatchedRoot() throws IOException
    {
        // java.io.tmpdir is redirected via the module's Surefire <argLine> (pom.xml), NOT here at runtime - the
        // JDK's temp-file helpers cache java.io.tmpdir in a static field on first use, and Surefire reuses one JVM
        // across every test class in this module by default, so a class-local System.setProperty could silently
        // lose that race against whatever earlier class already touched a temp file. Files.createDirectories on an
        // explicit path (unlike Files.createTempDirectory/File.createTempFile) never consults that cache, so it is
        // safe to call here regardless of ordering.
        watchedTempRoot = Paths.get(System.getProperty("java.io.tmpdir"));
        Files.createDirectories(watchedTempRoot);
    }

    // No @AfterAll cleanup of watchedTempRoot: it IS java.io.tmpdir for the whole (reused) Surefire JVM, so other
    // test classes running afterward in the same fork (e.g. UnbufferedUploadTransportGlobalCeilingTest) still need
    // it to exist. It lives under target/, which `mvn clean` already owns.

    @Test
    public void testUnbufferedTransportDeliversExactBytesWithNoTempFileSpillAndTheDetectorHasAWorkingPositiveControl() throws Exception
    {
        byte[] payload = randomPayload(20 * 1024);
        String expectedDigest = sha256Hex(payload);
        // Non-ASCII filename (accented Latin + CJK), built from explicit code points rather than a literal glyph to
        // keep this source file pure ASCII (AC-R5).
        String nonAsciiFilename = "r" + (char) 0x00e9 + "sum" + (char) 0x00e9 + "-" + (char) 0x65e5 + (char) 0x672c + (char) 0x8a9e + ".bin";

        // --- 1) raw transport -> ByteArrayChannel -------------------------------------------------------------
        ByteArrayChannel byteArrayChannel = ByteArrayChannel.create();
        String byteArrayUploadId = this.registerFormWithChannel(byteArrayChannel);
        JsonNode fileUploadNode = this.findFileUploadNode(this.renderUiJson());
        assertEquals("ui/upload/raw", fileUploadNode.get("uploadUrl")
                                                    .asText(),
                     "An opted-in element must render the raw transport's URL");
        assertTrue(fileUploadNode.get("unbufferedTransport")
                                 .asBoolean(),
                   "An opted-in element must render unbufferedTransport=true");

        TempTreeSpillDetector detector1 = new TempTreeSpillDetector(watchedTempRoot, payload);
        detector1.start();
        ResponseEntity<String> byteArrayResponse = this.postRaw(byteArrayUploadId, nonAsciiFilename, "application/octet-stream", payload);
        detector1.stop();

        assertEquals(HttpStatus.OK, byteArrayResponse.getStatusCode());
        JsonNode byteArrayReceipt = OBJECT_MAPPER.readTree(byteArrayResponse.getBody());
        assertEquals(byteArrayUploadId, byteArrayReceipt.get("uploadId")
                                                        .asText());
        assertEquals(nonAsciiFilename, byteArrayReceipt.get("filename")
                                                       .asText(),
                     "A non-ASCII filename must round-trip byte-exactly to UploadContent.filename() (AC-R5)");
        assertFalse(detector1.wasFound(), "No file anywhere in the watched temp tree may contain the uploaded bytes: " + detector1.matchedPath());
        assertArrayEquals(payload, byteArrayChannel.getContent()
                                                   .get()
                                                   .asBytes());
        assertEquals(expectedDigest, sha256Hex(byteArrayChannel.getContent()
                                                               .get()
                                                               .asBytes()));

        // --- 2) raw transport -> FileChannel (destination deliberately OUTSIDE the watched tree) --------------
        Path fileChannelDestinationDir = Paths.get("target", "unbuffered-upload-test-output");
        Files.createDirectories(fileChannelDestinationDir);
        Path destination = fileChannelDestinationDir.resolve("upload-" + UUID.randomUUID() + ".bin");
        FileChannel fileChannel = FileChannel.toPath(destination);
        String fileChannelUploadId = this.registerFormWithChannel(fileChannel);

        TempTreeSpillDetector detector2 = new TempTreeSpillDetector(watchedTempRoot, payload);
        detector2.start();
        ResponseEntity<String> fileChannelResponse = this.postRaw(fileChannelUploadId, "payload.bin", "application/octet-stream", payload);
        detector2.stop();

        assertEquals(HttpStatus.OK, fileChannelResponse.getStatusCode());
        assertFalse(detector2.wasFound(), "No file anywhere in the watched temp tree may contain the uploaded bytes: " + detector2.matchedPath());
        assertTrue(Files.exists(destination));
        assertEquals(expectedDigest, sha256Hex(Files.readAllBytes(destination)));
        Files.deleteIfExists(destination);

        // --- 3) positive control: the SAME detector class, unchanged, over the EXISTING multipart transport ---
        ByteArrayChannel controlChannel = ByteArrayChannel.create();
        String controlUploadId = this.registerFormWithChannel(controlChannel);

        TempTreeSpillDetector detector3 = new TempTreeSpillDetector(watchedTempRoot, payload);
        detector3.start();
        this.postMultipart(controlUploadId, "control.bin", payload);
        detector3.stop();

        assertTrue(detector3.wasFound(), "The positive control (existing multipart transport) must produce at least one matching temp file - "
                                         + "otherwise the detector is not proven to be watching the right tree");
    }

    private String registerFormWithChannel(UploadChannel channel)
    {
        this.reactUIService.createDefaultRoot(reactUI -> reactUI.addNewComponent(factory -> factory.newForm()
                                                                                                   .withUIContext((form, context) ->
                                                                                                   {
                                                                                                       form.attachTo(context.getFirstDocument());
                                                                                                       form.addFileUpload(fileUpload -> fileUpload.withUploadChannel(channel)
                                                                                                                                                  .withUnbufferedTransport());
                                                                                                   })));
        return this.findFileUploadNode(this.renderUiJson()).get("uploadId")
                   .asText();
    }

    private String renderUiJson()
    {
        // /ui's mapping only ever produces JSON. A real HTTP client (unlike MockMvc, which sends no Accept header)
        // sends one preferring text/html by default, which - with no HTML representation registered for /ui - yields
        // an HTML error page instead of the rendered tree, so the JSON preference must be made explicit here.
        RequestEntity<Void> request = RequestEntity.get(URI.create("http://localhost:" + this.port + "/ui"))
                                                   .accept(MediaType.APPLICATION_JSON)
                                                   .build();
        ResponseEntity<String> response = this.restTemplate.exchange(request, String.class);
        if (response.getBody() == null)
        {
            throw new IllegalStateException("GET /ui returned no body. status=" + response.getStatusCode() + " headers=" + response.getHeaders());
        }
        return response.getBody();
    }

    private ResponseEntity<String> postRaw(String uploadId, String filename, String contentType, byte[] payload) throws Exception
    {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Upload-Id", uploadId);
        headers.set("X-Filename", URLEncoder.encode(filename, StandardCharsets.UTF_8));
        headers.setContentType(MediaType.parseMediaType(contentType));
        RequestEntity<byte[]> request = new RequestEntity<>(payload, headers, HttpMethod.POST, new URI("http://localhost:" + this.port + "/ui/upload/raw"));
        return this.restTemplate.exchange(request, String.class);
    }

    private void postMultipart(String uploadId, String filename, byte[] payload)
    {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(payload) {
            @Override
            public String getFilename()
            {
                return filename;
            }
        });
        body.add("uploadId", uploadId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = this.restTemplate.postForEntity("/ui/upload", request, String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    private JsonNode findFileUploadNode(String json)
    {
        try
        {
            JsonNode root = OBJECT_MAPPER.readTree(json);
            JsonNode result = this.findFileUploadNode(root);
            assertNotNull(result, "Expected to find a fileUpload node in the rendered UI JSON: " + json);
            return result;
        }
        catch (Exception e)
        {
            throw new IllegalStateException(e);
        }
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

    private static byte[] randomPayload(int size)
    {
        byte[] payload = new byte[size];
        new Random(42).nextBytes(payload);
        return payload;
    }

    private static String sha256Hex(byte[] bytes) throws NoSuchAlgorithmException
    {
        byte[] digest = MessageDigest.getInstance("SHA-256")
                                     .digest(bytes);
        StringBuilder hex = new StringBuilder();
        for (byte b : digest)
        {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }

    /**
     * A daemon-thread sampler that walks {@code root} recursively every 1 ms and reports whether any file's
     * CONTENTS contain the exact bytes it is watching for. Deliberately reused unchanged (same class, same
     * mechanism) for both the negative assertions (the unbuffered transport) and the positive control (the
     * existing multipart transport), so a green negative result is evidence rather than an assumption about the
     * scanner (plan-154 W5's discipline, applied here to React4J's own suite).
     */
    private static final class TempTreeSpillDetector
    {
        private final Path       root;
        private final byte[]     needle;
        private volatile boolean found;
        private volatile String  matchedPath;
        private volatile Thread  samplerThread;
        private volatile boolean stopRequested;

        TempTreeSpillDetector(Path root, byte[] needle)
        {
            this.root = root;
            this.needle = needle;
        }

        void start()
        {
            this.samplerThread = new Thread(this::sampleLoop, "unbuffered-upload-temp-tree-spill-detector");
            this.samplerThread.setDaemon(true);
            this.samplerThread.start();
        }

        void stop() throws InterruptedException
        {
            this.stopRequested = true;
            this.samplerThread.join(5000);
        }

        boolean wasFound()
        {
            return this.found;
        }

        String matchedPath()
        {
            return this.matchedPath;
        }

        private void sampleLoop()
        {
            while (!this.stopRequested)
            {
                this.scanOnce();
                try
                {
                    Thread.sleep(1);
                }
                catch (InterruptedException e)
                {
                    Thread.currentThread()
                          .interrupt();
                    return;
                }
            }
            this.scanOnce();
        }

        private void scanOnce()
        {
            if (this.found || !Files.exists(this.root))
            {
                return;
            }
            try (Stream<Path> walk = Files.walk(this.root))
            {
                walk.filter(Files::isRegularFile)
                    .forEach(this::checkFile);
            }
            catch (IOException ignored)
            {
                // a transient failure (file created/deleted mid-walk) is expected and not a negative result
            }
        }

        private void checkFile(Path file)
        {
            try
            {
                byte[] content = Files.readAllBytes(file);
                if (containsSubsequence(content, this.needle))
                {
                    this.found = true;
                    this.matchedPath = file.toAbsolutePath()
                                           .toString();
                }
            }
            catch (IOException ignored)
            {
                // the file may be mid-write or mid-delete at the moment we read it; a transient read failure is
                // not itself a negative result - the next 1 ms iteration will see it if it is still there
            }
        }

        private static boolean containsSubsequence(byte[] haystack, byte[] needle)
        {
            if (needle.length == 0 || haystack.length < needle.length)
            {
                return false;
            }
            outer : for (int i = 0; i <= haystack.length - needle.length; i++)
            {
                for (int j = 0; j < needle.length; j++)
                {
                    if (haystack[i + j] != needle[j])
                    {
                        continue outer;
                    }
                }
                return true;
            }
            return false;
        }
    }
}
