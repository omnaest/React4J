package org.omnaest.react4j.upload;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.omnaest.react4j.EnableReactUI;
import org.omnaest.react4j.component.form.upload.ByteArrayChannel;
import org.omnaest.react4j.component.form.upload.FileChannel;
import org.omnaest.react4j.component.form.upload.UploadChannel;
import org.omnaest.react4j.component.form.upload.UploadContent;
import org.omnaest.react4j.component.form.upload.UploadReceipt;
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
 * The test JVM's {@code java.io.tmpdir} is redirected to a fresh directory before the Spring context (and therefore
 * embedded Tomcat) starts, so the watched tree is the one Tomcat spills into and nothing else. The redirection is
 * self-verifying: the positive control below can only pass if Tomcat's own spill file actually lands inside the
 * watched tree, which happens only if the redirection took effect before the embedded container booted.
 * <p>
 * <b>How the check decides - deterministic by construction.</b> There is no sampler thread, no sleep, no retry and no
 * waiting for a file to show up. The check is a complete, synchronous scan of the watched tree that runs <em>on the
 * request thread, inside the request</em>, from {@link SpillScanningChannel}: a test channel that wraps (composes) the
 * real {@link ByteArrayChannel} / {@link FileChannel}, so those channels' real behaviour is what is observed. The scan
 * runs at three points of every request ({@link ScanPoint}): before the channel reads the body, at the moment the
 * channel has read the body to its end, and after the channel has returned - all before the response exists. A spill
 * that exists at any of those instants is found with certainty, whatever its lifetime and whatever the scan costs
 * (the tree grows with the leftover {@code tomcat.*} directories that only {@code mvn clean} removes; the scan simply
 * takes longer, it never gives up). The scan either finds a spill or names every candidate it could not examine, and
 * an unexaminable candidate is never a clean verdict (see {@link #assertNoSpillSeen(String, SpillScanner)}).
 * <p>
 * The same mechanism is the positive control: over the existing multipart transport Tomcat's spill exists from the
 * container's parse until the request completes, so it exists at all three scan points - the control asserts exactly
 * that, which both proves the scanner can see a spill and proves the three points lie inside the window in which a
 * container spill lives.
 * <p>
 * <b>What this cannot see (the residual window).</b> A spill that is created and removed entirely between two scan
 * points - in particular one removed before the channel receives the request (before the first scan), or created
 * after the last scan but before the request completes - is invisible to it. So is a spill outside the watched tree
 * ({@code java.io.tmpdir} of this JVM), a spill that does not contain the first {@value #PROBE_LENGTH} bytes of the
 * payload verbatim (compressed, encrypted, offset), a file smaller than that (it cannot hold the probe), and anything
 * behind a symbolic link (links are not followed). The tree is also shared with any other test run that uses the same
 * {@code target} directory at the same time: such a run's own multipart spill carries the same payload bytes.
 */
@SpringBootTest(classes = UnbufferedUploadTransportEndToEndTest.TestApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
public class UnbufferedUploadTransportEndToEndTest
{
    /**
     * How many leading payload bytes a file must contain to count as a spill. A prefix instead of the whole payload, so that a spill still being written
     * (or written in pieces) is found as soon as its first bytes are on disk.
     */
    private static final int          PROBE_LENGTH  = 256;

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
        byte[] payload = randomPayload(20 * 1024, 42);
        String expectedDigest = sha256Hex(payload);
        // Non-ASCII filename (accented Latin + CJK), built from explicit code points rather than a literal glyph to
        // keep this source file pure ASCII (AC-R5).
        String nonAsciiFilename = "r" + (char) 0x00e9 + "sum" + (char) 0x00e9 + "-" + (char) 0x65e5 + (char) 0x672c + (char) 0x8a9e + ".bin";

        // --- 1) raw transport -> ByteArrayChannel -------------------------------------------------------------
        ByteArrayChannel byteArrayChannel = ByteArrayChannel.create();
        SpillScanner byteArrayScanner = new SpillScanner(watchedTempRoot, payload);
        String byteArrayUploadId = this.registerFormWithChannel(new SpillScanningChannel(byteArrayChannel, byteArrayScanner));
        JsonNode fileUploadNode = this.findFileUploadNode(this.renderUiJson());
        assertEquals("ui/upload/raw", fileUploadNode.get("uploadUrl")
                                                    .asText(),
                     "An opted-in element must render the raw transport's URL");
        assertTrue(fileUploadNode.get("unbufferedTransport")
                                 .asBoolean(),
                   "An opted-in element must render unbufferedTransport=true");

        ResponseEntity<String> byteArrayResponse = this.postRaw(byteArrayUploadId, nonAsciiFilename, "application/octet-stream", payload);

        assertEquals(HttpStatus.OK, byteArrayResponse.getStatusCode());
        JsonNode byteArrayReceipt = OBJECT_MAPPER.readTree(byteArrayResponse.getBody());
        assertEquals(byteArrayUploadId, byteArrayReceipt.get("uploadId")
                                                        .asText());
        assertEquals(nonAsciiFilename, byteArrayReceipt.get("filename")
                                                       .asText(),
                     "A non-ASCII filename must round-trip byte-exactly to UploadContent.filename() (AC-R5)");
        assertNoSpillSeen("leg 1 (raw transport -> ByteArrayChannel)", byteArrayScanner);
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
        assertFalse(destination.toAbsolutePath()
                               .startsWith(watchedTempRoot.toAbsolutePath()),
                    "The FileChannel's destination holds the payload by design, so it must lie outside the watched tree");
        FileChannel fileChannel = FileChannel.toPath(destination);
        SpillScanner fileChannelScanner = new SpillScanner(watchedTempRoot, payload);
        String fileChannelUploadId = this.registerFormWithChannel(new SpillScanningChannel(fileChannel, fileChannelScanner));

        ResponseEntity<String> fileChannelResponse = this.postRaw(fileChannelUploadId, "payload.bin", "application/octet-stream", payload);

        assertEquals(HttpStatus.OK, fileChannelResponse.getStatusCode());
        assertNoSpillSeen("leg 2 (raw transport -> FileChannel)", fileChannelScanner);
        assertTrue(Files.exists(destination));
        assertEquals(expectedDigest, sha256Hex(Files.readAllBytes(destination)));
        Files.deleteIfExists(destination);

        // --- 3) positive control: the SAME scanner and the SAME in-request scan points, over the EXISTING multipart transport ---
        // Tomcat's multipart spill exists from the container's parse of the request until the request completes, so it must be seen at every scan point.
        SpillScanner controlScanner = new SpillScanner(this.treeTheControlWatches(), payload);
        String controlUploadId = this.registerFormWithChannel(new SpillScanningChannel(ByteArrayChannel.create(), controlScanner));

        this.postMultipart(controlUploadId, "control.bin", payload);

        assertEquals(ScanPoint.all(), controlScanner.pointsScanned(), "leg 3: the in-request scans did not all run");
        assertEquals(ScanPoint.all(), controlScanner.pointsWhereASpillWasSeen(),
                     "The positive control (existing multipart transport) must show its spill file at every scan point - otherwise the scanner is not proven to be "
                                                                                 + "watching the right tree, or the scan points are not inside the window in which a container spill lives. Spills seen: "
                                                                                 + controlScanner.spillsSeen());
    }

    /**
     * The tree the positive control's scanner watches: the one Tomcat spills into. A seam for the mutation that proves the control can fail (watch a tree
     * the multipart spill never lands in and the assertion above must go red).
     */
    private Path treeTheControlWatches()
    {
        return watchedTempRoot;
    }

    /**
     * The scanner's own proof that it can see a spill: a file that exists at scan time is found even though it is gone right afterwards, a spill still being
     * written (only its first bytes on disk) is found, a file that merely has the same size is not a spill, and the finding of an earlier scan is not erased
     * by a later scan that finds nothing. This is what gives a clean verdict on the raw legs its meaning.
     */
    @Test
    public void testSpillScannerFindsAFileThatExistsAtScanTimeEvenWhenItIsGoneRightAfterwardsAndStaysSilentOtherwise(@TempDir Path tree) throws IOException
    {
        byte[] payload = randomPayload(4 * 1024, 7);
        Files.write(tree.resolve("same-size-other-bytes.bin"), randomPayload(payload.length, 8));
        Files.write(tree.resolve("tiny.bin"), Arrays.copyOf(payload, PROBE_LENGTH - 1));

        SpillScanner scanner = new SpillScanner(tree, payload);
        scanner.scanAllPoints();
        assertNoSpillSeen("a tree with no spill", scanner);

        SpillScanner shortLived = new SpillScanner(tree, payload);
        Path spill = Files.write(tree.resolve("short-lived-spill.tmp"), payload);
        shortLived.scan(ScanPoint.BEFORE_BODY_IS_READ);
        Files.delete(spill);
        shortLived.scan(ScanPoint.BODY_FULLY_READ);
        shortLived.scan(ScanPoint.AFTER_CHANNEL_RETURNED);
        assertEquals(List.of(ScanPoint.BEFORE_BODY_IS_READ), shortLived.pointsWhereASpillWasSeen());
        assertThrows(AssertionError.class, () -> assertNoSpillSeen("a spill that was gone again at the later scan points", shortLived));

        SpillScanner partial = new SpillScanner(tree, payload);
        Files.write(tree.resolve("half-written-spill.tmp"), Arrays.copyOf(payload, PROBE_LENGTH + 44));
        partial.scanAllPoints();
        assertEquals(ScanPoint.all(), partial.pointsWhereASpillWasSeen());
    }

    /**
     * A candidate that exists but cannot be read, and a tree that cannot be walked, must never read as "no spill". The reader is injected so the failure is
     * the one an operating system produces for a file another process holds open or a directory it will not list, on every platform.
     */
    @Test
    public void testSpillScannerNeverGivesACleanVerdictWhenACandidateCannotBeRead(@TempDir Path tree) throws IOException
    {
        byte[] payload = randomPayload(4 * 1024, 7);
        Path locked = Files.write(tree.resolve("locked.bin"), randomPayload(payload.length, 9));
        Files.write(tree.resolve("readable.bin"), randomPayload(payload.length, 10));

        SpillScanner readable = new SpillScanner(tree, payload);
        readable.scanAllPoints();
        assertNoSpillSeen("control: every candidate readable", readable);

        SpillScanner withLockedFile = new SpillScanner(tree, payload, file ->
        {
            if (file.equals(locked))
            {
                throw new AccessDeniedException(file.toString(), null, "simulated: held open by another process");
            }
            return Files.readAllBytes(file);
        });
        withLockedFile.scanAllPoints();
        assertEquals(ScanPoint.all(), withLockedFile.pointsScanned());
        assertEquals(3, withLockedFile.unexaminableCandidates()
                                      .size(),
                     "The locked file must be reported at each of the three scan points: " + withLockedFile.unexaminableCandidates());
        assertThrows(AssertionError.class, () -> assertNoSpillSeen("a locked candidate", withLockedFile));

        SpillScanner missingRoot = new SpillScanner(tree.resolve("does-not-exist"), payload);
        missingRoot.scanAllPoints();
        assertThrows(AssertionError.class, () -> assertNoSpillSeen("a watched tree that does not exist", missingRoot));
    }

    /**
     * The verdict of the negative checks. The scans must all have run, no file may hold the payload's start, and no candidate may have been left
     * unexamined: "I could not look" is not "there is nothing there".
     */
    private static void assertNoSpillSeen(String check, SpillScanner scanner)
    {
        assertEquals(ScanPoint.all(), scanner.pointsScanned(), check + ": the in-request scans did not all run, so the check did not look where it claims to");
        assertEquals(List.of(), scanner.spillsSeen(), check + ": a file in the watched temp tree contains the uploaded bytes");
        assertEquals(List.of(), scanner.unexaminableCandidates(), check + ": a candidate could not be examined, so the absence of a spill is not established");
    }

    /**
     * The instants, inside one request, at which the watched tree is scanned.
     */
    private enum ScanPoint
    {
        /** The channel has been handed the request, and has not read a byte of the body yet. */
        BEFORE_BODY_IS_READ,
        /** The channel has read the body to its end and is still inside its own write. */
        BODY_FULLY_READ,
        /** The channel has returned; the request (and with it any container spill) is still open. */
        AFTER_CHANNEL_RETURNED;

        static List<ScanPoint> all()
        {
            return List.of(values());
        }
    }

    /**
     * Reads a file's bytes; a seam so that the scanner's behaviour for a candidate that cannot be read can be exercised on every platform.
     */
    @FunctionalInterface
    private interface FileContents
    {
        byte[] read(Path file) throws IOException;
    }

    /**
     * Scans a whole directory tree, synchronously and to the end, for files containing the first {@value UnbufferedUploadTransportEndToEndTest#PROBE_LENGTH}
     * bytes of a payload, and records what it found, where, and every candidate it could not examine. It has no thread, no timer and no stop condition other
     * than having visited everything.
     */
    private static final class SpillScanner
    {
        private final Path            root;
        private final byte[]          probe;
        private final FileContents    contents;
        private final List<ScanPoint> pointsScanned = Collections.synchronizedList(new ArrayList<>());
        private final List<String>    spillsSeen    = Collections.synchronizedList(new ArrayList<>());
        private final List<ScanPoint> spillPoints   = Collections.synchronizedList(new ArrayList<>());
        private final List<String>    unexaminable  = Collections.synchronizedList(new ArrayList<>());

        SpillScanner(Path root, byte[] payload)
        {
            this(root, payload, Files::readAllBytes);
        }

        SpillScanner(Path root, byte[] payload, FileContents contents)
        {
            this.root = root;
            this.probe = Arrays.copyOf(payload, Math.min(PROBE_LENGTH, payload.length));
            this.contents = contents;
        }

        void scanAllPoints()
        {
            for (ScanPoint point : ScanPoint.values())
            {
                this.scan(point);
            }
        }

        void scan(ScanPoint point)
        {
            try
            {
                Files.walkFileTree(this.root, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attributes)
                    {
                        // A file shorter than the probe cannot contain it, so it is not a candidate and need not be read.
                        if (attributes.isRegularFile() && attributes.size() >= SpillScanner.this.probe.length)
                        {
                            SpillScanner.this.examine(file, point);
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException e)
                    {
                        SpillScanner.this.unexaminable.add(file.toAbsolutePath() + " at " + point + " (could not be visited): " + e);
                        return FileVisitResult.CONTINUE;
                    }
                });
            }
            catch (IOException | RuntimeException e)
            {
                this.unexaminable.add(this.root.toAbsolutePath() + " at " + point + " (walk aborted): " + e);
            }
            this.pointsScanned.add(point);
        }

        private void examine(Path file, ScanPoint point)
        {
            try
            {
                if (containsSubsequence(this.contents.read(file), this.probe))
                {
                    this.spillsSeen.add(file.toAbsolutePath() + " at " + point);
                    this.spillPoints.add(point);
                }
            }
            catch (IOException | RuntimeException e)
            {
                this.unexaminable.add(file.toAbsolutePath() + " at " + point + " (could not be read): " + e);
            }
        }

        List<ScanPoint> pointsScanned()
        {
            return new ArrayList<>(this.pointsScanned);
        }

        List<ScanPoint> pointsWhereASpillWasSeen()
        {
            Set<ScanPoint> distinct = new LinkedHashSet<>(this.spillPoints);
            return new ArrayList<>(distinct);
        }

        List<String> spillsSeen()
        {
            return new ArrayList<>(this.spillsSeen);
        }

        List<String> unexaminableCandidates()
        {
            return new ArrayList<>(this.unexaminable);
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

    /**
     * A channel that composes the real channel under test and scans the watched tree on the request thread around the real channel's work: before it reads
     * the body, when it has read the body to the end, and after it has returned. Everything else - size limit, accepted types, the receipt, the bytes - is
     * the wrapped channel's own.
     */
    private static final class SpillScanningChannel implements UploadChannel
    {
        private final UploadChannel delegate;
        private final SpillScanner  scanner;

        SpillScanningChannel(UploadChannel delegate, SpillScanner scanner)
        {
            this.delegate = delegate;
            this.scanner = scanner;
        }

        @Override
        public UploadReceipt consume(UploadContent content)
        {
            this.scanner.scan(ScanPoint.BEFORE_BODY_IS_READ);
            UploadReceipt receipt = this.delegate.consume(new EndOfBodyScanningContent(content, this.scanner));
            this.scanner.scan(ScanPoint.AFTER_CHANNEL_RETURNED);
            return receipt;
        }

        @Override
        public long maxSizeBytes()
        {
            return this.delegate.maxSizeBytes();
        }

        @Override
        public Set<String> acceptedContentTypes()
        {
            return this.delegate.acceptedContentTypes();
        }
    }

    /**
     * The upload content as the wrapped channel sees it: identical to the real one, except that the stream it hands out scans the watched tree once, at the
     * moment a read first reports the end of the body.
     */
    private static final class EndOfBodyScanningContent implements UploadContent
    {
        private final UploadContent content;
        private final SpillScanner  scanner;

        EndOfBodyScanningContent(UploadContent content, SpillScanner scanner)
        {
            this.content = content;
            this.scanner = scanner;
        }

        @Override
        public String filename()
        {
            return this.content.filename();
        }

        @Override
        public String contentType()
        {
            return this.content.contentType();
        }

        @Override
        public long size()
        {
            return this.content.size();
        }

        @Override
        public InputStream inputStream() throws IOException
        {
            return new FilterInputStream(this.content.inputStream()) {
                private boolean scannedAtEnd;

                @Override
                public int read() throws IOException
                {
                    int next = super.read();
                    if (next < 0)
                    {
                        this.scanOnceAtEnd();
                    }
                    return next;
                }

                @Override
                public int read(byte[] buffer, int offset, int length) throws IOException
                {
                    int count = super.read(buffer, offset, length);
                    if (count < 0)
                    {
                        this.scanOnceAtEnd();
                    }
                    return count;
                }

                private void scanOnceAtEnd()
                {
                    if (!this.scannedAtEnd)
                    {
                        this.scannedAtEnd = true;
                        EndOfBodyScanningContent.this.scanner.scan(ScanPoint.BODY_FULLY_READ);
                    }
                }
            };
        }
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

    private static byte[] randomPayload(int size, long seed)
    {
        byte[] payload = new byte[size];
        new Random(seed).nextBytes(payload);
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
}
