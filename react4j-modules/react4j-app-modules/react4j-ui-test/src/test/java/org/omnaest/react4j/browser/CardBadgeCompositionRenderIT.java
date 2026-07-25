package org.omnaest.react4j.browser;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
 * plan-74 Goal-2 fan-out (F3), the "container/composition" archetype: a {@code Composite} renders
 * its nested {@code Card} children in build order, and each {@code Card} in turn renders ITS
 * nested content (a {@code Badge}) as an independently identifiable child node.
 *
 * <p>
 * Uses {@link org.omnaest.react4j.ComponentShowcaseUI#buildShowcase}'s top-level
 * {@code Composite}, whose first three children are the "Badge", "Spinner" and "Placeholder"
 * {@code Card}s (in that build order), and the "Badge" {@code Card}'s nested {@code Badge}
 * component (text "New").
 *
 * <p>
 * <b>{@code data-location} -- documented gap, not fabricated:</b> the brief's "each addressable by
 * data-location" half is NOT asserted here for the SAME empirically-confirmed reason documented on
 * {@link HeadingLeafRenderIT}: every static (non-rerendering) node on this page -- Composite, every
 * Card, and the nested Badge -- carries {@code "target":null} server-side (checked via the live
 * {@code GET /ui} JSON), so none of them are wrapped with {@code LocationAttribute} and none carry
 * {@code data-location}. Nested addressability is instead proven here via DOM structure/order and
 * distinct content, which IS observable and correct. This is a framework-level finding, out of
 * this backend-programmer brief's scope to fix -- reported for {@code full-stack-engineer}
 * attention.
 *
 * <p>
 * Excluded from default {@code mvn test} via {@code @Tag("browser")} +
 * {@code <excludedGroups>browser</excludedGroups>} POM property (see memory
 * surefire-excludedgroups-property-not-config-literal / plan-74 Cliff C5, mirroring
 * {@link ToggleButtonRoundTripIT}).
 */
@Tag("browser")
@SpringBootTest(classes = MockApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
public class CardBadgeCompositionRenderIT
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
    public void compositeRendersCardChildrenInOrderEachWithOwnNestedContent()
    {
        this.page.navigate("http://localhost:" + this.port + "/");

        // Build-order proof: the Composite's Card children render in the SAME order they were
        // added (Badge, Spinner, Placeholder, ...).
        Locator cardTitles = this.page.locator(".card-title");
        cardTitles.first()
                  .waitFor(new Locator.WaitForOptions().setTimeout(10000));

        assertEquals("Badge", cardTitles.nth(0)
                                        .textContent(),
                     "First Card child must be Badge (build order)");
        assertEquals("Spinner", cardTitles.nth(1)
                                          .textContent(),
                     "Second Card child must be Spinner (build order)");
        assertEquals("Placeholder", cardTitles.nth(2)
                                              .textContent(),
                     "Third Card child must be Placeholder (build order)");

        // Nested addressability proof: the "Badge" Card renders exactly one nested Badge child,
        // independently identifiable by its own CSS identity and its own distinct rendered text.
        Locator badgeCard = this.page.locator(".card", new Page.LocatorOptions().setHasText("Badge"));
        assertEquals(1, badgeCard.count(), "Expected exactly one Card titled 'Badge'");

        Locator nestedBadge = badgeCard.locator(".badge");
        assertEquals(1, nestedBadge.count(), "Expected exactly one nested Badge component inside the 'Badge' Card");
        assertEquals("New", nestedBadge.textContent(), "Nested Badge must render its configured text");
    }
}
