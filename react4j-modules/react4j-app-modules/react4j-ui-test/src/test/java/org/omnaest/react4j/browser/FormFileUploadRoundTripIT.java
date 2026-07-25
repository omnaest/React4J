package org.omnaest.react4j.browser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.omnaest.react4j.MockApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;

/**
 * plan-74 Goal-2 fan-out (F3), the "form + file upload" archetype: a text {@code Form} field and a
 * {@code FileUpload} element round-trip against the real server -- filling the name field, setting
 * a real file on the {@code input[type=file]}, then clicking Submit -- and asserts BOTH round-trip
 * outcomes are reflected in the real DOM: (a) the {@code FileUpload} component's own
 * {@code role=status} confirmation ("Uploaded: &lt;filename&gt;") appears after the
 * {@code /ui/upload} + {@code onComplete} event round trip, and (b) the typed name value survives
 * the Submit button's {@code /ui/event} round trip unchanged (proving the submitted field data was
 * carried through the round trip rather than lost/cleared).
 *
 * <p>
 * Uses {@link org.omnaest.react4j.ComponentShowcaseUI#createFormProvider}'s {@code Form} -- one text
 * {@code InputFormElement} (placeholder "Enter your name") bound to a repository-backed
 * {@code Document} field, one {@code FileUploadFormElement}, and a "Submit" button whose
 * {@code onClick} reads the field and always returns {@code data} (the round-trip contract, see
 * memory {@code react4j-form-submit-onclick-bifunction}).
 *
 * <p>
 * <b>Shared-context restore:</b> the bound {@code Document} field is a repository-backed singleton
 * reused across IT classes in the same failsafe JVM fork (the same lesson documented on
 * {@link ModalRerenderingContainerRoundTripIT}). This test restores the name field to empty via a
 * second settled Submit round trip before returning, so a later test (or a later run of this same
 * test, reproduced for stability) does not see stale state.
 *
 * <p>
 * Excluded from default {@code mvn test} via {@code @Tag("browser")} +
 * {@code <excludedGroups>browser</excludedGroups>} POM property (see memory
 * surefire-excludedgroups-property-not-config-literal / plan-74 Cliff C5, mirroring
 * {@link ToggleButtonRoundTripIT}).
 */
@Tag("browser")
@SpringBootTest(classes = MockApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
public class FormFileUploadRoundTripIT
{
    @LocalServerPort
    private int        port;

    private Playwright playwright;
    private Browser    browser;
    private Page       page;

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
    public void submittingNameAndFileRoundTripsToTheDom(@TempDir Path tempDir) throws IOException
    {
        this.page.navigate("http://localhost:" + this.port + "/");

        Locator nameInput = this.page.locator("input[placeholder='Enter your name']");
        nameInput.waitFor(new Locator.WaitForOptions().setTimeout(10000));
        nameInput.fill("Ada Lovelace");

        Path uploadFile = tempDir.resolve("greeting.txt");
        Files.write(uploadFile, "hello from Playwright".getBytes(StandardCharsets.UTF_8));

        Locator fileInput = this.page.locator("input[type=file]");
        fileInput.setInputFiles(uploadFile);

        // FileUpload's own client-rendered confirmation -- proves the /ui/upload round trip
        // completed and the server-issued receipt (filename) reached the DOM.
        Locator uploadStatus = this.page.locator("[role='status']", new Page.LocatorOptions().setHasText("Uploaded:"));
        uploadStatus.waitFor(new Locator.WaitForOptions().setTimeout(10000));
        assertTrue(uploadStatus.textContent()
                               .contains("greeting.txt"),
                   "Upload confirmation must reflect the uploaded file's name");

        // Settle the FileUpload onComplete event round trip before continuing.
        this.waitForSettled();

        Locator submitButton = this.page.locator("button", new Page.LocatorOptions().setHasText("Submit"));

        // G2-form: click Submit -> ServerHandler round trip carrying the client-held name value.
        submitButton.click();
        this.waitForSettled();

        // G2-form outcome: BOTH round-trip results are still reflected in the DOM after settle --
        // the typed name was not lost, and the earlier upload confirmation was not wiped by the
        // Submit round trip's re-render.
        assertEquals("Ada Lovelace", nameInput.inputValue(), "Name field must retain its value after the settled Submit round trip");
        assertTrue(uploadStatus.textContent()
                               .contains("greeting.txt"),
                   "Upload confirmation must persist after the settled Submit round trip");

        // Restore shared server-side Document state (see class javadoc) so later tests/reruns start
        // from an empty name field.
        nameInput.fill("");
        submitButton.click();
        this.waitForSettled();
        assertEquals("", nameInput.inputValue(), "Name field must be restored to empty after the settled restore round trip");
    }

    private void waitForSettled()
    {
        this.page.waitForFunction("document.querySelector('.App')?.getAttribute('data-inflight-count') === '0'");
        assertEquals("0", this.page.locator(".App")
                                   .getAttribute("data-inflight-count"));
    }
}
