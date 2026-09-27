package org.omnaest.react4j.browser;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.omnaest.react4j.EnableReactUI;
import org.omnaest.react4j.component.form.upload.ByteArrayChannel;
import org.omnaest.react4j.data.annotations.EnableReactUIInMemoryRepository;
import org.omnaest.react4j.security.WebSecurityConfiguration;
import org.omnaest.react4j.service.ReactUIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Request;

/**
 * plan-154 Slice R, S3-AC-R7/AC-4: proves the browser client ACTUALLY reaches the unbuffered transport end to end,
 * not merely that {@code FileUploadFormNode#unbufferedTransport} is rendered correctly (that half is
 * {@code FileUploadFormElementImplTest}, react4j-core-components) and not merely that the raw endpoint itself works
 * against a hand-built HTTP request (that half is {@code UnbufferedUploadTransportEndToEndTest}, this module). Before
 * this slice, {@code FileUpload.tsx} never branched on the rendered flag, so an opted-in element in a real browser
 * would have posted multipart to {@code /ui/upload/raw} - rejected at the first server guard, and spilling a
 * plaintext temp file on the way there (the exact defect this slice exists to close). This test is what makes that
 * regression impossible to reintroduce silently: it fails if the client ever stops branching.
 * <p>
 * Deliberately its own minimal {@code @SpringBootApplication} rather than reusing {@link org.omnaest.react4j.MockApplication}
 * (mirrors {@code UnbufferedUploadTransportEndToEndTest}/{@code UnbufferedUploadTransportGlobalCeilingTest}) - the
 * shared showcase page's {@code FileUpload} element does not opt in, and changing it would remove browser coverage of
 * the default (multipart) transport that {@link FormFileUploadRoundTripIT} already provides.
 * <p>
 * Excluded from default {@code mvn test} via {@code @Tag("browser")} + {@code <excludedGroups>browser</excludedGroups>}
 * POM property (see memory surefire-excludedgroups-property-not-config-literal / plan-74 Cliff C5, mirroring
 * {@link FormFileUploadRoundTripIT}).
 */
@Tag("browser")
@SpringBootTest(classes = UnbufferedUploadTransportBrowserRoundTripIT.TestApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
public class UnbufferedUploadTransportBrowserRoundTripIT
{
    @LocalServerPort
    private int            port;

    @Autowired
    private ReactUIService reactUIService;

    private Playwright     playwright;
    private Browser        browser;
    private Page           page;

    @SpringBootApplication
    @EnableReactUI
    @EnableReactUIInMemoryRepository
    @Import(WebSecurityConfiguration.class)
    public static class TestApplication
    {
    }

    @BeforeEach
    public void openBrowser()
    {
        this.playwright = Playwright.create();
        this.browser = this.playwright.chromium()
                                      .launch(new BrowserType.LaunchOptions().setHeadless(true));
        this.page = this.browser.newPage();
    }

    @AfterEach
    public void closeBrowser()
    {
        if (this.browser != null)
        {
            this.browser.close();
        }
        if (this.playwright != null)
        {
            this.playwright.close();
        }
    }

    @Test
    public void anOptedInElementUploadsEndToEndThroughARealBrowser(@TempDir Path tempDir) throws IOException
    {
        ByteArrayChannel channel = ByteArrayChannel.create();
        this.registerFormWithOptedInChannel(channel);

        // A filename containing a space -- one of the two wire-contract traps this slice closes (trap b, section
        // 2 of the brief): the server decodes X-Filename with URLDecoder.decode(value, UTF_8), which turns a
        // literal '+' into a space and is otherwise compatible with encodeURIComponent. A real browser round trip
        // is what proves the CLIENT actually encodes with the compatible scheme, not merely that a unit test's
        // mocked axios call was shaped correctly.
        String uploadFilename = "my upload.txt";
        byte[] payload = "hello from a real browser, unbuffered".getBytes(StandardCharsets.UTF_8);
        Path uploadFile = tempDir.resolve(uploadFilename);
        Files.write(uploadFile, payload);

        this.page.navigate("http://localhost:" + this.port + "/");

        List<Request> capturedRawUploadRequests = new ArrayList<>();
        this.page.onRequest(request ->
        {
            if (request.url()
                       .endsWith("/ui/upload/raw"))
            {
                capturedRawUploadRequests.add(request);
            }
        });

        Locator fileInput = this.page.locator("input[type=file]");
        fileInput.waitFor(new Locator.WaitForOptions().setTimeout(10000));
        fileInput.setInputFiles(uploadFile);

        // FileUpload's own client-rendered confirmation -- proves the round trip completed and the server-issued
        // receipt (filename) reached the DOM, exactly as FormFileUploadRoundTripIT checks for the default transport.
        Locator uploadStatus = this.page.locator("[role='status']", new Page.LocatorOptions().setHasText("Uploaded:"));
        uploadStatus.waitFor(new Locator.WaitForOptions().setTimeout(10000));
        assertTrue(uploadStatus.textContent()
                               .contains(uploadFilename),
                   "Upload confirmation must reflect the uploaded file's name");

        this.page.waitForFunction("document.querySelector('.App')?.getAttribute('data-inflight-count') === '0'");

        // --- AC-1: the browser sent a RAW-BODY POST to the rendered uploadUrl, never multipart -----------------
        assertEquals(1, capturedRawUploadRequests.size(), "Exactly one request must have reached /ui/upload/raw");
        Request rawUploadRequest = capturedRawUploadRequests.get(0);

        String uploadIdHeader = rawUploadRequest.headers()
                                                .get("x-upload-id");
        assertNotNull(uploadIdHeader, "X-Upload-Id header must be present on the raw-body request");
        assertFalse(uploadIdHeader.isBlank(), "X-Upload-Id header must not be blank");

        String filenameHeader = rawUploadRequest.headers()
                                                .get("x-filename");
        assertNotNull(filenameHeader, "X-Filename header must be present on the raw-body request");
        assertEquals(uploadFilename, URLDecoder.decode(filenameHeader, StandardCharsets.UTF_8),
                     "X-Filename must decode (via the server's own URLDecoder.decode(value, UTF_8)) back to the exact client filename");

        // --- AC-2/AC-3 (trap a): Content-Type must never be a form content type ---------------------------------
        String contentTypeHeader = rawUploadRequest.headers()
                                                   .get("content-type");
        assertNotNull(contentTypeHeader, "Content-Type header must be present on the raw-body request");
        assertFalse(contentTypeHeader.toLowerCase()
                                     .startsWith("multipart/"),
                    "A real browser round trip on the unbuffered transport must never send multipart/*: " + contentTypeHeader);
        assertFalse(contentTypeHeader.equalsIgnoreCase("application/x-www-form-urlencoded"),
                    "A real browser round trip on the unbuffered transport must never fall back to application/x-www-form-urlencoded "
                                                                                             + "(the one content type the servlet container parses into request parameters, silently consuming the body): "
                                                                                             + contentTypeHeader);

        // --- AC-4: the upload actually reached the channel with the exact bytes ---------------------------------
        assertTrue(channel.getContent()
                          .isPresent(),
                   "The opted-in channel must have received the uploaded content");
        assertArrayEquals(payload, channel.getContent()
                                          .get()
                                          .asBytes(),
                          "The bytes the channel received must match the uploaded file exactly");
    }

    private void registerFormWithOptedInChannel(ByteArrayChannel channel)
    {
        this.reactUIService.createDefaultRoot(reactUI -> reactUI.addNewComponent(factory -> factory.newForm()
                                                                                                   .withUIContext((form, context) ->
                                                                                                   {
                                                                                                       form.attachTo(context.getFirstDocument());
                                                                                                       form.addFileUpload(fileUpload -> fileUpload.withUploadChannel(channel)
                                                                                                                                                  .withUnbufferedTransport());
                                                                                                   })));
    }
}
