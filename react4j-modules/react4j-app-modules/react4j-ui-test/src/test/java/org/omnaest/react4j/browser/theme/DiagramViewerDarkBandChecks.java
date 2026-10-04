package org.omnaest.react4j.browser.theme;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.omnaest.react4j.ComponentShowcaseUI;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/**
 * plan-277 F3 (kanban 866cf42b item 2), COVERAGE ONLY: what must hold for a DiagramViewer's control band in DARK colour mode, as one check logic for
 * the modern and the Tabler preset (the concrete subclasses choose the preset).
 * <p>
 * {@code .diagram-viewer} paints {@code var(--bs-white)} and stays a light island in dark mode on purpose (plan-274 S2), while the zoom select takes
 * the dark form colours and the three buttons carry no class at all. Expected: the island stays light (i), and the text of every band control is
 * readable on the background it is actually drawn on (ii). The effective background is the control's own computed background, or, where that is
 * transparent, the nearest non-transparent ancestor's (translucent layers are composited). Readable means WCAG contrast of at least 4.5:1 (AA for
 * normal text).
 * <p>
 * Anti-vacuity: the page must be measurably dark (control) so that "the viewer is light" cannot hold merely because everything is light, and exactly four
 * controls must be found so that a loop over nothing cannot pass.
 */
abstract class DiagramViewerDarkBandChecks extends ThemeBrowserSupport
{
    /**
     * WCAG 2.x AA contrast for normal text, an external standard
     */
    static final double MIN_TEXT_CONTRAST = 4.5;

    @Test
    public void testViewerStaysALightIslandOnADarkPage()
    {
        Locator viewer = this.openDarkShowcaseAndLocate(".diagram-viewer");
        double pageLuminance = BrowserColors.luminance(this.effectiveBackground(this.page.locator("body")));
        double viewerLuminance = BrowserColors.luminance(this.effectiveBackground(viewer));

        System.out.println(this.getClass()
                               .getSimpleName()
                           + " measured luminance: page " + pageLuminance + ", diagram viewer " + viewerLuminance);
        assertTrue(pageLuminance < 0.2, "control: the dark colour mode must be in effect, the page background luminance is " + pageLuminance);
        assertTrue(viewerLuminance >= 0.5 && viewerLuminance - pageLuminance >= 0.4,
                   "the diagram viewer must stay a light island, its luminance " + viewerLuminance + " against the page's " + pageLuminance);
    }

    @Test
    public void testEveryBandControlHasReadableTextOnItsOwnBackground()
    {
        this.openDarkShowcaseAndLocate(".diagram-viewer-controls");
        List<String> unreadable = new ArrayList<>();
        for (Locator control : this.bandControls())
        {
            double contrast = this.textContrast(control);
            if (contrast < MIN_TEXT_CONTRAST)
            {
                unreadable.add(this.describe(control) + " has contrast " + contrast);
            }
        }
        assertTrue(unreadable.isEmpty(), "band controls with text below " + MIN_TEXT_CONTRAST + ":1 on their own background: " + unreadable);
    }

    /**
     * Reports, without asserting a colour relation, what the band, the select and the buttons are actually drawn on, so that the numbers behind the two
     * checks above are on record for both presets
     */
    @Test
    public void testBandBackgroundsAreMeasuredAndReported()
    {
        Locator band = this.openDarkShowcaseAndLocate(".diagram-viewer-controls");
        StringBuilder report = new StringBuilder(this.getClass()
                                                     .getSimpleName()
                                                 + " measured band: own background " + computedStyle(band, "background-color") + ", effective "
                                                 + Arrays.toString(this.effectiveBackground(band)));
        for (Locator control : this.bandControls())
        {
            report.append("\n    ")
                  .append(this.describe(control))
                  .append(" background-color ")
                  .append(computedStyle(control, "background-color"))
                  .append(" (effective ")
                  .append(Arrays.toString(this.effectiveBackground(control)))
                  .append("), text ")
                  .append(computedStyle(control, "color"))
                  .append(", contrast ")
                  .append(this.textContrast(control));
        }
        System.out.println(report);
    }

    // ---- helpers ------------------------------------------------------------------------------------------------------------------

    /**
     * Opens the showcase and returns the given sub-element of the one DiagramViewer card the tests measure (located by its card title); fails unless the
     * page is in dark colour mode
     */
    private Locator openDarkShowcaseAndLocate(String selectorInCard)
    {
        this.openShowcaseAndLocateOpenModalButton();
        assertEquals("dark", this.page.evaluate("() => document.documentElement.getAttribute('data-bs-theme')"), "the dark colour mode must be configured");
        Locator element = this.card()
                              .locator(selectorInCard);
        element.first()
               .waitFor(new Locator.WaitForOptions().setTimeout(15000));
        return element.first();
    }

    private Locator card()
    {
        return this.page.locator(".card", new Page.LocatorOptions().setHasText(ComponentShowcaseUI.DIAGRAM_VIEWER_TALL_NARROW_CARD_TITLE))
                        .first();
    }

    /**
     * The zoom select and the three buttons of the control band; exactly four, or the check would pass over nothing
     */
    private List<Locator> bandControls()
    {
        Locator controls = this.card()
                               .locator(".diagram-viewer-controls")
                               .locator("select, button");
        assertEquals(4, controls.count(), "the control band must hold the zoom select and three buttons");
        List<Locator> result = new ArrayList<>();
        for (int index = 0; index < 4; index++)
        {
            result.add(controls.nth(index));
        }
        return result;
    }

    private String describe(Locator control)
    {
        return "<" + control.evaluate("e => e.tagName.toLowerCase()") + " aria-label='" + control.getAttribute("aria-label") + "'>";
    }

    private double textContrast(Locator control)
    {
        int[] background = this.effectiveBackground(control);
        int[] text = this.composite(BrowserColors.rgbaBytes(this.page, computedStyle(control, "color")), background);
        double lighter = Math.max(BrowserColors.luminance(text), BrowserColors.luminance(background));
        double darker = Math.min(BrowserColors.luminance(text), BrowserColors.luminance(background));
        return (lighter + 0.05) / (darker + 0.05);
    }

    /**
     * The colour (sRGB bytes) the element is really drawn on: its own computed background colour, or where that is transparent the nearest ancestor's;
     * translucent layers are composited over what lies below them, and a stack without any opaque layer is composited over white
     */
    private int[] effectiveBackground(Locator element)
    {
        @SuppressWarnings("unchecked")
        List<String> layers = (List<String>) element.evaluate("(e) => { const result = []; for (let n = e; n; n = n.parentElement) { result.push(getComputedStyle(n).backgroundColor); } return result; }");
        int[] below = {255, 255, 255};
        for (int index = layers.size() - 1; index >= 0; index--)
        {
            below = this.composite(BrowserColors.rgbaBytes(this.page, layers.get(index)), below);
        }
        return below;
    }

    private int[] composite(int[] rgba, int[] below)
    {
        double alpha = rgba[3] / 255.0;
        return new int[] {(int) Math.round(rgba[0] * alpha + below[0] * (1 - alpha)), (int) Math.round(rgba[1] * alpha + below[1] * (1 - alpha)),
                (int) Math.round(rgba[2] * alpha + below[2] * (1 - alpha))};
    }
}
