package org.omnaest.react4j.browser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
 * plan-74 Goal-2 fan-out (F3), the "rerendering / interval" archetype: an
 * {@code IntervalRerenderingContainer} self-refreshes its content on a client-side timer (see
 * {@code IntervalRerenderingContainer.tsx}'s {@code setInterval(() => this.reloadChildrenAndRefresh(),
 * intervalDuration)}), polling {@code GET /ui} sub-node data -- WITHOUT going through a
 * user-triggered {@code /ui/event}/{@code /ui/upload} round trip.
 *
 * <p>
 * <b>Settle-signal scope note:</b> {@code Backend.getUISubNode} (the polling call this container
 * uses) is deliberately NOT wrapped by {@code InFlightTracker} -- only {@code /ui/event} and
 * {@code /ui/upload} round trips increment/decrement {@code data-inflight-count} (Cliff C2). This
 * test therefore does NOT wait on the settle signal; it waits, timing-tolerantly, for the rendered
 * "Server time: HH:mm:ss" text itself to change, bounded by a single {@code waitForFunction} call
 * (no hard sleeps) generous enough to span at least two of the component's configured 2-second
 * refresh cycles.
 *
 * <p>
 * Uses {@link org.omnaest.react4j.ComponentShowcaseUI#buildShowcase}'s
 * {@code IntervalRerenderingContainer} card, configured with
 * {@code withIntervalDuration(2, TimeUnit.SECONDS)}.
 *
 * <p>
 * Excluded from default {@code mvn test} via {@code @Tag("browser")} +
 * {@code <excludedGroups>browser</excludedGroups>} POM property (see memory
 * surefire-excludedgroups-property-not-config-literal / plan-74 Cliff C5, mirroring
 * {@link ToggleButtonRoundTripIT}).
 */
@Tag("browser")
@SpringBootTest(classes = MockApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
public class IntervalRerenderingContainerLiveUpdateIT
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
    public void serverTimeTextUpdatesOnItsOwnIntervalWithoutUserInteraction()
    {
        this.page.navigate("http://localhost:" + this.port + "/");

        // Scoped to ".card-inner-body p" (not just "p"): Card.tsx wraps its OWN title in a <p><h4
        // class="card-title">...</h4></p> wrapper, so an unscoped "p" locator on the card would
        // ambiguously match both the title wrapper and the actual Paragraph content.
        Locator card = this.page.locator(".card", new Page.LocatorOptions().setHasText("IntervalRerenderingContainer"));
        Locator serverTimeText = card.locator(".card-inner-body p");
        serverTimeText.waitFor(new Locator.WaitForOptions().setTimeout(10000));

        String initialText = serverTimeText.textContent();
        assertTrue(initialText.contains("Server time:"), "Container must render the 'Server time: HH:mm:ss' text");

        // Bounded, timing-tolerant wait (no hard sleep): allow up to ~5s (roughly two 2s refresh
        // cycles plus network/render slack) for the polled content to change. "Server time:" is the
        // only paragraph with that prefix on the page, so a plain DOM scan is unambiguous.
        this.page.waitForFunction(
                                  "(expected) => { const p = Array.from(document.querySelectorAll('p')).find(el => el.textContent.startsWith('Server time:')); return !!p && p.textContent !== expected; }",
                                  initialText, new Page.WaitForFunctionOptions().setTimeout(5000));

        String updatedText = serverTimeText.textContent();
        assertNotEquals(initialText, updatedText, "Server time text must have changed after the interval refresh");
        assertTrue(updatedText.contains("Server time:"), "Refreshed content must still render the 'Server time:' label");
    }

    /**
     * plan-235 S3 AC-BROWSER-6: the interval-wrapped subtree both TICKS on its own timer AND still HANDLES a
     * click, observed across at least two ticks. The two halves need two DIFFERENT waits (see
     * {@code ComponentShowcaseUI#createIntervalContent}, which is what added the clickable
     * {@code Button} this test exercises - the original wiring had no clickable child at all):
     * <ul>
     * <li>the tick half does NOT go through {@code /ui/event} ({@code Backend.getUISubNode} is not wrapped by
     * {@code InFlightTracker}), so it is observed with a bounded {@code page.waitForFunction} on the rendered DOM
     * text, exactly like {@link #serverTimeTextUpdatesOnItsOwnIntervalWithoutUserInteraction()} above;</li>
     * <li>the click half DOES go through {@code /ui/event}, so it is observed via the
     * {@code .App[data-inflight-count]==="0"} settle signal ({@link #waitForSettled()}).</li>
     * </ul>
     * Mixing these up produces a flaky test that gets blamed on the fix under test rather than on the test itself.
     */
    @Test
    public void intervalTicksAcrossTwoRefreshesAndStillHandlesClicks()
    {
        this.page.navigate("http://localhost:" + this.port + "/");

        Locator card = this.page.locator(".card", new Page.LocatorOptions().setHasText("IntervalRerenderingContainer"));
        Locator serverTimeText = card.locator(".card-inner-body p");
        serverTimeText.waitFor(new Locator.WaitForOptions().setTimeout(10000));

        String textAfterLoad = serverTimeText.textContent();

        // Tick 1: bounded wait on the rendered DOM text itself, no settle signal (does not go through /ui/event).
        this.page.waitForFunction(
                                  "(expected) => { const p = Array.from(document.querySelectorAll('p')).find(el => el.textContent.startsWith('Server time:')); return !!p && p.textContent !== expected; }",
                                  textAfterLoad, new Page.WaitForFunctionOptions().setTimeout(5000));
        String textAfterTick1 = serverTimeText.textContent();
        assertNotEquals(textAfterLoad, textAfterTick1, "Server time text must change after the FIRST tick");

        // Tick 2: same bounded-wait technique, relative to tick 1's text.
        this.page.waitForFunction(
                                  "(expected) => { const p = Array.from(document.querySelectorAll('p')).find(el => el.textContent.startsWith('Server time:')); return !!p && p.textContent !== expected; }",
                                  textAfterTick1, new Page.WaitForFunctionOptions().setTimeout(5000));
        String textAfterTick2 = serverTimeText.textContent();
        assertNotEquals(textAfterTick1, textAfterTick2, "Server time text must change AGAIN after the SECOND tick");

        // Click half: goes through /ui/event, so wait on the settle signal, never the DOM-text waitForFunction
        // used for the ticks above.
        Locator counterButton = card.locator(".card-inner-body button", new Locator.LocatorOptions().setHasText("Bump Interval Counter"));
        assertEquals(1, counterButton.count(), "Expected exactly one 'Bump Interval Counter' button inside the IntervalRerenderingContainer card");
        assertTrue(counterButton.textContent()
                                .contains("(0)"),
                   "Counter button must start at 0 clicks");

        counterButton.click();
        this.waitForSettled();

        Locator counterButtonAfterClick = card.locator(".card-inner-body button", new Locator.LocatorOptions().setHasText("Bump Interval Counter"));
        assertTrue(counterButtonAfterClick.textContent()
                                          .contains("(1)"),
                   "Counter button must read 1 click after the settled round trip - the interval-wrapped subtree "
                                                            + "must still handle a click while it keeps ticking on its own timer");
    }

    private void waitForSettled()
    {
        this.page.waitForFunction("document.querySelector('.App')?.getAttribute('data-inflight-count') === '0'");
        assertEquals("0", this.page.locator(".App")
                                   .getAttribute("data-inflight-count"));
    }
}
