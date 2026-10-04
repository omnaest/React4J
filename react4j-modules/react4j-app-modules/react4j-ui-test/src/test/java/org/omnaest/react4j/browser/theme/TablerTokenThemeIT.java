package org.omnaest.react4j.browser.theme;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.omnaest.react4j.MockApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;

import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.FrameLocator;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

/**
 * plan-277 T3, AC6: with the Tabler preset, a primary colour of {@value TablerTokenThemeTestConfiguration#PRIMARY} and a base radius of
 * {@value TablerTokenThemeTestConfiguration#RADIUS} configured through the Java API, a real browser must render the showcase's "Open modal" button in
 * that colour and radius, derive its hover shade from that colour (not Tabler's own), colour a link from it, and do so in dark mode as well. The
 * block that achieves it holds Tabler base variables only: nothing here is derived in Java, so every shade asserted below is what Tabler itself computes
 * in the browser from the base variable.
 * <br>
 * Freshness first: the SERVED page must link the Tabler sheet and carry the token block, and the served sheet must be Tabler's, so a stale core or
 * core-ui jar fails loudly instead of making the computed-style assertions meaningless (testing P12).
 */
@Tag("browser")
@SpringBootTest(classes = MockApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles(TablerTokenThemeTestConfiguration.PROFILE)
public class TablerTokenThemeIT extends ThemeBrowserSupport
{
    private static final String TABLER_STYLESHEET_PATH = "/css/theme/react4j-tabler.css";
    private static final String PRIMARY_RGB            = "rgb(15, 118, 110)";
    private static final int[]  PRIMARY_BYTES          = {15, 118, 110};

    private static final String PROBE_FRAME_ID         = "r4j-default-probe";

    @Test
    public void testServedPageLinksTheTablerSheetAndCarriesTheBaseVariableBlockAfterItAndTheServedSheetIsTablers()
    {
        String servedHtml = this.fetchServedIndexHtml();
        String head = servedHtml.substring(0, servedHtml.indexOf("</head>"));

        int linkIndex = head.indexOf("<link rel=\"stylesheet\" href=\"" + TABLER_STYLESHEET_PATH + "?");
        int styleIndex = head.indexOf("<style>");
        int appStylesheetIndex = head.indexOf("href=\"/css/color.css");
        assertTrue(linkIndex >= 0, "freshness: no Tabler link in the served head, a stale core jar was probably used: " + head);
        assertTrue(styleIndex > linkIndex, "freshness: the served head carries no token style block after the Tabler link, a stale core jar was probably used: " + head);
        assertTrue(appStylesheetIndex < 0 || styleIndex < appStylesheetIndex, "the token block must precede the application stylesheets: " + head);
        assertFalse(head.contains("react4j-modern.css"), head);
        assertTrue(head.contains("--bs-primary:" + TablerTokenThemeTestConfiguration.PRIMARY + ";--bs-primary-rgb:15, 118, 110"), head);
        assertTrue(head.contains("--bs-border-radius-md:" + TablerTokenThemeTestConfiguration.RADIUS), head);
        assertFalse(head.contains(".btn-primary"), "no component rule may be written for Tabler: " + head);

        APIResponse sheet = this.page.request()
                                     .get(this.baseUrl() + TABLER_STYLESHEET_PATH);
        String sheetText = sheet.text();
        assertEquals(200, sheet.status());
        assertTrue(sheetText.startsWith("/*!"), "freshness: the served sheet is not the bannered Tabler sheet, a stale core-ui jar was probably used: "
                                                + sheetText.substring(0, Math.min(80, sheetText.length())));
        assertTrue(sheetText.substring(0, 400)
                            .contains("Tabler v"),
                   sheetText.substring(0, 400));
    }

    @Test
    public void testOpenModalButtonIsRenderedInTheConfiguredPrimaryColourAndRadius()
    {
        Locator openModalButton = this.openShowcaseAndLocateOpenModalButton();

        // Tabler's primary button has a transparent border (the modern theme's has the primary one), so the background is the one colour to read
        assertEquals(PRIMARY_RGB, computedStyle(openModalButton, "background-color"));
        assertEquals("12px", computedStyle(openModalButton, "border-top-left-radius"));
        assertEquals("12px", computedStyle(openModalButton, "border-bottom-right-radius"));
    }

    /**
     * The hover shade is derived by Tabler in the browser from the base variable ({@code oklch(from var(--bs-primary) ...)}), nothing in the token block
     * says anything about it. Expected: not the unhovered colour, darker than it, and not what Tabler derives for ITS OWN primary, which is measured
     * here in a second document that loads the same stylesheet without the token block (an iframe), through the same route and the same browser. The
     * expectation is never read from the element under test.
     */
    @Test
    public void testOpenModalButtonHoverIsDarkerThanTheConfiguredPrimaryAndNotTablersOwnHover()
    {
        Locator openModalButton = this.openShowcaseAndLocateOpenModalButton();
        assertEquals(PRIMARY_RGB, computedStyle(openModalButton, "background-color"), "not hovered yet");

        int[] tablerOwnHover = this.measureTablersOwnPrimaryButtonHover();

        openModalButton.hover();
        String hoveredText = this.awaitSettledChange(openModalButton, "background-color", PRIMARY_RGB);
        int[] hovered = this.colorBytes(hoveredText);

        System.out.println("TablerTokenThemeIT measured hover: configured primary -> " + hoveredText + " (sRGB " + List.of(hovered[0], hovered[1], hovered[2])
                           + "), Tabler's own primary -> sRGB " + List.of(tablerOwnHover[0], tablerOwnHover[1], tablerOwnHover[2]));
        assertNotEquals(PRIMARY_RGB, hoveredText, "hover must change the background");
        assertTrue(luminance(hovered) < luminance(PRIMARY_BYTES), "the hover shade must be darker than the primary: " + hoveredText);
        assertTrue(maxChannelDistance(hovered, tablerOwnHover) > 20, "the hover shade " + hoveredText + " must follow the configured primary, not Tabler's own hover "
                                                                     + List.of(tablerOwnHover[0], tablerOwnHover[1], tablerOwnHover[2]));
    }

    /**
     * Tabler draws the breadcrumb link colour from its link colour, which is the primary colour in light mode ({@code --bs-breadcrumb-link-color:
     * var(--bs-link-color)}, {@code --bs-link-color: light-dark(var(--bs-primary), ...)}). The showcase's "Home" breadcrumb entry is that element.
     */
    @Test
    public void testALinkIsColouredFromTheConfiguredPrimary()
    {
        this.openShowcaseAndLocateOpenModalButton();
        Locator homeLink = this.page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Home")
                                                                                         .setExact(true));
        homeLink.waitFor(new Locator.WaitForOptions().setTimeout(15000));

        int[] linkColor = this.colorBytes(computedStyle(homeLink, "color"));

        assertTrue(maxChannelDistance(linkColor, PRIMARY_BYTES) <= 1, "the breadcrumb link must be drawn in the configured primary, got " + computedStyle(homeLink, "color"));
    }

    /**
     * The same colour must hold with the dark colour mode active. The attribute is set in the page and the background re-read; the control is that the
     * dark scope is really in effect (the page background turns darker), so the test cannot pass merely because the attribute had no consequence.
     */
    @Test
    public void testThePrimaryAlsoHoldsInDarkMode()
    {
        Locator openModalButton = this.openShowcaseAndLocateOpenModalButton();
        assertEquals(PRIMARY_RGB, computedStyle(openModalButton, "background-color"), "light mode first");
        int[] lightPage = this.colorBytes(this.pageBackground());

        this.page.evaluate("() => document.documentElement.setAttribute('data-bs-theme', 'dark')");
        String darkPageText = this.awaitSettledChange(this.page.locator("body"), "background-color", this.pageBackground());
        int[] darkPage = this.colorBytes(darkPageText);

        assertEquals("dark", this.page.evaluate("() => document.documentElement.getAttribute('data-bs-theme')"));
        assertTrue(luminance(darkPage) < luminance(lightPage), "control: the dark scope must be in effect, the page went from " + List.of(lightPage[0], lightPage[1], lightPage[2])
                                                               + " to " + darkPageText);
        assertEquals(PRIMARY_RGB, computedStyle(openModalButton, "background-color"), "dark mode must not reset the configured primary");
        assertEquals("12px", computedStyle(openModalButton, "border-top-left-radius"), "dark mode must not reset the configured radius");
    }

    // ---- measuring Tabler's own default, in a document without the token block ---------------------------------------------

    /**
     * Loads the served Tabler stylesheet into a second, same-origin document (an iframe whose {@code srcdoc} links nothing else), hovers a primary
     * button there and returns its hover background as sRGB bytes: Tabler's derivation for Tabler's own primary.
     */
    private int[] measureTablersOwnPrimaryButtonHover()
    {
        this.page.evaluate("() => new Promise(resolve => { const frame = document.createElement('iframe'); frame.id = '" + PROBE_FRAME_ID + "';"
                           + " frame.style.cssText = 'position:fixed;left:0;top:0;width:320px;height:120px;border:0;z-index:2147483647;background:#fff';"
                           + " frame.onload = () => resolve(true);"
                           + " frame.srcdoc = '<!doctype html><html><head><link rel=\"stylesheet\" href=\"" + TABLER_STYLESHEET_PATH + "\"></head>"
                           + "<body style=\"margin:0\"><button type=\"button\" class=\"btn btn-primary\" id=\"probe\">Probe</button></body></html>';"
                           + " document.body.appendChild(frame); })");
        FrameLocator frame = this.page.frameLocator("#" + PROBE_FRAME_ID);
        Locator probe = frame.locator("#probe");
        probe.waitFor(new Locator.WaitForOptions().setTimeout(15000));

        String unhovered = this.awaitSettledChange(probe, "background-color", "");
        assertNotEquals(PRIMARY_RGB, unhovered, "the probe must show Tabler's own primary, not the configured one");
        probe.hover();
        String hovered = this.awaitSettledChange(probe, "background-color", unhovered);
        assertNotEquals(unhovered, hovered, "Tabler's own primary button must change its background on hover");
        int[] bytes = this.colorBytes(hovered);

        this.page.evaluate("() => document.getElementById('" + PROBE_FRAME_ID + "').remove()");
        this.page.mouse()
                 .move(0, 0);
        return bytes;
    }

    // ---- browser helpers -------------------------------------------------------------------------------------------------

    private String pageBackground()
    {
        return (String) this.page.evaluate("() => getComputedStyle(document.body).backgroundColor");
    }

    /**
     * Polls until the property differs from {@code before} and then stops changing (Tabler transitions its colours), or four seconds passed, and
     * returns the last reading, so a failure shows what the browser actually computed
     */
    private String awaitSettledChange(Locator element, String property, String before)
    {
        String previous = computedStyle(element, property);
        long deadline = System.currentTimeMillis() + 4000;
        while (System.currentTimeMillis() < deadline)
        {
            this.page.waitForTimeout(120);
            String current = computedStyle(element, property);
            if (current.equals(previous) && !current.equals(before))
            {
                return current;
            }
            previous = current;
        }
        return previous;
    }

    /**
     * The sRGB bytes the browser's own canvas makes of any CSS colour text, including {@code oklch()}, {@code color-mix()} and {@code color()}
     * results, which {@code getComputedStyle} serialises in their own space instead of as {@code rgb()}
     */
    private int[] colorBytes(String cssColor)
    {
        Object result = this.page.evaluate("(css) => { const canvas = document.createElement('canvas'); canvas.width = 1; canvas.height = 1;"
                                           + " const context = canvas.getContext('2d'); context.fillStyle = '#010203'; context.fillStyle = css;"
                                           + " if (context.fillStyle === '#010203') { return null; }"
                                           + " context.clearRect(0, 0, 1, 1); context.fillRect(0, 0, 1, 1);"
                                           + " return Array.from(context.getImageData(0, 0, 1, 1).data); }",
                                           cssColor);
        assertTrue(result instanceof List, "the browser could not parse the colour '" + cssColor + "'");
        List<?> data = (List<?>) result;
        return new int[] {((Number) data.get(0)).intValue(), ((Number) data.get(1)).intValue(), ((Number) data.get(2)).intValue()};
    }

    // ---- colour arithmetic (WCAG relative luminance) ----------------------------------------------------------------------

    private static double luminance(int[] rgb)
    {
        return 0.2126 * linear(rgb[0]) + 0.7152 * linear(rgb[1]) + 0.0722 * linear(rgb[2]);
    }

    private static double linear(int channel)
    {
        double value = channel / 255.0;
        return value <= 0.03928 ? value / 12.92 : Math.pow((value + 0.055) / 1.055, 2.4);
    }

    private static int maxChannelDistance(int[] first, int[] second)
    {
        int distance = 0;
        for (int index = 0; index < 3; index++)
        {
            distance = Math.max(distance, Math.abs(first[index] - second[index]));
        }
        return distance;
    }
}
