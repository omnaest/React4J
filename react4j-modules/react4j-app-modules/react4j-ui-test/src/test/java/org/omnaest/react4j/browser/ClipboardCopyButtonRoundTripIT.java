package org.omnaest.react4j.browser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.omnaest.react4j.EnableReactUI;
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
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;

/**
 * plan-157 Cliff Y5, S5-AC-R2/R3/R4: proves the new generic {@code ClipboardCopyButton} React4J component actually
 * writes to, and later clears, the REAL operating-system/browser clipboard - not merely that a JS function was
 * invoked or a timer was scheduled (plan-157 Y12 pre-mortem item 3). A component-level Java unit test
 * ({@code ClipboardCopyButtonImplTest}, react4j-core-components) and a jest test
 * ({@code ClipboardCopyButton.test.tsx}, react4j-core-ui) each already prove their own half of the seam in
 * isolation with a mocked {@code navigator.clipboard}; this test is what proves the two halves agree on the wire
 * (S5-AC-R4) and that the real API genuinely does what both assume it does (S5-AC-R2/R3).
 * <p>
 * Deliberately its own minimal {@code @SpringBootApplication} rather than reusing the shared showcase page - per
 * the recorded procedure {@code react4j browser self contained testapplication plus playwright network capture
 * wire contract proof}, adding a granted-clipboard-permission {@link BrowserContext} here has no reason to touch
 * any EXISTING browser IT's coverage, and a bespoke page keeps this test's DOM to exactly the one component under
 * test (no ambiguity about which button is "the" clipboard button).
 * <p>
 * The component is deliberately generic (S5-AC-R1): its inputs below are exactly a text, a label and a duration -
 * nothing in this test, the node, or the renderer names any application concept.
 * <p>
 * Excluded from default {@code mvn test} via {@code @Tag("browser")} + {@code <excludedGroups>browser</excludedGroups>}
 * POM property (see memory surefire-excludedgroups-property-not-config-literal / plan-74 Cliff C5, mirroring
 * {@link ToggleButtonRoundTripIT}).
 */
@Tag("browser")
@SpringBootTest(classes = ClipboardCopyButtonRoundTripIT.TestApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
public class ClipboardCopyButtonRoundTripIT
{
    private static final String COPIED_VALUE                 = "one-time-token-4f8c9d2a";
    private static final int    CLEAR_AFTER_DURATION_SECONDS = 1;

    @LocalServerPort
    private int                 port;

    @Autowired
    private ReactUIService      reactUIService;

    private Playwright          playwright;
    private Browser             browser;
    private BrowserContext      context;
    private Page                page;

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
        this.reactUIService.createDefaultRoot(reactUI -> reactUI.addNewComponent(factory -> factory.newClipboardCopyButton()
                                                                                                   .withText(COPIED_VALUE)
                                                                                                   .withLabel("Copy")
                                                                                                   .withClearAfterDuration(CLEAR_AFTER_DURATION_SECONDS,
                                                                                                                           TimeUnit.SECONDS)));

        this.playwright = Playwright.create();
        this.browser = this.playwright.chromium()
                                      .launch(new BrowserType.LaunchOptions().setHeadless(true));
        // navigator.clipboard.writeText/readText require BOTH a secure context (satisfied here by
        // localhost, per the component's own javadoc) AND an explicit clipboard permission grant -
        // without this, Chromium rejects the call with a permission error rather than a silent no-op,
        // which would make the test fail for a reason unrelated to the component under test.
        this.context = this.browser.newContext(new Browser.NewContextOptions().setPermissions(Arrays.asList("clipboard-read", "clipboard-write")));
        this.page = this.context.newPage();
    }

    @AfterEach
    public void closeBrowser()
    {
        if (this.context != null)
        {
            this.context.close();
        }
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
    public void clickingCopiesToTheRealClipboardAndAutoClearsAfterTheConfiguredDuration()
    {
        this.page.navigate("http://localhost:" + this.port + "/");

        Locator copyButton = this.page.locator("button", new Page.LocatorOptions().setHasText("Copy"));
        copyButton.waitFor(new Locator.WaitForOptions().setTimeout(10000));

        // S5-AC-R1 / the component's own frozen property: it renders a button only, never the value itself.
        assertFalse(this.page.content()
                             .contains(COPIED_VALUE),
                    "The served page must never contain the copied value itself - only the button that copies it");

        // S5-AC-R2: click -> the REAL clipboard holds the supplied text, not a proxy for "a function ran".
        copyButton.click();
        this.page.waitForFunction("() => navigator.clipboard.readText().then(t => t === '" + COPIED_VALUE + "')");
        String clipboardAfterCopy = this.readClipboard();
        assertEquals(COPIED_VALUE, clipboardAfterCopy, "The real clipboard must hold exactly the copied value right after the click");

        // S5-AC-R3: after the configured duration, the REAL clipboard is empty again.
        this.page.waitForTimeout((CLEAR_AFTER_DURATION_SECONDS * 1000) + 1500);
        this.page.waitForFunction("() => navigator.clipboard.readText().then(t => t === '')");
        String clipboardAfterClear = this.readClipboard();
        assertEquals("", clipboardAfterClear, "The real clipboard must be empty again once the configured clear-after duration has elapsed");
    }

    /**
     * Reads the browser's ACTUAL clipboard content via {@code navigator.clipboard.readText()} - a Playwright
     * {@code page.evaluate} round trip into the real page, not an assertion on any in-page JS state. This is the
     * proof S5-AC-R2/R3 require: the clipboard's real contents, never a mock or a "was the function called" check.
     */
    private String readClipboard()
    {
        Object result = this.page.evaluate("() => navigator.clipboard.readText()");
        return String.valueOf(result);
    }
}
