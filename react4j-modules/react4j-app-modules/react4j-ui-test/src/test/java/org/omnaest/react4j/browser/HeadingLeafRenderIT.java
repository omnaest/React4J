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
 * plan-74 Goal-2 fan-out (F3), the "leaf render" archetype: a Heading renders with the correct
 * HTML tag and correct text -- no interaction, no server round trip, just a static-presence check
 * against the real DOM produced by the shared render pipeline.
 *
 * <p>
 * Uses {@link org.omnaest.react4j.ComponentShowcaseUI}'s top-level
 * {@code factory.newHeading().withText("Component Showcase").withLevel(2)} -- the only
 * {@code <h2>} on the page, so the CSS selector alone is a unique, unambiguous locator.
 *
 * <p>
 * <b>{@code data-location} -- documented gap, not fabricated:</b> the brief's "stable
 * data-location" half of this archetype is NOT asserted here. Empirical check (dumped the live
 * {@code GET /ui} JSON and the rendered DOM): this Heading's node carries {@code "target":null}
 * server-side, so {@code Renderer.renderNode}'s {@code if (node && node.target)} guard never wraps
 * it with {@code LocationAttribute} and NO {@code data-location} attribute is emitted -- confirmed
 * across every top-level static node (Heading, every Card, the nested Badge). The ONLY elements on
 * this page that DO carry {@code data-location} are the three nodes living inside a
 * {@code RerenderingContainer}'s dynamically-provided content (the Modal/Offcanvas "Open ..."
 * buttons and the ToggleButton checkbox -- see {@link ModalRerenderingContainerRoundTripIT} and
 * {@link ToggleButtonRoundTripIT}), i.e. Goal 3's "every node carries data-location" claim
 * currently holds only for rerendering-container-scoped subtrees, not for the static page tree.
 * This is a framework-level (react4j-core Location-assignment) finding, not a showcase-content
 * gap, and is out of this backend-programmer brief's scope to fix -- reported for
 * {@code full-stack-engineer} attention.
 *
 * <p>
 * Excluded from default {@code mvn test} via {@code @Tag("browser")} +
 * {@code <excludedGroups>browser</excludedGroups>} POM property (see memory
 * surefire-excludedgroups-property-not-config-literal / plan-74 Cliff C5, mirroring
 * {@link ToggleButtonRoundTripIT}).
 */
@Tag("browser")
@SpringBootTest(classes = MockApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
public class HeadingLeafRenderIT
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
    public void headingRendersCorrectTagAndText()
    {
        this.page.navigate("http://localhost:" + this.port + "/");

        // The CSS type selector "h2" already asserts the correct HTML tag. Scoped to a DIRECT
        // child of the page body -- react-bootstrap's Accordion ALSO renders its own <h2
        // class="accordion-header"> panel headers deep inside a Card, so an unscoped "h2" locator
        // is ambiguous (3 matches) on this showcase page.
        Locator heading = this.page.locator(".body-full > h2");
        heading.waitFor(new Locator.WaitForOptions().setTimeout(10000));

        assertEquals(1, heading.count(), "Expected exactly one top-level <h2> on this page");
        assertEquals("Component Showcase", heading.textContent(), "Heading must render the exact configured text");

        // Static presence stability: re-reading (no interaction, no reload) is identical.
        assertEquals("Component Showcase", heading.textContent(), "Heading text must be stable across reads with no interaction");
    }
}
