package org.omnaest.react4j.browser.theme;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.omnaest.react4j.ComponentShowcaseUI;
import org.omnaest.react4j.MockApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

/**
 * plan-277 T4: the Tabler preset ({@link TablerThemeTestConfiguration}, no design tokens) as a real browser renders the showcase.
 * <p>
 * Freshness first: the SERVED head must link the Tabler sheet and neither the modern nor the stock one, and the sheet must really be loaded as exactly
 * one {@code document.styleSheets} entry with rules, so a stale core or core-ui jar fails loudly instead of making the rendering assertions
 * meaningless (testing P12). The expected primary colour comes from the literal {@code --bs-primary-rgb} triplet of the served sheet and never from the
 * element under test. Components are located by explicit identity (exact accessible name, or the showcase's own card title), never by position.
 */
@Tag("browser")
@SpringBootTest(classes = MockApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles(TablerThemeTestConfiguration.PROFILE)
public class TablerThemeIT extends ThemeBrowserSupport
{
    private static final String  TABLER_STYLESHEET_PATH = "/css/theme/react4j-tabler.css";
    private static final Pattern PRIMARY_RGB_LITERAL    = Pattern.compile("--bs-primary-rgb:\\s*(\\d+),\\s*(\\d+),\\s*(\\d+)");

    /**
     * Tabler 1.6.1's own default primary. Pinning it here makes a Tabler bump a deliberate act (the "how to update Tabler" steps include this test)
     */
    private static final int[]   TABLER_PRIMARY_BYTES   = {6, 111, 209};

    /**
     * oklch -> sRGB conversion in the browser rounds, and Tabler's literal triplet is a rounded copy of the oklch value
     */
    private static final int     COLOUR_TOLERANCE       = 3;

    @Test
    public void testServedHeadLinksTheTablerSheetAndNeitherTheModernNorTheStockOne()
    {
        String servedHtml = this.fetchServedIndexHtml();
        String head = servedHtml.substring(0, servedHtml.indexOf("</head>"));

        assertTrue(head.contains("<link rel=\"stylesheet\" href=\"" + TABLER_STYLESHEET_PATH + "?"),
                   "freshness: the served head must carry the Tabler link, a stale core jar was probably used. Head: " + head);
        assertFalse(head.contains("react4j-modern.css"), head);
        assertFalse(head.contains("bootstrap.min.css"), head);
        assertTrue(head.contains("data-bs-theme=\"light\"") || servedHtml.contains("data-bs-theme=\"light\""), "default colour mode is light: " + head);
    }

    @Test
    public void testServedSheetIsTablersAndIsLoadedExactlyOnceWithRulesInTheBrowser()
    {
        String sheet = this.servedTablerSheet();
        assertTrue(sheet.startsWith("/*!"), "freshness: the served sheet is not the bannered Tabler sheet, a stale core-ui jar was probably used");
        assertTrue(sheet.substring(0, 600)
                        .contains("Tabler v"),
                   sheet.substring(0, 600));

        this.openShowcaseAndLocateOpenModalButton();
        Object loadedRuleCount = this.page.evaluate("() => { const sheets = Array.from(document.styleSheets).filter(s => s.href && s.href.includes('"
                                                    + TABLER_STYLESHEET_PATH + "'));" + " return sheets.length === 1 ? sheets[0].cssRules.length : -sheets.length; }");

        assertTrue(((Number) loadedRuleCount).intValue() > 0,
                   "exactly one document.styleSheets entry for " + TABLER_STYLESHEET_PATH + " with cssRules > 0 expected, got " + loadedRuleCount);
        assertEquals("light", this.page.evaluate("() => document.documentElement.getAttribute('data-bs-theme')"));
    }

    @Test
    public void testOpenModalButtonIsRenderedInTablersOwnDefaultPrimary()
    {
        int[] literal = this.primaryTripletOfTheServedSheet();
        assertEquals(List.of(TABLER_PRIMARY_BYTES[0], TABLER_PRIMARY_BYTES[1], TABLER_PRIMARY_BYTES[2]), List.of(literal[0], literal[1], literal[2]),
                     "the served sheet's own --bs-primary-rgb literal changed: a Tabler bump? update TABLER_PRIMARY_BYTES deliberately");

        Locator openModalButton = this.openShowcaseAndLocateOpenModalButton();
        String background = computedStyle(openModalButton, "background-color");
        int[] bytes = BrowserColors.srgbBytes(this.page, background);

        assertTrue(BrowserColors.maxChannelDistance(bytes, literal) <= COLOUR_TOLERANCE,
                   "the Open modal button must be drawn in Tabler's primary " + List.of(literal[0], literal[1], literal[2]) + " but its background is " + background
                                                                                         + " = sRGB " + List.of(bytes[0], bytes[1], bytes[2]));
    }

    /**
     * plan-274 S3b pinned the band at 40px and every control at 31px in {@code DiagramViewer.css}, because the host's height is what cover and contain
     * both measure against. Tabler's smaller body font and line height must not move that, or {@code DiagramViewerZoomOverflowIT}'s geometry breaks.
     */
    @Test
    public void testDiagramViewerControlBandKeepsItsPinnedHeightUnderTabler()
    {
        this.openShowcaseAndLocateOpenModalButton();
        Locator card = this.page.locator(".card", new Page.LocatorOptions().setHasText(ComponentShowcaseUI.DIAGRAM_VIEWER_TALL_NARROW_CARD_TITLE))
                                .first();
        Locator controls = card.locator(".diagram-viewer-controls");
        controls.waitFor(new Locator.WaitForOptions().setTimeout(15000));

        @SuppressWarnings("unchecked")
        List<Number> heights = (List<Number>) controls.evaluate("(band) => [band.getBoundingClientRect().height, ...Array.from(band.children).map(c => c.getBoundingClientRect().height)]");

        assertEquals(40.0, heights.get(0)
                                  .doubleValue(),
                     0.5, "the control band must stay 40px (4+31+4+1), heights of band and children: " + heights);
        assertTrue(heights.size() > 1, "the band must hold its controls: " + heights);
        for (int index = 1; index < heights.size(); index++)
        {
            assertEquals(31.0, heights.get(index)
                                      .doubleValue(),
                         0.5, "control " + index + " must stay 31px high, heights of band and children: " + heights);
        }
    }

    /**
     * Tabler's own .badge text colour is its muted secondary, unreadable on the solid success colour React4J's Badge asks for; react4j-tabler.scss
     * pairs the solid colour with Tabler's light foreground. Expected: a ratio of at least 2.5 between text and background; Tabler's own white-on-green pairing measures 2.63, the unfixed muted text far less.
     */
    @Test
    public void testSolidBadgeTextIsReadableOnItsBackground()
    {
        this.openShowcaseAndLocateOpenModalButton();
        Locator badge = this.page.locator(".badge", new Page.LocatorOptions().setHasText("New"))
                                 .first();
        badge.waitFor(new Locator.WaitForOptions().setTimeout(15000));

        int[] text = BrowserColors.srgbBytes(this.page, computedStyle(badge, "color"));
        int[] background = BrowserColors.srgbBytes(this.page, computedStyle(badge, "background-color"));
        double lighter = Math.max(BrowserColors.luminance(text), BrowserColors.luminance(background));
        double darker = Math.min(BrowserColors.luminance(text), BrowserColors.luminance(background));
        double contrast = (lighter + 0.05) / (darker + 0.05);

        assertTrue(contrast >= 2.5, "badge text " + List.of(text[0], text[1], text[2]) + " on " + List.of(background[0], background[1], background[2]) + " has contrast " + contrast);
    }

    /**
     * Tabler makes .card-body position:relative, which turns it into the containing block of the toast container and lets the overflow-x:auto of
     * React4J's custom.css clip the toast to its card; custom.css pins position:static. The clip only trims the toast's edges (the showcase toast is
     * taller than its card body, but its centre stays inside it), so a centre probe cannot see it: every edge midpoint of the toast must be hit too.
     */
    @Test
    public void testToastIsNotClippedByItsCard()
    {
        this.openShowcaseAndLocateOpenModalButton();
        Locator toast = this.page.locator(".toast", new Page.LocatorOptions().setHasText("A toast message."))
                                 .first();
        toast.waitFor(new Locator.WaitForOptions().setTimeout(15000));
        this.scrollToCenter(toast);

        assertReachable(toast.locator(".toast-body"), "toast body");
        assertEveryEdgeMidpointReachable(toast, "toast");
    }

    @Test
    public void testStandaloneDropdownOpensAndItsMenuIsVisibleInsideTheViewport()
    {
        this.openShowcaseAndLocateOpenModalButton();
        Locator toggle = this.page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Options")
                                                                                         .setExact(true));
        this.scrollToCenter(toggle);
        toggle.click();

        this.assertMenuVisibleInsideViewport(toggle.locator("xpath=..")
                                                   .locator(".dropdown-menu.show"),
                                             "Action 1", "Action 2");
    }

    @Test
    public void testSplitButtonDropdownOpensAndItsMenuIsVisibleInsideTheViewport()
    {
        this.openShowcaseAndLocateOpenModalButton();
        Locator card = this.page.locator(".card", new Page.LocatorOptions().setHasText("SplitButton"))
                                .last();
        Locator toggle = card.locator("button.dropdown-toggle-split");
        this.scrollToCenter(toggle);
        toggle.click();

        this.assertMenuVisibleInsideViewport(card.locator(".dropdown-menu.show"), "Save as...", "Save a copy");
    }

    @Test
    public void testModalOpensAndIsVisibleInsideTheViewport()
    {
        Locator openModalButton = this.openShowcaseAndLocateOpenModalButton();
        this.scrollToCenter(openModalButton);
        openModalButton.click();

        Locator modal = this.page.locator(".modal.show");
        try
        {
            modal.waitFor(new Locator.WaitForOptions().setTimeout(10000));
            Locator dialog = modal.locator(".modal-dialog");
            assertTrue(dialog.isVisible(), "the modal dialog must be visible");
            assertTrue(modal.locator(".modal-title")
                            .textContent()
                            .contains("Demo Modal"));
            assertInsideViewport(dialog, "modal dialog");
            // the modal fades in (Tabler's own transition), so wait for the end of it instead of reading mid-flight
            this.page.waitForFunction("parseFloat(getComputedStyle(document.querySelector('.modal.show')).opacity) === 1");
            assertEquals("1", computedStyle(modal, "opacity"), "the modal must be fully faded in");
        }
        finally
        {
            // modalVisible is a server side singleton of the cached showcase context: leave it closed even when an assertion failed, or every later
            // test of this context finds its clicks intercepted by the modal backdrop
            if (modal.count() > 0)
            {
                modal.locator(".btn-close")
                     .click();
                this.page.waitForFunction("document.querySelector('.App')?.getAttribute('data-inflight-count') === '0'");
                this.page.waitForFunction("document.querySelectorAll('.modal.show').length === 0");
            }
        }
    }

    /**
     * Centres the element in the viewport (the showcase scrolls inside a container), so that a dropdown menu opening below it has room and the click
     * does not depend on Playwright's own scrolling
     */
    private void scrollToCenter(Locator element)
    {
        element.evaluate("e => e.scrollIntoView({block: 'center'})");
        this.page.waitForTimeout(300);
    }

    private void assertMenuVisibleInsideViewport(Locator menu, String firstItemText, String lastEnabledItemText)
    {
        menu.waitFor(new Locator.WaitForOptions().setTimeout(10000));
        assertTrue(menu.isVisible(), "the dropdown menu must be visible");
        assertTrue(menu.getByText(firstItemText)
                       .first()
                       .isVisible(),
                   "the menu item '" + firstItemText + "' must be visible");
        assertInsideViewport(menu, "dropdown menu");
        assertEquals("1", computedStyle(menu, "opacity"));
        // visible by box is not enough: React4J's custom.css gives .card-body overflow-x:auto, which clips an open menu to its card unless the Tabler
        // sheet lets the card body overflow, so the last ENABLED item (a disabled item has pointer-events:none and passes the pointer through) must be what the pointer actually hits at its centre
        assertReachable(menu.locator(".dropdown-item", new Locator.LocatorOptions().setHasText(lastEnabledItemText))
                            .first(),
                        "last enabled item '" + lastEnabledItemText + "' of the dropdown menu");
    }

    /**
     * The element, or one of its descendants, is what the browser hits at the element's own centre: it is neither clipped away nor covered
     */
    private void assertReachable(Locator element, String what)
    {
        Object hit = element.evaluate("(e) => { const r = e.getBoundingClientRect(); const t = document.elementFromPoint(r.left + r.width / 2, r.top + r.height / 2);"
                                      + " return t === e || e.contains(t) ? 'reachable' : (t ? t.tagName + '.' + t.className : 'nothing'); }");
        assertEquals("reachable", hit, "the " + what + " must be reachable at its centre, but the browser hits " + hit);
    }

    /**
     * The element, or one of its descendants, is what the browser hits just inside each of its four edge midpoints and at its centre: no edge is
     * clipped away by an ancestor's overflow. Midpoints, not corners, so that rounded corners cannot fail it.
     */
    private void assertEveryEdgeMidpointReachable(Locator element, String what)
    {
        Object hit = element.evaluate("(e) => { const r = e.getBoundingClientRect(); const inset = 3; const points = {"
                                      + " top: [r.left + r.width / 2, r.top + inset], bottom: [r.left + r.width / 2, r.bottom - inset],"
                                      + " left: [r.left + inset, r.top + r.height / 2], right: [r.right - inset, r.top + r.height / 2] };"
                                      + " const misses = []; for (const [name, [x, y]] of Object.entries(points)) { const t = document.elementFromPoint(x, y);"
                                      + " if (!(t === e || e.contains(t))) { misses.push(name + ' -> ' + (t ? t.tagName + '.' + t.className : 'nothing')); } }"
                                      + " return misses.length === 0 ? 'reachable' : misses.join(', '); }");
        assertEquals("reachable", hit, "the " + what + " must be reachable at the middle of each of its edges (no edge clipped by an ancestor), but the browser hits " + hit);
    }

    private void assertInsideViewport(Locator element, String what)
    {
        @SuppressWarnings("unchecked")
        List<Number> box = (List<Number>) element.evaluate("(e) => { const r = e.getBoundingClientRect();"
                                                           + " return [r.left, r.top, r.right, r.bottom, r.width, r.height, window.innerWidth, window.innerHeight]; }");
        String description = what + " box [left, top, right, bottom, width, height, viewportWidth, viewportHeight] = " + box;
        assertTrue(box.get(4)
                      .doubleValue() > 20
                   && box.get(5)
                         .doubleValue() > 20,
                   "the " + description);
        assertTrue(box.get(0)
                      .doubleValue() >= -0.5
                   && box.get(1)
                         .doubleValue() >= -0.5
                   && box.get(2)
                         .doubleValue() <= box.get(6)
                                              .doubleValue()
                                           + 0.5
                   && box.get(3)
                         .doubleValue() <= box.get(7)
                                              .doubleValue()
                                           + 0.5,
                   "the " + description + " must lie inside the viewport");
    }

    private String servedTablerSheet()
    {
        return this.page.request()
                        .get(this.baseUrl() + TABLER_STYLESHEET_PATH)
                        .text();
    }

    private int[] primaryTripletOfTheServedSheet()
    {
        Matcher matcher = PRIMARY_RGB_LITERAL.matcher(this.servedTablerSheet());
        assertTrue(matcher.find(), "the served Tabler sheet holds no --bs-primary-rgb literal, a stale or foreign sheet?");
        return new int[] {Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)), Integer.parseInt(matcher.group(3))};
    }
}
