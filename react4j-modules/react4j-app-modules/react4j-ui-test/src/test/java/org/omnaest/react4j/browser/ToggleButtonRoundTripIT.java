package org.omnaest.react4j.browser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
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
 * plan-74 Cliff C5 / Goal-2 pilot (G2-toggle, the pivotal interactive archetype): a real browser
 * drives the shared render + interaction pipeline end-to-end against the booted
 * {@link MockApplication} (react4j-ui-test / ComponentShowcaseUI). Proves node JSON -> real DOM
 * (Goal 2), the server-derived {@code data-location} selector and {@code data-inflight-count}
 * settle signal (Goal 3), by locating the ToggleButton's checkbox input via {@code data-location},
 * clicking its associated label, waiting for the round-trip to settle, and asserting the toggled
 * DOM state.
 *
 * <p>
 * NOTE on the locator: react-bootstrap's {@code ToggleButton} renders the {@code <input
 * type=checkbox>} and its {@code <label>} as SIBLINGS (not nested) -- {@code data-location} is
 * stamped by {@code LocationAttribute} onto whichever host node {@code ReactDOM.findDOMNode}
 * returns for the component, which here is the {@code <input>}, not the label. The visible text
 * ("Toggle me") lives only on the label, so a combined {@code [data-location]} + text-filter
 * locator never matches -- locate the input by {@code data-location} directly (unique per
 * archetype instance on this page), then click its associated {@code <label for=...>}.
 *
 * <p>
 * Excluded from default {@code mvn test} via the {@code browser} tag +
 * {@code <excludedGroups>browser</excludedGroups>} POM property (see memory
 * surefire-excludedgroups-property-not-config-literal / plan-74 Cliff C5). Run on demand via
 * {@code mvn verify -DexcludedGroups=} (see report for exact invocation).
 */
@Tag("browser")
@SpringBootTest(classes = MockApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
public class ToggleButtonRoundTripIT
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
    public void clickingToggleButtonFlipsPressedStateAfterSettledRoundTrip()
    {
        this.page.navigate("http://localhost:" + this.port + "/");

        // G3-location: locate the pivotal interactive archetype (ToggleButton, driving a
        // RerenderingContainer) via the server-derived data-location selector -- unique on this
        // page. The initial GET /ui page-data fetch (App.componentDidMount -> Backend.getUI) is
        // NOT wrapped by InFlightTracker (only /ui/event and /ui/upload round trips are, per the
        // C2 contract), so wait on the locator's own attach rather than the settle signal here.
        Locator checkboxInput = this.page.locator("input[type=checkbox][data-location]");
        checkboxInput.waitFor(new Locator.WaitForOptions().setTimeout(10000));

        assertEquals(1, checkboxInput.count(), "Expected exactly one ToggleButton checkbox carrying data-location on this page");
        assertTrue(checkboxInput.getAttribute("data-location")
                                .length() > 0,
                   "data-location must be a non-empty server-derived path");
        assertFalse(checkboxInput.isChecked(), "ToggleButton should start unpressed");

        String inputId = checkboxInput.getAttribute("id");
        Locator label = this.page.locator("label[for='" + inputId + "']");

        // G2-toggle: click -> ServerHandler round trip -> re-render.
        label.click();

        // G3-settle: wait for the in-flight counter to return to 0 instead of sleeping.
        this.waitForSettled();

        assertTrue(checkboxInput.isChecked(), "ToggleButton should flip to pressed after the settled round trip");
    }

    private void waitForSettled()
    {
        this.page.waitForFunction("document.querySelector('.App')?.getAttribute('data-inflight-count') === '0'");
        assertEquals("0", this.page.locator(".App")
                                   .getAttribute("data-inflight-count"));
    }
}
