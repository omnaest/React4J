package org.omnaest.react4j.browser.theme;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.omnaest.react4j.ComponentShowcaseUI;
import org.omnaest.react4j.domain.Icon;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

/**
 * plan-277 F3 (kanban 866cf42b item 4): the one check logic for the three showcase cards that no browser check addressed before - a Bootstrap table, a
 * NAV-presented dropdown and the form validation feedback. The concrete subclasses only choose the theme preset (modern and Tabler), so every check
 * runs with identical logic on both.
 * <p>
 * Every colour expectation is the browser's own resolution of the theme's custom property ({@link BrowserColors#resolveColorVariable}) and never a value
 * read from the element under test, and every one is paired with a value that must differ from it, so a check cannot pass merely because both sides
 * share one cause.
 */
abstract class ShowcaseCoverageChecks extends ThemeBrowserSupport
{
    /**
     * Settling time for border colours, which Bootstrap and Tabler transition over 150ms
     */
    private static final int COLOUR_SETTLE_MILLIS = 600;

    // ---- table ----------------------------------------------------------------------------------------------------------------

    @Test
    public void testTableIsRenderedInItsCardAndTheThemeStylesItsBorders()
    {
        Locator card = this.openShowcaseAndLocateCard(ComponentShowcaseUI.TABLE_CARD_TITLE);
        Locator table = card.locator("table.table");
        assertEquals(1, table.count(), "exactly one table.table expected in the card '" + ComponentShowcaseUI.TABLE_CARD_TITLE + "'");
        assertEquals(List.of("Item", "Count", "State"), table.locator("thead th")
                                                             .allTextContents());
        assertEquals(3, table.locator("tbody tr")
                             .count(),
                     "the three rows of the showcase table");
        assertEquals("Beta", table.locator("tbody tr")
                                  .nth(1)
                                  .locator("td")
                                  .first()
                                  .textContent());

        Locator cell = table.locator("tbody td")
                            .first();
        String themedBorder = computedStyle(cell, "border-bottom-color");
        int[] themedBytes = BrowserColors.srgbBytes(this.page, themedBorder);
        int[] expectedBytes = BrowserColors.srgbBytes(this.page, BrowserColors.resolveColorVariable(table, "--bs-table-border-color"));
        int[] unstyledBytes = BrowserColors.srgbBytes(this.page, this.unstyledTableCellBorderColor());

        System.out.println(this.getClass()
                               .getSimpleName()
                           + " measured table cell border-bottom-color " + themedBorder + " = sRGB " + java.util.Arrays.toString(themedBytes) + ", resolved --bs-table-border-color "
                           + java.util.Arrays.toString(expectedBytes) + ", unstyled table cell " + java.util.Arrays.toString(unstyledBytes));
        assertEquals(0, BrowserColors.maxChannelDistance(themedBytes, expectedBytes),
                     "the cell border must be the theme's --bs-table-border-color " + java.util.Arrays.toString(expectedBytes) + " but is " + themedBorder);
        assertNotEquals("0px", computedStyle(cell, "border-bottom-width"), "a styled table cell has a bottom border");
        assertTrue(BrowserColors.maxChannelDistance(themedBytes, unstyledBytes) > 0,
                   "the themed border " + java.util.Arrays.toString(themedBytes) + " must differ from the unstyled table cell's " + java.util.Arrays.toString(unstyledBytes));
    }

    // ---- NAV dropdown ---------------------------------------------------------------------------------------------------------

    @Test
    public void testNavDropdownOpensAndItsMenuIsVisibleInsideTheViewportAndUnclipped()
    {
        Locator card = this.openShowcaseAndLocateCard(ComponentShowcaseUI.NAV_DROPDOWN_CARD_TITLE);
        Locator toggle = card.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName(ComponentShowcaseUI.NAV_DROPDOWN_TOGGLE_LABEL)
                                                                                       .setExact(true));
        assertEquals(1, toggle.count(), "exactly one toggle '" + ComponentShowcaseUI.NAV_DROPDOWN_TOGGLE_LABEL + "' expected in the card");
        OverlayProbes.scrollToCenter(this.page, toggle);
        toggle.click();

        Locator menu = card.locator(".dropdown-menu.show");
        OverlayProbes.assertMenuVisibleInsideViewportAndUnclipped(menu, "Reports", "Settings");
        OverlayProbes.assertReachable(menu, "NAV dropdown menu");
    }

    // ---- collapsed hamburger (plan-277 section 8 item 2) ---------------------------------------------------------------------

    @Test
    public void testHamburgerDoesNotCoverTheFirstBodyElementAtDesktopWidth()
    {
        this.assertHamburgerClearsTheFirstBodyElement(1280, 900);
    }

    @Test
    public void testHamburgerDoesNotCoverTheFirstBodyElementAtPhoneWidth()
    {
        this.assertHamburgerClearsTheFirstBodyElement(400, 900);
    }

    // ---- NavigationBar dropdown (plan-277 section 8 item 3) -------------------------------------------------------------------

    @Test
    public void testNavigationBarDropdownOpensUnclippedAndItsItemNavigatesToItsLocator()
    {
        this.openShowcaseAndLocateOpenModalButton();
        this.page.locator(".navbar-toggler")
                 .click();
        Locator navbar = this.page.locator("#navbarContent");
        navbar.waitFor(new Locator.WaitForOptions().setTimeout(10000));

        Locator toggle = navbar.locator(".dropdown-toggle");
        assertEquals(1, toggle.count(), "exactly one dropdown toggle expected inside #navbarContent");
        assertEquals(ComponentShowcaseUI.NAV_DROPDOWN_ENTRY_TOGGLE_TEXT, toggle.textContent()
                                                                               .trim());
        toggle.click();

        Locator menu = navbar.locator(".dropdown-menu.show");
        OverlayProbes.assertMenuVisibleInsideViewportAndUnclipped(menu, ComponentShowcaseUI.NAV_DROPDOWN_ENTRY_A_TEXT, ComponentShowcaseUI.NAV_DROPDOWN_ENTRY_B_TEXT);

        Locator itemB = menu.locator(".dropdown-item", new Locator.LocatorOptions().setHasText(ComponentShowcaseUI.NAV_DROPDOWN_ENTRY_B_TEXT));
        assertEquals("#" + ComponentShowcaseUI.NAV_TARGET_B_LOCATOR, itemB.getAttribute("href"));
        itemB.click();
        this.page.waitForFunction("location.hash === '#" + ComponentShowcaseUI.NAV_TARGET_B_LOCATOR + "'");
        assertEquals("#" + ComponentShowcaseUI.NAV_TARGET_B_LOCATOR, this.page.evaluate("location.hash"));
    }

    // ---- Icon (plan-277 section 8 item 4) -------------------------------------------------------------------------------------

    @Test
    public void testEveryStandardIconRendersARealGlyphFromTheFontAwesomeFace()
    {
        Locator card = this.openShowcaseAndLocateCard(ComponentShowcaseUI.ICON_CARD_TITLE);
        assertEquals(Icon.StandardIcon.values().length, card.locator("i.fas")
                                                            .count(),
                     "one <i class=fas> per StandardIcon expected in the card '" + ComponentShowcaseUI.ICON_CARD_TITLE + "'");

        // The face is requested lazily, when a glyph is first laid out: wait until the page itself has loaded it, then let the document settle
        try
        {
            this.page.waitForFunction("() => [...document.fonts].some(f => f.family.replace(/[\"']/g, '') === 'Font Awesome 5 Free' && f.status === 'loaded')",
                                      null, new Page.WaitForFunctionOptions().setTimeout(10000));
        }
        catch (RuntimeException e)
        {
            throw new AssertionError("no Font Awesome 5 Free face reached status 'loaded': " + this.page.evaluate(
                                                                                                                  "async () => { await document.fonts.ready; return JSON.stringify([...document.fonts].map(f => f.family + ' ' + f.weight + ' ' + f.status)); }"),
                                     e);
        }
        @SuppressWarnings("unchecked")
        List<String> faces = (List<String>) this.page.evaluate("async () => { await document.fonts.ready; return [...document.fonts].filter(f => f.family.replace(/[\"']/g, '') === 'Font Awesome 5 Free')"
                                                               + ".map(f => f.weight + ':' + f.status); }");
        assertTrue(faces.contains("900:loaded"), "the solid (weight 900) Font Awesome 5 Free face must be loaded after document.fonts.ready, faces: " + faces);

        Set<String> glyphs = new HashSet<>();
        for (Icon.StandardIcon icon : Icon.StandardIcon.values())
        {
            Locator glyph = card.locator("i.fa-" + icon.get());
            assertEquals(1, glyph.count(), "exactly one <i> expected for " + icon + " (fa-" + icon.get() + ")");
            @SuppressWarnings("unchecked")
            Map<String, Object> measured = (Map<String, Object>) glyph.evaluate("e => { const before = getComputedStyle(e, '::before'); const content = before.content; const family = before.fontFamily;"
                                                                                + " const context = document.createElement('canvas').getContext('2d'); context.font = before.fontWeight + ' ' + before.fontSize + ' ' + family;"
                                                                                + " const code = content.length === 3 ? content.charCodeAt(1) : 0;"
                                                                                + " return { content: content, code: code, family: family, glyphWidth: context.measureText(content.length === 3 ? content.charAt(1) : '').width,"
                                                                                + " fontLoaded: document.fonts.check(before.fontWeight + ' ' + before.fontSize + ' ' + family, content.length === 3 ? content.charAt(1) : '') }; }");
            System.out.println(this.getClass()
                                   .getSimpleName()
                               + " measured " + icon + " (fa-" + icon.get() + "): " + measured);
            int code = ((Number) measured.get("code")).intValue();
            assertTrue(code >= 0xE000 && code <= 0xF8FF, icon + ": ::before content must be one private-use Font Awesome glyph but is " + measured.get("content"));
            assertTrue(String.valueOf(measured.get("family"))
                             .replace("\"", "")
                             .contains("Font Awesome 5 Free"),
                       icon + ": ::before font-family must be Font Awesome 5 Free but is " + measured.get("family"));
            assertTrue(((Number) measured.get("glyphWidth")).doubleValue() > 0, icon + ": the glyph must have a non-zero rendered width: " + measured);
            assertEquals(Boolean.TRUE, measured.get("fontLoaded"), icon + ": the face of its glyph must be loaded: " + measured);
            glyphs.add(String.valueOf(measured.get("content")));
        }
        assertEquals(Icon.StandardIcon.values().length, glyphs.size(), "every StandardIcon must resolve to its own glyph: " + glyphs);
    }

    // ---- form validation, through the real click round trip ---------------------------------------------------------------------

    @Test
    public void testInvalidFieldIsDrawnInTheThemesInvalidColoursAfterTheClickRoundTrip()
    {
        Locator card = this.clickValidateAndAwaitFeedback();
        Locator invalidControl = card.getByLabel(ComponentShowcaseUI.VALIDATION_INVALID_FIELD_LABEL, new Locator.GetByLabelOptions().setExact(true));
        Locator validControl = card.getByLabel(ComponentShowcaseUI.VALIDATION_VALID_FIELD_LABEL, new Locator.GetByLabelOptions().setExact(true));
        assertTrue(hasClass(invalidControl, "is-invalid"), "the control labelled '" + ComponentShowcaseUI.VALIDATION_INVALID_FIELD_LABEL + "' must be is-invalid");

        int[] border = BrowserColors.srgbBytes(this.page, computedStyle(invalidControl, "border-top-color"));
        int[] expectedBorder = BrowserColors.srgbBytes(this.page, BrowserColors.resolveColorVariable(card, "--bs-form-invalid-border-color"));
        int[] validBorder = BrowserColors.srgbBytes(this.page, computedStyle(validControl, "border-top-color"));
        Locator feedback = card.locator(".invalid-feedback", new Locator.LocatorOptions().setHasText(ComponentShowcaseUI.VALIDATION_INVALID_MESSAGE));
        int[] feedbackColour = BrowserColors.srgbBytes(this.page, computedStyle(feedback, "color"));
        int[] expectedFeedbackColour = BrowserColors.srgbBytes(this.page, BrowserColors.resolveColorVariable(card, "--bs-form-invalid-color"));

        System.out.println(this.getClass()
                               .getSimpleName()
                           + " measured invalid border " + java.util.Arrays.toString(border) + " (expected " + java.util.Arrays.toString(expectedBorder) + "), valid border "
                           + java.util.Arrays.toString(validBorder) + ", invalid feedback colour " + java.util.Arrays.toString(feedbackColour) + " (expected "
                           + java.util.Arrays.toString(expectedFeedbackColour) + ")");
        assertEquals(0, BrowserColors.maxChannelDistance(border, expectedBorder), "the invalid control's border must be --bs-form-invalid-border-color");
        assertTrue(BrowserColors.maxChannelDistance(border, validBorder) > 0, "the invalid control's border must differ from the valid control's");
        assertTrue(feedback.isVisible(), "the invalid feedback text must be visible");
        assertEquals(0, BrowserColors.maxChannelDistance(feedbackColour, expectedFeedbackColour), "the invalid feedback text must be drawn in --bs-form-invalid-color");
    }

    @Test
    public void testValidFieldIsDrawnInTheThemesValidColoursAfterTheClickRoundTrip()
    {
        Locator card = this.clickValidateAndAwaitFeedback();
        Locator invalidControl = card.getByLabel(ComponentShowcaseUI.VALIDATION_INVALID_FIELD_LABEL, new Locator.GetByLabelOptions().setExact(true));
        Locator validControl = card.getByLabel(ComponentShowcaseUI.VALIDATION_VALID_FIELD_LABEL, new Locator.GetByLabelOptions().setExact(true));
        assertTrue(hasClass(validControl, "is-valid"), "the control labelled '" + ComponentShowcaseUI.VALIDATION_VALID_FIELD_LABEL + "' must be is-valid");

        int[] border = BrowserColors.srgbBytes(this.page, computedStyle(validControl, "border-top-color"));
        int[] expectedBorder = BrowserColors.srgbBytes(this.page, BrowserColors.resolveColorVariable(card, "--bs-form-valid-border-color"));
        int[] invalidBorder = BrowserColors.srgbBytes(this.page, computedStyle(invalidControl, "border-top-color"));
        Locator feedback = card.locator(".valid-feedback", new Locator.LocatorOptions().setHasText(ComponentShowcaseUI.VALIDATION_VALID_MESSAGE));
        int[] feedbackColour = BrowserColors.srgbBytes(this.page, computedStyle(feedback, "color"));
        int[] expectedFeedbackColour = BrowserColors.srgbBytes(this.page, BrowserColors.resolveColorVariable(card, "--bs-form-valid-color"));

        System.out.println(this.getClass()
                               .getSimpleName()
                           + " measured valid border " + java.util.Arrays.toString(border) + " (expected " + java.util.Arrays.toString(expectedBorder) + "), invalid border "
                           + java.util.Arrays.toString(invalidBorder) + ", valid feedback colour " + java.util.Arrays.toString(feedbackColour) + " (expected "
                           + java.util.Arrays.toString(expectedFeedbackColour) + ")");
        assertEquals(0, BrowserColors.maxChannelDistance(border, expectedBorder), "the valid control's border must be --bs-form-valid-border-color");
        assertTrue(BrowserColors.maxChannelDistance(border, invalidBorder) > 0, "the valid control's border must differ from the invalid control's");
        assertTrue(feedback.isVisible(), "the valid feedback text must be visible");
        assertEquals(0, BrowserColors.maxChannelDistance(feedbackColour, expectedFeedbackColour), "the valid feedback text must be drawn in --bs-form-valid-color");
    }

    // ---- helpers ------------------------------------------------------------------------------------------------------------------

    /**
     * Opens the showcase at the given viewport (collapsed navigation) and asserts, by box arithmetic on getBoundingClientRect, that the first element
     * of the body (the showcase heading) intersects neither the hamburger container nor the toggler itself
     */
    private void assertHamburgerClearsTheFirstBodyElement(int width, int height)
    {
        this.page.setViewportSize(width, height);
        this.page.navigate(this.baseUrl() + "/");
        Locator heading = this.page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("Component Showcase")
                                                                                           .setExact(true));
        heading.waitFor(new Locator.WaitForOptions().setTimeout(15000));
        Locator container = this.page.locator(".navbar-menu-icon-container");
        Locator toggler = this.page.locator(".navbar-toggler");
        assertEquals(1, container.count(), "exactly one .navbar-menu-icon-container expected in the collapsed state");
        assertEquals(1, toggler.count(), "exactly one .navbar-toggler expected in the collapsed state");

        double[] headingBox = boundingBox(heading);
        double[] containerBox = boundingBox(container);
        double[] togglerBox = boundingBox(toggler);
        System.out.println(this.getClass()
                               .getSimpleName()
                           + " measured at " + width + "x" + height + " [left, top, right, bottom]: heading " + java.util.Arrays.toString(headingBox) + ", hamburger container "
                           + java.util.Arrays.toString(containerBox) + ", toggler " + java.util.Arrays.toString(togglerBox));
        assertTrue(containerBox[2] - containerBox[0] > 20 && containerBox[3] - containerBox[1] > 20, "the hamburger container must have a real box: " + java.util.Arrays.toString(containerBox));
        assertFalse(intersects(headingBox, containerBox), "the first body element " + java.util.Arrays.toString(headingBox) + " must not intersect the hamburger container "
                                                          + java.util.Arrays.toString(containerBox));
        assertFalse(intersects(headingBox, togglerBox), "the first body element " + java.util.Arrays.toString(headingBox) + " must not intersect the toggler "
                                                        + java.util.Arrays.toString(togglerBox));
    }

    private static double[] boundingBox(Locator element)
    {
        @SuppressWarnings("unchecked")
        List<Number> box = (List<Number>) element.evaluate("e => { const r = e.getBoundingClientRect(); return [r.left, r.top, r.right, r.bottom]; }");
        return box.stream()
                  .mapToDouble(Number::doubleValue)
                  .toArray();
    }

    /**
     * Two boxes [left, top, right, bottom] share an area of more than half a pixel in each direction
     */
    private static boolean intersects(double[] a, double[] b)
    {
        return Math.min(a[2], b[2]) - Math.max(a[0], b[0]) > 0.5 && Math.min(a[3], b[3]) - Math.max(a[1], b[1]) > 0.5;
    }

    /**
     * Opens the showcase and returns the one card carrying the given title text (located by its title, never by position)
     */
    private Locator openShowcaseAndLocateCard(String title)
    {
        this.openShowcaseAndLocateOpenModalButton();
        Locator card = this.page.locator(".card", new Page.LocatorOptions().setHasText(title));
        card.first()
            .waitFor(new Locator.WaitForOptions().setTimeout(15000));
        assertEquals(1, card.count(), "exactly one card matching the title text '" + title + "' expected, a collision with another showcase card?");
        OverlayProbes.scrollToCenter(this.page, card);
        return card;
    }

    /**
     * Clicks the validation card's button and waits until the server round trip has rendered both kinds of feedback; returns the card
     */
    private Locator clickValidateAndAwaitFeedback()
    {
        Locator card = this.openShowcaseAndLocateCard(ComponentShowcaseUI.VALIDATION_CARD_TITLE);
        card.getByLabel(ComponentShowcaseUI.VALIDATION_INVALID_FIELD_LABEL, new Locator.GetByLabelOptions().setExact(true)).fill("jdoe");
        card.getByLabel(ComponentShowcaseUI.VALIDATION_VALID_FIELD_LABEL, new Locator.GetByLabelOptions().setExact(true)).fill("jdoe@example.org");
        card.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName(ComponentShowcaseUI.VALIDATION_BUTTON_NAME)
                                                                      .setExact(true))
            .click();
        card.locator(".invalid-feedback", new Locator.LocatorOptions().setHasText(ComponentShowcaseUI.VALIDATION_INVALID_MESSAGE))
            .waitFor(new Locator.WaitForOptions().setTimeout(15000));
        card.locator(".valid-feedback", new Locator.LocatorOptions().setHasText(ComponentShowcaseUI.VALIDATION_VALID_MESSAGE))
            .waitFor(new Locator.WaitForOptions().setTimeout(15000));
        this.page.waitForFunction("document.querySelector('.App')?.getAttribute('data-inflight-count') === '0'");
        this.page.waitForTimeout(COLOUR_SETTLE_MILLIS);
        return card;
    }

    /**
     * The border colour of a cell of a plain {@code <table>} that carries no {@code .table} class, in the same document and under the same theme: the
     * "not styled by the theme" reference the themed cell must differ from
     */
    private String unstyledTableCellBorderColor()
    {
        return (String) this.page.evaluate("() => { const table = document.createElement('table'); table.innerHTML = '<tbody><tr><td>x</td></tr></tbody>';"
                                           + " document.body.appendChild(table); const colour = getComputedStyle(table.querySelector('td')).borderBottomColor; table.remove();"
                                           + " return colour; }");
    }

    private static boolean hasClass(Locator element, String className)
    {
        return (Boolean) element.evaluate("(e, name) => e.classList.contains(name)", className);
    }
}
