package org.omnaest.react4j.browser;

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
}
