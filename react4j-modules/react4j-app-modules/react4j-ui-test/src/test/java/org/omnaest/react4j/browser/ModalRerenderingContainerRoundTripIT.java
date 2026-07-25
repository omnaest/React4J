package org.omnaest.react4j.browser;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
 * plan-74 Goal-2 fan-out (F3), the "rerendering" archetype: a button-driven
 * {@code RerenderingContainer} re-renders its content after a settled {@code /ui/event} round
 * trip. Uses {@link org.omnaest.react4j.ComponentShowcaseUI#createModalProvider}'s
 * {@code RerenderingContainer} wrapping a {@code Modal} driven by a server-side
 * {@code AtomicBoolean} (the same plan-12/13/14 show/hide pattern the pilot's ToggleButton uses,
 * but here asserting a DIFFERENT DOM outcome -- the Modal newly present/visible -- rather than a
 * toggled input's checked state).
 *
 * <p>
 * <b>Interval half of the archetype -- documented gap, not fabricated:</b> {@code
 * IntervalRerenderingContainer} is a real react4j-core-components archetype
 * ({@code react4j-intervalrerendingcontainer-polling}) but is NOT wired into
 * {@code ComponentShowcaseUI}/{@code MockUI} in this served app (confirmed by source read: no
 * {@code newIntervalRerenderingContainer} call in either class). Per the brief ("use the closest
 * available component or document the gap -- do not fabricate"), the closest available and
 * genuinely-served component demonstrating the "container re-renders on the settle signal" half
 * is this button-driven {@code RerenderingContainer}; the interval/timer half is not covered here
 * and would require the app-provider class ({@code ComponentShowcaseUI}, a UI-tree-construction
 * class routed to {@code frontend-programmer} per memory
 * route-reactuiprovider-classes-to-frontend) to wire an
 * {@code IntervalRerenderingContainer} into the showcase first -- out of this backend-programmer
 * brief's scope.
 *
 * <p>
 * Excluded from default {@code mvn test} via {@code @Tag("browser")} +
 * {@code <excludedGroups>browser</excludedGroups>} POM property (see memory
 * surefire-excludedgroups-property-not-config-literal / plan-74 Cliff C5, mirroring
 * {@link ToggleButtonRoundTripIT}).
 */
@Tag("browser")
@SpringBootTest(classes = MockApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
public class ModalRerenderingContainerRoundTripIT
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
    public void clickingOpenModalRerendersContainerAndShowsModalAfterSettledRoundTrip()
    {
        this.page.navigate("http://localhost:" + this.port + "/");

        Locator openModalButton = this.page.locator("button", new Page.LocatorOptions().setHasText("Open modal"));
        openModalButton.waitFor(new Locator.WaitForOptions().setTimeout(10000));

        Locator modalDialog = this.page.locator(".modal.show");
        assertEquals(0, modalDialog.count(), "Modal must not be present in the DOM before the round trip (react-bootstrap unmounts on hide)");

        // G2-rerender: click -> ServerHandler flips the server-side flag -> RerenderingContainer
        // round trip.
        openModalButton.click();

        // G3-settle: wait for the in-flight counter to return to 0 instead of sleeping.
        this.waitForSettled();

        modalDialog.waitFor(new Locator.WaitForOptions().setTimeout(10000));
        assertEquals(1, modalDialog.count(), "RerenderingContainer must re-render the Modal visible after the settled round trip");
        assertTrue(modalDialog.locator(".modal-title")
                              .textContent()
                              .contains("Demo Modal"),
                   "Re-rendered Modal must show its configured title");

        // Close what this test opened: modalVisible is a server-side singleton field on the
        // ComponentShowcaseUI bean, which Spring Boot Test caches and REUSES across IT classes with
        // an identical @SpringBootTest signature in the same failsafe JVM fork (confirmed: this
        // shared state, left open, made the next-run IT class in the suite -- ToggleButtonRoundTripIT
        // -- start with the Modal already covering the page and time out). Closing it restores the
        // shared singleton to its initial state for whichever IT class runs next.
        modalDialog.locator(".btn-close")
                   .click();
        this.waitForSettled();
        assertEquals(0, this.page.locator(".modal.show")
                                 .count(),
                     "Modal must close again after the settled round trip, restoring shared server-side state for later tests");
    }

    private void waitForSettled()
    {
        this.page.waitForFunction("document.querySelector('.App')?.getAttribute('data-inflight-count') === '0'");
        assertEquals("0", this.page.locator(".App")
                                   .getAttribute("data-inflight-count"));
    }
}
