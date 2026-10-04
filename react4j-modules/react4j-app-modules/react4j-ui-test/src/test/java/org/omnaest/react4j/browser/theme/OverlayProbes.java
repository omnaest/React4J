package org.omnaest.react4j.browser.theme;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/**
 * plan-277 F1: the browser probes for an absolutely positioned overlay (dropdown menu, toast) that an ancestor's {@code overflow} can clip, shared by the
 * Tabler and the modern theme tests so that both presets are held to the same check. Lifted out of {@code TablerThemeIT}, which was the only user.
 * <p>
 * A visibility assertion must sample the overlay's edges and not only its centre: a centre probe stays green while the edges are cut away (workspace
 * defect class "overlay clipped by an ancestor's overflow", see {@code react4j-core-ui/CLAUDE.md}).
 */
final class OverlayProbes
{
    private OverlayProbes()
    {
        // static helper
    }

    /**
     * Centres the element in the viewport (the showcase scrolls inside a container), so that an overlay opening below it has room and the click does
     * not depend on Playwright's own scrolling
     */
    static void scrollToCenter(Page page, Locator element)
    {
        element.evaluate("e => e.scrollIntoView({block: 'center'})");
        page.waitForTimeout(300);
    }

    /**
     * An OPEN dropdown menu is visible, fully inside the viewport, reaches its first item, and is not clipped by any ancestor: the browser hits the
     * menu (or a descendant) at the centre of the last enabled item AND just inside every edge midpoint of the menu.
     * <p>
     * The last ENABLED item is probed, because a disabled item has {@code pointer-events:none} and passes the pointer through.
     */
    static void assertMenuVisibleInsideViewportAndUnclipped(Locator menu, String firstItemText, String lastEnabledItemText)
    {
        menu.waitFor(new Locator.WaitForOptions().setTimeout(10000));
        assertTrue(menu.isVisible(), "the dropdown menu must be visible");
        assertTrue(menu.getByText(firstItemText)
                       .first()
                       .isVisible(),
                   "the menu item '" + firstItemText + "' must be visible");
        assertInsideViewport(menu, "dropdown menu");
        assertEquals("1", menu.evaluate("e => getComputedStyle(e).opacity"));
        assertReachable(menu.locator(".dropdown-item", new Locator.LocatorOptions().setHasText(lastEnabledItemText))
                            .first(),
                        "last enabled item '" + lastEnabledItemText + "' of the dropdown menu");
        assertEveryEdgeMidpointReachable(menu, "dropdown menu");
    }

    /**
     * The element, or one of its descendants, is what the browser hits at the element's own centre: it is neither clipped away nor covered
     */
    static void assertReachable(Locator element, String what)
    {
        Object hit = element.evaluate("(e) => { const r = e.getBoundingClientRect(); const t = document.elementFromPoint(r.left + r.width / 2, r.top + r.height / 2);"
                                      + " return t === e || e.contains(t) ? 'reachable' : (t ? t.tagName + '.' + t.className : 'nothing'); }");
        assertEquals("reachable", hit, "the " + what + " must be reachable at its centre, but the browser hits " + hit);
    }

    /**
     * The element, or one of its descendants, is what the browser hits just inside each of its four edge midpoints and at its centre: no edge is
     * clipped away by an ancestor's overflow. Midpoints, not corners, so that rounded corners cannot fail it.
     */
    static void assertEveryEdgeMidpointReachable(Locator element, String what)
    {
        Object hit = element.evaluate("(e) => { const r = e.getBoundingClientRect(); const inset = 3; const points = {"
                                      + " top: [r.left + r.width / 2, r.top + inset], bottom: [r.left + r.width / 2, r.bottom - inset],"
                                      + " left: [r.left + inset, r.top + r.height / 2], right: [r.right - inset, r.top + r.height / 2] };"
                                      + " const misses = []; for (const [name, [x, y]] of Object.entries(points)) { const t = document.elementFromPoint(x, y);"
                                      + " if (!(t === e || e.contains(t))) { misses.push(name + ' -> ' + (t ? t.tagName + '.' + t.className : 'nothing')); } }"
                                      + " return misses.length === 0 ? 'reachable' : misses.join(', '); }");
        assertEquals("reachable", hit, "the " + what + " must be reachable at the middle of each of its edges (no edge clipped by an ancestor), but the browser hits " + hit);
    }

    static void assertInsideViewport(Locator element, String what)
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
}
