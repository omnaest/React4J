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
 * plan-74 Goal-2 fan-out (F3), the "navigation" archetype: clicking a {@code NavigationBar} entry
 * scrolls the page to its linked in-page target.
 *
 * <p>
 * <b>Honest mechanism, documented:</b> {@code NavigationBar.tsx} renders a plain
 * {@code <a href="#nav-target-a">} (see {@link org.omnaest.react4j.ComponentShowcaseUI}'s
 * {@code withNavigationBar(...withLinkedLocator(...))} wired to a
 * {@code Card.withLinkLocator(...)} target -- confirmed via memory
 * {@code react4j-navigationbar-anchor-navigation}). Clicking it is a native browser anchor
 * navigation (URL fragment change + native scroll-into-view), NOT a client-side router and NOT a
 * content-swap -- React4J's {@code NavigationBar} has no routing capability. This test asserts
 * exactly that mechanism: the URL fragment changes, the linked target {@code Card} is scrolled into
 * the viewport, and -- to make the "not a content swap" half explicit rather than merely
 * asserted-by-omission -- the rest of the page (the top {@code Heading} and the second nav entry)
 * remains present and unchanged. It does NOT assert content replacement, because React4J's
 * NavigationBar does not replace content.
 *
 * <p>
 * <b>Known gap (plan-74 F3, out of this brief's scope):</b> a locale-switch test is intentionally
 * NOT written. The served {@link org.omnaest.react4j.ComponentShowcaseUI} wires no locale-switching
 * affordance -- no {@code LocaleContext}/{@code WebDomainContext} usage (see memory
 * {@code react4j-locale-domain-i18n-switching}), no persisted per-locale domain, and no UI control
 * to switch locales. There is nothing in the served app for a locale-switch browser test to drive;
 * fabricating one would test framework capability the showcase never exercises. Wiring a
 * locale-switch affordance into the showcase (and its browser test) is left for plan-74 F3
 * follow-up.
 *
 * <p>
 * Excluded from default {@code mvn test} via {@code @Tag("browser")} +
 * {@code <excludedGroups>browser</excludedGroups>} POM property (see memory
 * surefire-excludedgroups-property-not-config-literal / plan-74 Cliff C5, mirroring
 * {@link ToggleButtonRoundTripIT}).
 */
@Tag("browser")
@SpringBootTest(classes = MockApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
public class NavigationBarAnchorScrollIT
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
    public void clickingNavigationEntryAnchorScrollsToLinkedTargetCard()
    {
        this.page.navigate("http://localhost:" + this.port + "/");

        // HomePage.tsx starts with menuCollapsed=true -- the NavigationBar (#navbarContent) is only
        // mounted once the "navbar-toggler" hamburger button is clicked (confirmed by source read).
        Locator navToggler = this.page.locator(".navbar-toggler");
        navToggler.waitFor(new Locator.WaitForOptions().setTimeout(10000));
        navToggler.click();

        Locator navEntry = this.page.locator("a.nav-link", new Page.LocatorOptions().setHasText("Navigation Target A"));
        navEntry.waitFor(new Locator.WaitForOptions().setTimeout(10000));
        assertEquals("#nav-target-a", navEntry.getAttribute("href"), "NavigationBar entry must link via a plain in-page anchor href, not a router path");

        Locator targetCard = this.page.locator("#nav-target-a");
        assertEquals(1, targetCard.count(), "Expected exactly one Card carrying the linked locator id 'nav-target-a'");

        // G2-navigation: native anchor click -> browser sets the URL fragment and scrolls.
        navEntry.click();

        assertTrue(this.page.url()
                            .endsWith("#nav-target-a"),
                   "URL fragment must be updated by the native anchor navigation");

        Boolean targetInViewport = (Boolean) this.page.evaluate(
                                                                "() => { const el = document.getElementById('nav-target-a'); if (!el) { return false; } const r = el.getBoundingClientRect(); return r.top >= 0 && r.top <= window.innerHeight; }");
        assertTrue(targetInViewport, "Linked target Card must be scrolled into the viewport after the anchor navigation");

        // Not a content swap: the rest of the page is untouched -- the top-level Heading and the
        // OTHER navigation entry are both still present. Scoped to a direct "body-*" child (not a
        // bare "h2"): react-bootstrap's Accordion also renders its own nested <h2
        // class="accordion-header">, and HomePage.tsx's wrapping div is ".body-full" when the nav
        // menu is collapsed but ".body-bottom" once expanded (as it is here, post-toggler-click).
        assertEquals(1, this.page.locator(".body-bottom > h2", new Page.LocatorOptions().setHasText("Component Showcase"))
                                 .count(),
                     "Top-level page Heading must remain present -- anchor navigation must not swap page content");
        assertEquals(1, this.page.locator("a.nav-link", new Page.LocatorOptions().setHasText("Navigation Target B"))
                                 .count(),
                     "The other NavigationBar entry must remain present -- anchor navigation must not swap page content");
    }
}
