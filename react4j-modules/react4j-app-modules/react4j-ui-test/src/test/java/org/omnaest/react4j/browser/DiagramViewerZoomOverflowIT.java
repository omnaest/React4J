package org.omnaest.react4j.browser;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.omnaest.react4j.ComponentShowcaseUI;
import org.omnaest.react4j.MockApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.BoundingBox;
import com.microsoft.playwright.options.ViewportSize;

/**
 * plan-265 Slice S0 - the measuring instrument and the RED baseline, against UNMODIFIED code. Changes NO
 * production file. jsdom implements no layout (verified at source: {@code react4j-core-ui} runs
 * CRA-default {@code jest-environment-jsdom} with no {@code jest} block anywhere), so
 * {@code scrollWidth}/{@code clientWidth}/{@code scrollHeight}/{@code clientHeight}/
 * {@code getBoundingClientRect()} all read {@code 0}/zeroes there - a jest test of these acceptance
 * criteria would be either unprovable ({@code 0 > 0}) or vacuously green ({@code 0 === 0} with no
 * implementation at all). This class is therefore the ONLY admissible instrument for AC-1/AC-2/AC-3/
 * AC-6/AC-8/AC-8b/AC-10/AC-11 - a real Chromium browser, driven via Playwright, exactly as
 * {@code IntervalRerenderingContainerLiveUpdateIT} (lifecycle, page-settle convention) and
 * {@code KanbanBoardServer}'s {@code ColumnDropTargetGeometryIT} (the {@code page.evaluate(...) -> JSON}
 * via Jackson measurement pattern) already establish for this workspace.
 *
 * <p>
 * <b>Fixtures.</b> {@link ComponentShowcaseUI#DIAGRAM_VIEWER_TALL_NARROW_CARD_TITLE} /
 * {@link ComponentShowcaseUI#DIAGRAM_VIEWER_WIDE_FLAT_CARD_TITLE} /
 * {@link ComponentShowcaseUI#DIAGRAM_VIEWER_NON_INTERACTIVE_CARD_TITLE} are the handle contract this test
 * addresses each fixture by - the card's own TITLE, a deliberate contract established on the showcase
 * page (not a position, child index, or incidental class), consumed here as a compile-time-checked
 * constant reference rather than a hand-duplicated string literal, since both classes are compiled in the
 * same Maven module (main and test share one compile scope).
 *
 * <p>
 * <b>The scroll host.</b> {@link #domDump_confirmsScrollHostElement()} dumps the rendered subtree once and
 * is the evidence every other test's assertions are written against: {@code .diagram-viewer-svg-host} was
 * the only element in {@code DiagramViewer.css} carrying an {@code overflow} value at the time, and the DOM
 * dump confirms it is a real {@code DIV} wrapping the injected {@code <svg>} directly. Since S1 it carries
 * {@code overflow: auto} and IS the scroll container.
 *
 * <p>
 * <b>S1 landed 2026-09-27</b>, so this class is no longer measuring a RED baseline - it is the acceptance
 * suite for the shipped mechanism, and all of it is green. Two kinds of change were made to it in S1, both
 * to FIXTURES and neither to an assertion: three tests now drive the host to {@code scrollLeft = 0,
 * scrollTop = 0} before measuring, because a zoom change is centre-preserving (plan-265 constraint 4) and
 * therefore lands mid-range rather than at the origin - something that could not exist at HEAD, where
 * there was no scroll position at all; and every synthesized keyboard/wheel gesture is followed by
 * {@link #waitForScrollToSettle(Locator)}, because Chromium animates such a scroll over several frames
 * while the dispatch call returns immediately. The S0 predictions recorded below are left as written.
 *
 * <p>
 * <b>plan-266 S1 extended this class on 2026-09-27</b> with the control band, the column flex chain and the
 * ancestor-chain guard (AC-1, AC-4, AC-5, AC-6, AC-7). Nothing above was weakened or removed: the plan-265
 * assertions are untouched, and {@code .diagram-viewer-controls} is still the locator
 * {@link #setZoomRatio(Locator, String)} waits on, because the cluster kept its class when it stopped being
 * an overlay and became a band. The plan-266 tests are grouped together lower down under their own section
 * header, which also records WHY the chain guard is scoped to the component's own boundary on this page and
 * which limb of plan-266's AC-1 is not satisfiable in this repo at all.
 *
 * <p>
 * <b>Predictions, not instructions (plan-265 S0 brief).</b> Against unmodified code: AC-1, AC-3, AC-10 and
 * AC-11 are predicted to FAIL (no scroll region exists at any zoom - zoom rewrites the {@code viewBox},
 * never the element's own box); AC-2 is predicted to PASS (at Fit the svg is exactly
 * {@code width:100%; height:100%}, so there is genuinely no overflow); AC-6/AC-8/AC-8b are uncertain. Each
 * test method reports its actually MEASURED numbers via {@code System.out.println} regardless of the
 * prediction - a result that disagrees with the prediction is a finding about the prediction or the
 * instrument, not something to quietly correct here.
 */
@Tag("browser")
@SpringBootTest(classes = MockApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
public class DiagramViewerZoomOverflowIT
{
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @LocalServerPort
    private int                       port;

    private Playwright                playwright;

    private Browser                   browser;

    @BeforeEach
    public void openBrowser()
    {
        this.playwright = Playwright.create();
        this.browser = this.playwright.chromium()
                                      .launch(new BrowserType.LaunchOptions().setHeadless(true));
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

    // ------------------------------------------------------------------------------------------------
    // First act: confirm which element is the scroll host, before any assertion is written against one.
    // ------------------------------------------------------------------------------------------------

    @Test
    void domDump_confirmsScrollHostElement() throws Exception
    {
        try (BrowserContext context = this.newContext())
        {
            Page page = this.openBoard(context);
            Locator card = this.card(page, ComponentShowcaseUI.DIAGRAM_VIEWER_TALL_NARROW_CARD_TITLE);
            this.waitForDiagramMounted(card);
            Locator viewer = card.locator(".diagram-viewer");

            String json = (String) viewer.evaluate("(viewer) => {"
                                                   + "  const host = viewer.querySelector('.diagram-viewer-svg-host');"
                                                   + "  const svg = host.querySelector('svg');"
                                                   + "  function dump(el) {"
                                                   + "    const cs = getComputedStyle(el);"
                                                   + "    const cls = (el.className && el.className.baseVal !== undefined) ? el.className.baseVal : el.className;"
                                                   + "    return { tag: el.tagName, className: cls, overflowX: cs.overflowX, overflowY: cs.overflowY,"
                                                   + "             clientWidth: el.clientWidth, clientHeight: el.clientHeight,"
                                                   + "             scrollWidth: el.scrollWidth, scrollHeight: el.scrollHeight };" + "  }"
                                                   + "  return JSON.stringify({ viewer: dump(viewer), host: dump(host), svg: dump(svg) });" + "}");
            JsonNode dump = OBJECT_MAPPER.readTree(json);

            System.out.println("[plan-265 S0 DOM DUMP] .diagram-viewer      = " + dump.get("viewer"));
            System.out.println("[plan-265 S0 DOM DUMP] .diagram-viewer-svg-host = " + dump.get("host"));
            System.out.println("[plan-265 S0 DOM DUMP] svg (interior)       = " + dump.get("svg"));

            assertEquals("DIV", dump.get("host")
                                    .get("tag")
                                    .asText(),
                         "the scroll-host candidate '.diagram-viewer-svg-host' must be a real DIV");
        }
    }

    // ------------------------------------------------------------------------------------------------
    // AC-1 - at 400%, the scroll host must overflow BOTH axes. Two separate assertions.
    // ------------------------------------------------------------------------------------------------

    @Test
    void at400Percent_scrollHostOverflowsBothAxes_tallNarrow() throws Exception
    {
        this.assertOverflowsBothAxesAt400Percent(ComponentShowcaseUI.DIAGRAM_VIEWER_TALL_NARROW_CARD_TITLE, "tall-narrow");
    }

    @Test
    void at400Percent_scrollHostOverflowsBothAxes_wideFlat() throws Exception
    {
        this.assertOverflowsBothAxesAt400Percent(ComponentShowcaseUI.DIAGRAM_VIEWER_WIDE_FLAT_CARD_TITLE, "wide-flat");
    }

    private void assertOverflowsBothAxesAt400Percent(String cardTitle, String fixtureLabel) throws Exception
    {
        try (BrowserContext context = this.newContext())
        {
            Page page = this.openBoard(context);
            Locator card = this.card(page, cardTitle);
            this.waitForDiagramMounted(card);
            this.setZoomRatio(card, "4");

            JsonNode g = this.measureGeometry(card);
            double scrollWidth = g.get("scrollWidth")
                                  .asDouble();
            double clientWidth = g.get("clientWidth")
                                  .asDouble();
            double scrollHeight = g.get("scrollHeight")
                                   .asDouble();
            double clientHeight = g.get("clientHeight")
                                   .asDouble();

            System.out.println("[plan-265 S0 AC-1] " + fixtureLabel + " @400% - scrollWidth=" + scrollWidth + "px clientWidth=" + clientWidth
                               + "px, scrollHeight=" + scrollHeight + "px clientHeight=" + clientHeight + "px");

            assertAll(fixtureLabel + " AC-1",
                      () -> assertTrue(scrollWidth > clientWidth,
                                       fixtureLabel + ": AC-1 horizontal - scrollWidth (" + scrollWidth + ") must exceed clientWidth (" + clientWidth
                                                                  + ") at 400%"),
                      () -> assertTrue(scrollHeight > clientHeight,
                                       fixtureLabel + ": AC-1 vertical - scrollHeight (" + scrollHeight + ") must exceed clientHeight (" + clientHeight
                                                                    + ") at 400%"));
        }
    }

    // ------------------------------------------------------------------------------------------------
    // AC-2 - at Fit (100%), no overflow on either axis. Interactive AND non-interactive limbs.
    // ------------------------------------------------------------------------------------------------

    @Test
    void atFit_noOverflow_tallNarrow() throws Exception
    {
        this.assertNoOverflowAtFit(ComponentShowcaseUI.DIAGRAM_VIEWER_TALL_NARROW_CARD_TITLE, "tall-narrow", true);
    }

    @Test
    void atFit_noOverflow_wideFlat() throws Exception
    {
        this.assertNoOverflowAtFit(ComponentShowcaseUI.DIAGRAM_VIEWER_WIDE_FLAT_CARD_TITLE, "wide-flat", true);
    }

    @Test
    void atFit_noOverflow_nonInteractive() throws Exception
    {
        this.assertNoOverflowAtFit(ComponentShowcaseUI.DIAGRAM_VIEWER_NON_INTERACTIVE_CARD_TITLE, "non-interactive", false);
    }

    private void assertNoOverflowAtFit(String cardTitle, String fixtureLabel, boolean interactive) throws Exception
    {
        try (BrowserContext context = this.newContext())
        {
            Page page = this.openBoard(context);
            Locator card = this.card(page, cardTitle);
            this.waitForDiagramMounted(card);
            if (interactive)
            {
                // Selected explicitly rather than relied upon as the initial state, so the assertion does
                // not depend on an assumed default.
                this.setZoomRatio(card, "1");
            }

            JsonNode g = this.measureGeometry(card);
            double scrollWidth = g.get("scrollWidth")
                                  .asDouble();
            double clientWidth = g.get("clientWidth")
                                  .asDouble();
            double scrollHeight = g.get("scrollHeight")
                                   .asDouble();
            double clientHeight = g.get("clientHeight")
                                   .asDouble();

            System.out.println("[plan-265 S0 AC-2] " + fixtureLabel + " @Fit - scrollWidth=" + scrollWidth + "px clientWidth=" + clientWidth
                               + "px, scrollHeight=" + scrollHeight + "px clientHeight=" + clientHeight + "px");

            assertAll(fixtureLabel + " AC-2",
                      () -> assertEquals(clientWidth, scrollWidth, 0.5, fixtureLabel + ": AC-2 - scrollWidth must equal clientWidth at Fit"),
                      () -> assertEquals(clientHeight, scrollHeight, 0.5, fixtureLabel + ": AC-2 - scrollHeight must equal clientHeight at Fit"));
        }
    }

    // ------------------------------------------------------------------------------------------------
    // AC-3 - setting scrollLeft/scrollTop to a nonzero value shifts an SVG-interior element's rect by
    // the same amount (within 1px). The vertical twin is a SEPARATE assertion (H3/C2-A cannot be caught
    // by only asserting one axis).
    // ------------------------------------------------------------------------------------------------

    @Test
    void at400Percent_scrollPositionShiftsSvgInteriorElement_tallNarrow() throws Exception
    {
        this.assertScrollShiftsSvgInteriorElement(ComponentShowcaseUI.DIAGRAM_VIEWER_TALL_NARROW_CARD_TITLE, "tall-narrow");
    }

    @Test
    void at400Percent_scrollPositionShiftsSvgInteriorElement_wideFlat() throws Exception
    {
        this.assertScrollShiftsSvgInteriorElement(ComponentShowcaseUI.DIAGRAM_VIEWER_WIDE_FLAT_CARD_TITLE, "wide-flat");
    }

    private void assertScrollShiftsSvgInteriorElement(String cardTitle, String fixtureLabel) throws Exception
    {
        try (BrowserContext context = this.newContext())
        {
            Page page = this.openBoard(context);
            Locator card = this.card(page, cardTitle);
            this.waitForDiagramMounted(card);
            this.setZoomRatio(card, "4");
            // plan-265 S1: drive to the origin FIRST. A zoom change is centre-preserving (plan-265 constraint
            // 4), so selecting 400% from Fit deliberately lands in the MIDDLE of the new scroll range rather
            // than at 0 - measured scrollLeft=2349.0 on both fixtures. This fixture's "shift by exactly the
            // amount requested" arithmetic needs a known starting offset, and 0 is the one AC-3 is written
            // against; at HEAD there was no scroll position at all, so S0 could not have seen this. The
            // assertion itself is untouched - only the starting state it measures a delta from.
            this.setScroll(card, 0.0, 0.0);

            JsonNode before = this.measureMarkerRect(card);
            double xBefore = before.get("x")
                                   .asDouble();
            double yBefore = before.get("y")
                                   .asDouble();

            double requestedScrollLeft = 20.0;
            double requestedScrollTop = 15.0;
            this.setScroll(card, requestedScrollLeft, requestedScrollTop);

            JsonNode actualScroll = this.measureScroll(card);
            double actualScrollLeft = actualScroll.get("left")
                                                  .asDouble();
            double actualScrollTop = actualScroll.get("top")
                                                 .asDouble();

            JsonNode after = this.measureMarkerRect(card);
            double xAfter = after.get("x")
                                 .asDouble();
            double yAfter = after.get("y")
                                 .asDouble();

            System.out.println("[plan-265 S0 AC-3] " + fixtureLabel + " - requested scrollLeft=" + requestedScrollLeft + " actual scrollLeft read back="
                               + actualScrollLeft + ", marker x before=" + xBefore + " after=" + xAfter + " (measured shift=" + (xBefore - xAfter) + ")");
            System.out.println("[plan-265 S0 AC-3] " + fixtureLabel + " - requested scrollTop=" + requestedScrollTop + " actual scrollTop read back="
                               + actualScrollTop + ", marker y before=" + yBefore + " after=" + yAfter + " (measured shift=" + (yBefore - yAfter) + ")");

            assertAll(fixtureLabel + " AC-3",
                      () -> assertEquals(requestedScrollLeft, xBefore - xAfter, 1.0,
                                         fixtureLabel + ": AC-3 horizontal - setting scrollLeft to " + requestedScrollLeft
                                                                                     + " must shift the marker's x by that amount (within 1px); actual scrollLeft read back="
                                                                                     + actualScrollLeft),
                      () -> assertEquals(requestedScrollTop, yBefore - yAfter, 1.0,
                                         fixtureLabel + ": AC-3 vertical twin - setting scrollTop to " + requestedScrollTop
                                                                                    + " must shift the marker's y by that amount (within 1px); actual scrollTop read back="
                                                                                    + actualScrollTop));
        }
    }

    // ------------------------------------------------------------------------------------------------
    // AC-6 - at 50%, no scroll region on either axis, and the content is centred. "Centred" is measured
    // as: the svg's own rect centre coincides with the host's content-box centre (2px tolerance).
    // ------------------------------------------------------------------------------------------------

    @Test
    void at50Percent_noScrollRegion_andContentCentred_tallNarrow() throws Exception
    {
        this.assertCentredAt50Percent(ComponentShowcaseUI.DIAGRAM_VIEWER_TALL_NARROW_CARD_TITLE, "tall-narrow");
    }

    @Test
    void at50Percent_noScrollRegion_andContentCentred_wideFlat() throws Exception
    {
        this.assertCentredAt50Percent(ComponentShowcaseUI.DIAGRAM_VIEWER_WIDE_FLAT_CARD_TITLE, "wide-flat");
    }

    private void assertCentredAt50Percent(String cardTitle, String fixtureLabel) throws Exception
    {
        try (BrowserContext context = this.newContext())
        {
            Page page = this.openBoard(context);
            Locator card = this.card(page, cardTitle);
            this.waitForDiagramMounted(card);
            this.setZoomRatio(card, "0.5");

            JsonNode g = this.measureGeometry(card);
            double scrollWidth = g.get("scrollWidth")
                                  .asDouble();
            double clientWidth = g.get("clientWidth")
                                  .asDouble();
            double scrollHeight = g.get("scrollHeight")
                                   .asDouble();
            double clientHeight = g.get("clientHeight")
                                   .asDouble();
            double hostCentreX = g.get("hostLeft")
                                  .asDouble()
                                 + g.get("hostWidth")
                                    .asDouble()
                                   / 2;
            double hostCentreY = g.get("hostTop")
                                  .asDouble()
                                 + g.get("hostHeight")
                                    .asDouble()
                                   / 2;
            double svgCentreX = g.get("svgLeft")
                                 .asDouble()
                                + g.get("svgWidth")
                                   .asDouble()
                                  / 2;
            double svgCentreY = g.get("svgTop")
                                 .asDouble()
                                + g.get("svgHeight")
                                   .asDouble()
                                  / 2;

            System.out.println("[plan-265 S0 AC-6] " + fixtureLabel + " @50% - scrollWidth=" + scrollWidth + "px clientWidth=" + clientWidth
                               + "px, scrollHeight=" + scrollHeight + "px clientHeight=" + clientHeight + "px");
            System.out.println("[plan-265 S0 AC-6] " + fixtureLabel + " @50% - host centre=(" + hostCentreX + "," + hostCentreY + "), svg centre=("
                               + svgCentreX + "," + svgCentreY + ")");

            assertAll(fixtureLabel + " AC-6",
                      () -> assertEquals(clientWidth, scrollWidth, 0.5, fixtureLabel + ": AC-6 - no horizontal scroll region at 50%"),
                      () -> assertEquals(clientHeight, scrollHeight, 0.5, fixtureLabel + ": AC-6 - no vertical scroll region at 50%"),
                      () -> assertEquals(hostCentreX, svgCentreX, 2.0,
                                         fixtureLabel + ": AC-6 - content must be horizontally centred at 50%; host centre x=" + hostCentreX
                                                                       + " svg centre x=" + svgCentreX),
                      () -> assertEquals(hostCentreY, svgCentreY, 2.0,
                                         fixtureLabel + ": AC-6 - content must be vertically centred at 50%; host centre y=" + hostCentreY
                                                                       + " svg centre y=" + svgCentreY));
        }
    }

    // ------------------------------------------------------------------------------------------------
    // AC-8 - at 400%, the control cluster is fully within the diagram viewer's own visible box.
    // ------------------------------------------------------------------------------------------------

    @Test
    void at400Percent_controlsFullyWithinVisibleBox_tallNarrow() throws Exception
    {
        this.assertControlsFullyWithinVisibleBox(ComponentShowcaseUI.DIAGRAM_VIEWER_TALL_NARROW_CARD_TITLE, "tall-narrow");
    }

    @Test
    void at400Percent_controlsFullyWithinVisibleBox_wideFlat() throws Exception
    {
        this.assertControlsFullyWithinVisibleBox(ComponentShowcaseUI.DIAGRAM_VIEWER_WIDE_FLAT_CARD_TITLE, "wide-flat");
    }

    private void assertControlsFullyWithinVisibleBox(String cardTitle, String fixtureLabel) throws Exception
    {
        try (BrowserContext context = this.newContext())
        {
            Page page = this.openBoard(context);
            Locator card = this.card(page, cardTitle);
            this.waitForDiagramMounted(card);
            this.setZoomRatio(card, "4");

            JsonNode m = this.measureControlsAndBoxes(card);
            double viewerLeft = m.get("viewerLeft")
                                 .asDouble();
            double viewerTop = m.get("viewerTop")
                                .asDouble();
            double viewerRight = viewerLeft + m.get("viewerWidth")
                                               .asDouble();
            double viewerBottom = viewerTop + m.get("viewerHeight")
                                               .asDouble();
            double controlsLeft = m.get("controlsLeft")
                                   .asDouble();
            double controlsTop = m.get("controlsTop")
                                  .asDouble();
            double controlsRight = m.get("controlsRight")
                                    .asDouble();
            double controlsBottom = m.get("controlsBottom")
                                     .asDouble();

            System.out.println("[plan-265 S0 AC-8] " + fixtureLabel + " @400% - viewer box=[" + viewerLeft + "," + viewerTop + "," + viewerRight + ","
                               + viewerBottom + "], controls box=[" + controlsLeft + "," + controlsTop + "," + controlsRight + "," + controlsBottom + "]");

            assertAll(fixtureLabel + " AC-8",
                      () -> assertTrue(controlsLeft >= viewerLeft - 1.0,
                                       fixtureLabel + ": AC-8 - control cluster left edge (" + controlsLeft + ") must be within the diagram viewer's own "
                                                                         + "box (left=" + viewerLeft + ")"),
                      () -> assertTrue(controlsTop >= viewerTop - 1.0,
                                       fixtureLabel + ": AC-8 - control cluster top edge (" + controlsTop + ") must be within the diagram viewer's own box "
                                                                       + "(top=" + viewerTop + ")"),
                      () -> assertTrue(controlsRight <= viewerRight + 1.0,
                                       fixtureLabel + ": AC-8 - control cluster right edge (" + controlsRight + ") must be within the diagram viewer's own "
                                                                           + "box (right=" + viewerRight + ")"),
                      () -> assertTrue(controlsBottom <= viewerBottom + 1.0,
                                       fixtureLabel + ": AC-8 - control cluster bottom edge (" + controlsBottom + ") must be within the diagram viewer's "
                                                                             + "own box (bottom=" + viewerBottom + ")"));
        }
    }

    // ------------------------------------------------------------------------------------------------
    // AC-8b - at 400%, the control cluster's bounding box does not intersect either scrollbar gutter:
    // nothing of the cluster may lie outside the host's own CONTENT box.
    // ------------------------------------------------------------------------------------------------

    @Test
    void at400Percent_controlsDoNotIntersectScrollbarGutter_tallNarrow() throws Exception
    {
        this.assertControlsDoNotIntersectScrollbarGutter(ComponentShowcaseUI.DIAGRAM_VIEWER_TALL_NARROW_CARD_TITLE, "tall-narrow");
    }

    @Test
    void at400Percent_controlsDoNotIntersectScrollbarGutter_wideFlat() throws Exception
    {
        this.assertControlsDoNotIntersectScrollbarGutter(ComponentShowcaseUI.DIAGRAM_VIEWER_WIDE_FLAT_CARD_TITLE, "wide-flat");
    }

    private void assertControlsDoNotIntersectScrollbarGutter(String cardTitle, String fixtureLabel) throws Exception
    {
        try (BrowserContext context = this.newContext())
        {
            Page page = this.openBoard(context);
            Locator card = this.card(page, cardTitle);
            this.waitForDiagramMounted(card);
            this.setZoomRatio(card, "4");

            JsonNode m = this.measureControlsAndBoxes(card);
            double hostLeft = m.get("hostLeft")
                               .asDouble();
            double hostTop = m.get("hostTop")
                              .asDouble();
            double hostClientWidth = m.get("hostClientWidth")
                                      .asDouble();
            double hostClientHeight = m.get("hostClientHeight")
                                       .asDouble();
            double controlsRight = m.get("controlsRight")
                                    .asDouble();
            double controlsBottom = m.get("controlsBottom")
                                     .asDouble();

            double hostContentRight = hostLeft + hostClientWidth;
            double hostContentBottom = hostTop + hostClientHeight;

            System.out.println("[plan-265 S0 AC-8b] " + fixtureLabel + " @400% - host content box right=" + hostContentRight + " bottom="
                               + hostContentBottom + ", controls right=" + controlsRight + " bottom=" + controlsBottom);

            assertAll(fixtureLabel + " AC-8b",
                      () -> assertTrue(controlsRight <= hostContentRight + 1.0,
                                       fixtureLabel + ": AC-8b - cluster.right (" + controlsRight + ") must not exceed host.left+host.clientWidth ("
                                                                                + hostContentRight + ")"),
                      () -> assertTrue(controlsBottom <= hostContentBottom + 1.0,
                                       fixtureLabel + ": AC-8b - cluster.bottom (" + controlsBottom + ") must not exceed host.top+host.clientHeight ("
                                                                                  + hostContentBottom + ")"));
        }
    }

    // ------------------------------------------------------------------------------------------------
    // AC-10 - at 400% with scrollLeft/scrollTop both 0: no content lies before the scroll origin - the
    // svg's left/top edges must coincide with the host's content-box left/top (within 1px).
    // ------------------------------------------------------------------------------------------------

    @Test
    void at400PercentAtOrigin_noContentBeforeScrollOrigin_tallNarrow() throws Exception
    {
        this.assertNoContentBeforeScrollOrigin(ComponentShowcaseUI.DIAGRAM_VIEWER_TALL_NARROW_CARD_TITLE, "tall-narrow");
    }

    @Test
    void at400PercentAtOrigin_noContentBeforeScrollOrigin_wideFlat() throws Exception
    {
        this.assertNoContentBeforeScrollOrigin(ComponentShowcaseUI.DIAGRAM_VIEWER_WIDE_FLAT_CARD_TITLE, "wide-flat");
    }

    private void assertNoContentBeforeScrollOrigin(String cardTitle, String fixtureLabel) throws Exception
    {
        try (BrowserContext context = this.newContext())
        {
            Page page = this.openBoard(context);
            Locator card = this.card(page, cardTitle);
            this.waitForDiagramMounted(card);
            this.setZoomRatio(card, "4");
            // plan-265 S1: AC-10 is worded "At 400%, WITH scrollLeft/scrollTop AT 0, ..." - so the fixture has
            // to ESTABLISH the origin rather than assume it. A zoom change is centre-preserving (constraint 4),
            // which lands at the middle of the new range (measured scrollLeft=2349.0), and at HEAD there was no
            // scroll position for S0 to have discovered this against. The two precondition assertions below
            // therefore now confirm the origin was REACHED - which is itself the property AC-10 is about, since
            // start-edge overflow that cannot be reached is exactly the C2-A failure this criterion exists to
            // catch. The geometric assertions are unchanged, and mutation M2 still reds them.
            this.setScroll(card, 0.0, 0.0);

            JsonNode scroll = this.measureScroll(card);
            JsonNode geometry = this.measureGeometry(card);
            double scrollLeft = scroll.get("left")
                                      .asDouble();
            double scrollTop = scroll.get("top")
                                     .asDouble();
            double hostLeft = geometry.get("hostLeft")
                                      .asDouble();
            double hostTop = geometry.get("hostTop")
                                     .asDouble();
            double svgLeft = geometry.get("svgLeft")
                                     .asDouble();
            double svgTop = geometry.get("svgTop")
                                    .asDouble();

            System.out.println("[plan-265 S0 AC-10] " + fixtureLabel + " @400% - scrollLeft=" + scrollLeft + " scrollTop=" + scrollTop + ", hostLeft="
                               + hostLeft + " svgLeft=" + svgLeft + ", hostTop=" + hostTop + " svgTop=" + svgTop);

            assertAll(fixtureLabel + " AC-10",
                      () -> assertEquals(0.0, scrollLeft, 0.001, fixtureLabel + ": AC-10 precondition - scrollLeft must be 0"),
                      () -> assertEquals(0.0, scrollTop, 0.001, fixtureLabel + ": AC-10 precondition - scrollTop must be 0"),
                      () -> assertEquals(hostLeft, svgLeft, 1.0,
                                         fixtureLabel + ": AC-10 - the svg's left edge must coincide with the host's content-box left edge at "
                                                                 + "scrollLeft=0; hostLeft=" + hostLeft + " svgLeft=" + svgLeft),
                      () -> assertEquals(hostTop, svgTop, 1.0,
                                         fixtureLabel + ": AC-10 - the svg's top edge must coincide with the host's content-box top edge at scrollTop=0; "
                                                               + "hostTop=" + hostTop + " svgTop=" + svgTop));
        }
    }

    // ------------------------------------------------------------------------------------------------
    // AC-11 - the scroll host is genuinely drivable: focusable, keyboard-drivable, wheel-drivable.
    // ------------------------------------------------------------------------------------------------

    @Test
    void at400Percent_scrollHostIsKeyboardAndWheelDrivable_tallNarrow() throws Exception
    {
        this.assertScrollHostIsDrivable(ComponentShowcaseUI.DIAGRAM_VIEWER_TALL_NARROW_CARD_TITLE, "tall-narrow");
    }

    @Test
    void at400Percent_scrollHostIsKeyboardAndWheelDrivable_wideFlat() throws Exception
    {
        this.assertScrollHostIsDrivable(ComponentShowcaseUI.DIAGRAM_VIEWER_WIDE_FLAT_CARD_TITLE, "wide-flat");
    }

    private void assertScrollHostIsDrivable(String cardTitle, String fixtureLabel) throws Exception
    {
        try (BrowserContext context = this.newContext())
        {
            Page page = this.openBoard(context);
            Locator card = this.card(page, cardTitle);
            this.waitForDiagramMounted(card);
            this.setZoomRatio(card, "4");
            // plan-265 S1: start every gesture from the origin, so "increases" is unambiguous and has the whole
            // range to move into (a zoom change is centre-preserving, so 400% lands mid-range - constraint 4).
            this.setScroll(card, 0.0, 0.0);
            Locator host = card.locator(".diagram-viewer-svg-host");

            Object tabIndexAttr = host.evaluate("(host) => host.getAttribute('tabindex')");
            System.out.println("[plan-265 S0 AC-11] " + fixtureLabel + " - tabindex attribute=" + tabIndexAttr);

            host.focus();
            boolean isActiveElement = (Boolean) host.evaluate("(host) => document.activeElement === host");
            System.out.println("[plan-265 S0 AC-11] " + fixtureLabel + " - document.activeElement === host after focus()? " + isActiveElement);

            JsonNode beforeArrow = this.measureScroll(card);
            page.keyboard()
                .press("ArrowRight");
            this.waitForScrollToSettle(card);
            JsonNode afterArrowRight = this.measureScroll(card);
            page.keyboard()
                .press("ArrowDown");
            this.waitForScrollToSettle(card);
            JsonNode afterArrowDown = this.measureScroll(card);
            System.out.println("[plan-265 S0 AC-11] " + fixtureLabel + " - scrollLeft before=" + beforeArrow.get("left")
                                                                                                            .asDouble()
                               + " after ArrowRight=" + afterArrowRight.get("left")
                                                                       .asDouble());
            System.out.println("[plan-265 S0 AC-11] " + fixtureLabel + " - scrollTop before=" + beforeArrow.get("top")
                                                                                                           .asDouble()
                               + " after ArrowDown=" + afterArrowDown.get("top")
                                                                     .asDouble());

            BoundingBox box = host.boundingBox();
            double cx = box.x + box.width / 2;
            double cy = box.y + box.height / 2;
            page.mouse()
                .move(cx, cy);
            JsonNode beforeWheel = this.measureScroll(card);
            page.mouse()
                .wheel(0, 100);
            this.waitForScrollToSettle(card);
            JsonNode afterWheel = this.measureScroll(card);
            System.out.println("[plan-265 S0 AC-11] " + fixtureLabel + " - scrollTop before wheel=" + beforeWheel.get("top")
                                                                                                                 .asDouble()
                               + " after wheel=" + afterWheel.get("top")
                                                             .asDouble());

            page.keyboard()
                .down("Shift");
            JsonNode beforeShiftWheel = this.measureScroll(card);
            page.mouse()
                .wheel(0, 100);
            this.waitForScrollToSettle(card);
            JsonNode afterShiftWheel = this.measureScroll(card);
            page.keyboard()
                .up("Shift");
            System.out.println("[plan-265 S0 AC-11] " + fixtureLabel + " - scrollLeft before shift+wheel=" + beforeShiftWheel.get("left")
                                                                                                                             .asDouble()
                               + " after shift+wheel=" + afterShiftWheel.get("left")
                                                                        .asDouble());

            assertAll(fixtureLabel + " AC-11",
                      () -> assertNotNull(tabIndexAttr,
                                          fixtureLabel + ": AC-11 - the scroll host must carry a tabIndex to be focusable; measured tabindex attribute="
                                                        + tabIndexAttr),
                      () -> assertTrue(isActiveElement, fixtureLabel + ": AC-11 - the scroll host must become document.activeElement after focus()"),
                      () -> assertTrue(afterArrowRight.get("left")
                                                      .asDouble() > beforeArrow.get("left")
                                                                               .asDouble(),
                                       fixtureLabel + ": AC-11 - an ArrowRight key press at 400% must increase scrollLeft; before=" + beforeArrow.get("left")
                                                                                                                                                 .asDouble()
                                                                                            + " after=" + afterArrowRight.get("left")
                                                                                                                         .asDouble()),
                      () -> assertTrue(afterArrowDown.get("top")
                                                     .asDouble() > beforeArrow.get("top")
                                                                              .asDouble(),
                                       fixtureLabel + ": AC-11 - an ArrowDown key press at 400% must increase scrollTop; before=" + beforeArrow.get("top")
                                                                                                                                               .asDouble()
                                                                                           + " after=" + afterArrowDown.get("top")
                                                                                                                       .asDouble()),
                      () -> assertTrue(afterWheel.get("top")
                                                 .asDouble() > beforeWheel.get("top")
                                                                          .asDouble(),
                                       fixtureLabel + ": AC-11 - a wheel gesture must increase scrollTop; before=" + beforeWheel.get("top")
                                                                                                                                .asDouble()
                                                                                       + " after=" + afterWheel.get("top")
                                                                                                               .asDouble()),
                      () -> assertTrue(afterShiftWheel.get("left")
                                                      .asDouble() > beforeShiftWheel.get("left")
                                                                                    .asDouble(),
                                       fixtureLabel + ": AC-11 - a shift+wheel gesture must increase scrollLeft; before=" + beforeShiftWheel.get("left")
                                                                                                                                            .asDouble()
                                                                                                 + " after=" + afterShiftWheel.get("left")
                                                                                                                              .asDouble()));
        }
    }

    // ------------------------------------------------------------------------------------------------
    // AC-14 (plan-265 S1) - one real pixel screenshot, for HUMAN visual review. DOM and accessibility
    // snapshots structurally cannot detect visual clipping: a control can be geometrically "inside the box"
    // per AC-8/AC-8b and still be rendered invisible or unusable, and those criteria are numeric and
    // therefore blind to it. Written under target/ as scratch, never committed. This test asserts only that
    // the artifact was produced and that the preconditions it is supposed to show actually hold (zoomed,
    // overflowing, scrolled away from the origin) - the pixels themselves are reviewed by a person, and an
    // assertion pretending to do that review would be the vacuous green this whole instrument exists to
    // avoid.
    // ------------------------------------------------------------------------------------------------

    @Test
    void at400PercentScrolledAwayFromOrigin_screenshotForVisualReview_wideFlat() throws Exception
    {
        try (BrowserContext context = this.newContext())
        {
            Page page = this.openBoard(context);
            Locator card = this.card(page, ComponentShowcaseUI.DIAGRAM_VIEWER_WIDE_FLAT_CARD_TITLE);
            this.waitForDiagramMounted(card);
            this.setZoomRatio(card, "4");

            JsonNode geometry = this.measureGeometry(card);
            double scrollWidth = geometry.get("scrollWidth")
                                         .asDouble();
            double clientWidth = geometry.get("clientWidth")
                                         .asDouble();
            double scrollHeight = geometry.get("scrollHeight")
                                          .asDouble();
            double clientHeight = geometry.get("clientHeight")
                                          .asDouble();

            // Deliberately away from the origin on BOTH axes, and far enough in to be unmistakable in the
            // image rather than a couple of pixels a reviewer could not honestly see.
            double targetLeft = (scrollWidth - clientWidth) / 2;
            double targetTop = (scrollHeight - clientHeight) / 2;
            this.setScroll(card, targetLeft, targetTop);
            JsonNode scroll = this.measureScroll(card);

            Path screenshot = Paths.get("target", "plan-265-ac14-wide-flat-400percent-scrolled.png");
            card.locator(".diagram-viewer")
                .screenshot(new Locator.ScreenshotOptions().setPath(screenshot));

            System.out.println("[plan-265 S1 AC-14] wide-flat @400% - scrollWidth=" + scrollWidth + " clientWidth=" + clientWidth + ", scrollHeight="
                               + scrollHeight + " clientHeight=" + clientHeight);
            System.out.println("[plan-265 S1 AC-14] wide-flat @400% - scrolled to scrollLeft=" + scroll.get("left")
                                                                                                       .asDouble()
                               + " scrollTop=" + scroll.get("top")
                                                       .asDouble()
                               + "; screenshot written to " + screenshot.toAbsolutePath());

            assertAll("wide-flat AC-14",
                      () -> assertTrue(Files.size(screenshot) > 0, "AC-14 - a non-empty screenshot must have been written to " + screenshot.toAbsolutePath()),
                      () -> assertTrue(scrollWidth > clientWidth, "AC-14 precondition - the image must show a horizontally overflowing scroll region"),
                      () -> assertTrue(scrollHeight > clientHeight, "AC-14 precondition - the image must show a vertically overflowing scroll region"),
                      () -> assertTrue(scroll.get("left")
                                             .asDouble() > 0,
                                       "AC-14 precondition - the image must show the diagram scrolled away from the horizontal origin"),
                      () -> assertTrue(scroll.get("top")
                                             .asDouble() > 0,
                                       "AC-14 precondition - the image must show the diagram scrolled away from the vertical origin"));
        }
    }

    // ================================================================================================
    // plan-266 S1 - the control band, the column flex chain, and the ancestor-chain guard.
    //
    // WHY AN ANCESTOR-CHAIN ENUMERATION AND NOT A GEOMETRY READING. plan-265's whole acceptance set
    // measured the HOST and never walked up from it, and that is the sole reason every one of its 19
    // tests was green over a defect (the consuming application's .modal-body overflowing by exactly
    // 38px) that was present at Fit on every single overlay open. The instrument that would have caught
    // it is not a sharper measurement of the host - it is looking somewhere the host's own numbers
    // cannot reach. So these tests ENUMERATE the chain and assert a SET, by identity and count.
    //
    // WHICH LIMB OF plan-266 AC-1 THIS IS, AND WHICH IS NOT SATISFIABLE HERE. plan-266's AC-1 names
    // `.modal-body` and the card dialog; there is NO modal on this showcase page, so those limbs belong
    // to KanbanBoardServer's own DiagramViewerScrollSeamIT (plan-266 S2) and are deliberately absent
    // here rather than approximated. What IS provable in this repo, and is exactly this slice's own
    // regression risk, is that ADDING A FLEX ITEM TO THIS CHAIN INTRODUCES NO NEW OVERFLOWING ANCESTOR.
    //
    // THE SET IS SCOPED TO THE COMPONENT'S OWN BOUNDARY, AND THAT SCOPE IS NOT A WEAKENING - it is what
    // makes the assertion falsifiable at all. This showcase page is a long list of cards, so the PAGE
    // legitimately scrolls: an unscoped "no ancestor anywhere overflows" assertion would be red at HEAD,
    // red after this change, and red after any conceivable future change - a permanently-red assertion
    // proves nothing about anything. The scope taken is "every ancestor from .diagram-viewer up to and
    // INCLUDING the containing .card", which is precisely the positional analogue of `.modal-body` in
    // the consuming application: the consumer's own box, between the component and the page's scroller.
    // The full chain up to documentElement is DUMPED regardless, so the page-level entries are visible
    // in the output and can be compared by eye rather than being hidden by the scope.
    // ================================================================================================

    @Test
    void plan266_atFit_noOverflowingAncestorWithinComponentBoundary_tallNarrow() throws Exception
    {
        this.assertNoOverflowingAncestor(ComponentShowcaseUI.DIAGRAM_VIEWER_TALL_NARROW_CARD_TITLE, "tall-narrow", "1", "Fit");
    }

    @Test
    void plan266_atFit_noOverflowingAncestorWithinComponentBoundary_wideFlat() throws Exception
    {
        this.assertNoOverflowingAncestor(ComponentShowcaseUI.DIAGRAM_VIEWER_WIDE_FLAT_CARD_TITLE, "wide-flat", "1", "Fit");
    }

    @Test
    void plan266_at400Percent_noOverflowingAncestorWithinComponentBoundary_tallNarrow() throws Exception
    {
        this.assertNoOverflowingAncestor(ComponentShowcaseUI.DIAGRAM_VIEWER_TALL_NARROW_CARD_TITLE, "tall-narrow", "4", "400%");
    }

    @Test
    void plan266_at400Percent_noOverflowingAncestorWithinComponentBoundary_wideFlat() throws Exception
    {
        this.assertNoOverflowingAncestor(ComponentShowcaseUI.DIAGRAM_VIEWER_WIDE_FLAT_CARD_TITLE, "wide-flat", "4", "400%");
    }

    private void assertNoOverflowingAncestor(String cardTitle, String fixtureLabel, String ratioValue, String ratioLabel) throws Exception
    {
        try (BrowserContext context = this.newContext())
        {
            Page page = this.openBoard(context);
            Locator card = this.card(page, cardTitle);
            this.waitForDiagramMounted(card);
            this.setZoomRatio(card, ratioValue);

            JsonNode chain = this.measureAncestorChain(card);

            System.out.println("[plan-266 S1 AC-1] " + fixtureLabel + " @" + ratioLabel + " - FULL ancestor chain from .diagram-viewer-svg-host to "
                               + "documentElement (" + chain.size() + " elements):");
            List<String> overflowingWithinBoundary = new ArrayList<>();
            for (JsonNode element : chain)
            {
                System.out.println("[plan-266 S1 AC-1]   " + this.describeChainElement(element));
                if (element.get("withinComponentBoundary")
                           .asBoolean()
                    && (element.get("overflowsX")
                               .asBoolean()
                        || element.get("overflowsY")
                                  .asBoolean()))
                {
                    overflowingWithinBoundary.add(this.describeChainElement(element));
                }
            }

            JsonNode viewer = this.chainElementByClass(chain, "diagram-viewer");
            System.out.println("[plan-266 S1 AC-1] " + fixtureLabel + " @" + ratioLabel + " - overflowing ancestors WITHIN the component boundary = "
                               + overflowingWithinBoundary);

            JsonNode band = this.measureBandAndHost(card);
            JsonNode host = this.measureGeometry(card);
            System.out.println("[plan-266 S1 BAND SUBTRACTION] " + fixtureLabel + " @" + ratioLabel + " @viewport 1600x1000 - band rendered height="
                               + band.get("bandHeight")
                                     .asDouble()
                               + "px; .diagram-viewer content box=" + band.get("viewerClientWidth")
                                                                          .asDouble()
                               + "x" + band.get("viewerClientHeight")
                                           .asDouble()
                               + "; host CONTENT box (the box percentages resolve against)=" + host.get("clientWidth")
                                                                                                   .asDouble()
                               + "x" + host.get("clientHeight")
                                           .asDouble());

            assertAll(fixtureLabel + " @" + ratioLabel + " plan-266 AC-1",
                      () -> assertEquals(List.of(), overflowingWithinBoundary,
                                         fixtureLabel + " @" + ratioLabel
                                                                               + ": plan-266 AC-1 - the set of ancestors between .diagram-viewer-svg-host and its containing .card whose "
                                                                               + "scrollWidth exceeds clientWidth or scrollHeight exceeds clientHeight must be EMPTY"),
                      // Called out separately from the set above because .diagram-viewer is THE element this slice
                      // added a flex item to, so it is the one a broken column chain overflows first. A set
                      // assertion reports "something overflowed"; this names the element the mutation reds.
                      () -> assertNotNull(viewer, fixtureLabel + ": the chain must contain .diagram-viewer"),
                      () -> assertEquals(viewer.get("clientWidth")
                                               .asDouble(),
                                         viewer.get("scrollWidth")
                                               .asDouble(),
                                         0.5, fixtureLabel + " @" + ratioLabel + ": plan-266 AC-1 - .diagram-viewer itself must not overflow horizontally"),
                      () -> assertEquals(viewer.get("clientHeight")
                                               .asDouble(),
                                         viewer.get("scrollHeight")
                                               .asDouble(),
                                         0.5, fixtureLabel + " @" + ratioLabel + ": plan-266 AC-1 - .diagram-viewer itself must not overflow vertically"));
        }
    }

    // ------------------------------------------------------------------------------------------------
    // plan-266 AC-4 - the svg's percentage still resolves against the host's NEW, band-reduced content
    // box. This is hypothesis H-COL (plan-266 section 2.8) measured rather than assumed: DiagramViewer.css's
    // three recorded layout failures were all taken on a ROW flex chain, and this slice turned it into a
    // COLUMN one with an extra item. The ratio, not the absolute size, is the invariant - it holds only
    // if "calc(100% * scale)" resolves against the box the host actually ended up with.
    // ------------------------------------------------------------------------------------------------

    @Test
    void plan266_at400Percent_svgPercentageResolvesAgainstBandReducedHostBox_tallNarrow() throws Exception
    {
        this.assertScaleRatioIsFour(ComponentShowcaseUI.DIAGRAM_VIEWER_TALL_NARROW_CARD_TITLE, "tall-narrow");
    }

    @Test
    void plan266_at400Percent_svgPercentageResolvesAgainstBandReducedHostBox_wideFlat() throws Exception
    {
        this.assertScaleRatioIsFour(ComponentShowcaseUI.DIAGRAM_VIEWER_WIDE_FLAT_CARD_TITLE, "wide-flat");
    }

    private void assertScaleRatioIsFour(String cardTitle, String fixtureLabel) throws Exception
    {
        try (BrowserContext context = this.newContext())
        {
            Page page = this.openBoard(context);
            Locator card = this.card(page, cardTitle);
            this.waitForDiagramMounted(card);
            this.setZoomRatio(card, "4");

            JsonNode g = this.measureGeometry(card);
            double scrollWidth = g.get("scrollWidth")
                                  .asDouble();
            double clientWidth = g.get("clientWidth")
                                  .asDouble();
            double scrollHeight = g.get("scrollHeight")
                                   .asDouble();
            double clientHeight = g.get("clientHeight")
                                   .asDouble();
            double horizontalRatio = scrollWidth / clientWidth;
            double verticalRatio = scrollHeight / clientHeight;
            JsonNode band = this.measureBandAndHost(card);

            System.out.println("[plan-266 S1 AC-4] " + fixtureLabel + " @400% - host content box " + clientWidth + "x" + clientHeight + ", scroll box "
                               + scrollWidth + "x" + scrollHeight + " => ratios " + horizontalRatio + " / " + verticalRatio + "; band height="
                               + band.get("bandHeight")
                                     .asDouble()
                               + "px, .diagram-viewer content box " + band.get("viewerClientWidth")
                                                                          .asDouble()
                               + "x" + band.get("viewerClientHeight")
                                           .asDouble());

            // Stated as its own line, in its own terms, rather than left to be read out of the chain dump: a
            // following card computes a cover-fit scale factor from exactly these three numbers, and a
            // misreading here propagates straight into that scale. CONTENT box (clientWidth/clientHeight),
            // not the border box - the content box is what the svg's percentages actually resolve against.
            System.out.println("[plan-266 S1 BAND SUBTRACTION] " + fixtureLabel + " @viewport 1600x1000 - band rendered height="
                               + band.get("bandHeight")
                                     .asDouble()
                               + "px; .diagram-viewer content box=" + band.get("viewerClientWidth")
                                                                          .asDouble()
                               + "x" + band.get("viewerClientHeight")
                                           .asDouble()
                               + "; host CONTENT box (the box percentages resolve against)=" + clientWidth + "x" + clientHeight);

            assertAll(fixtureLabel + " plan-266 AC-4",
                      () -> assertEquals(4.0, horizontalRatio, 0.05,
                                         fixtureLabel + ": plan-266 AC-4 - scrollWidth/clientWidth must be 4.0 at 400%; measured " + horizontalRatio),
                      () -> assertEquals(4.0, verticalRatio, 0.05,
                                         fixtureLabel + ": plan-266 AC-4 - scrollHeight/clientHeight must be 4.0 at 400%; measured " + verticalRatio),
                      // Without this the ratio assertions above would also pass in the degenerate world where the
                      // band reserved no height at all, i.e. where the band was never rendered - the criterion is
                      // "resolves against the BAND-REDUCED box", so the reduction has to be shown to exist.
                      () -> assertTrue(band.get("bandHeight")
                                           .asDouble() > 0,
                                       fixtureLabel + ": plan-266 AC-4 - the band must actually occupy height, otherwise "
                                                            + "\"resolves against the band-reduced content box\" is vacuous; measured " + band.get("bandHeight")
                                                                                                                                              .asDouble()));
        }
    }

    // ------------------------------------------------------------------------------------------------
    // plan-266 AC-5 - the host's flex declarations, read from getComputedStyle rather than from the
    // stylesheet source. "min-height: 0" is load-bearing on the NEW column axis for a second, independent
    // reason from the one DiagramViewer.css originally recorded it for: flexbox's default
    // "min-height: auto" floor would stop the host shrinking to make room for the band, and the overflow
    // would then escape onto .diagram-viewer. plan-266's pre-mortem names forgetting it as a predicted
    // failure; AC-11's mutation proves the pairing bites.
    // ------------------------------------------------------------------------------------------------

    @Test
    void plan266_hostKeepsItsFlexShrinkFloorsOnTheColumnAxis_tallNarrow() throws Exception
    {
        this.assertHostFlexDeclarations(ComponentShowcaseUI.DIAGRAM_VIEWER_TALL_NARROW_CARD_TITLE, "tall-narrow");
    }

    @Test
    void plan266_hostKeepsItsFlexShrinkFloorsOnTheColumnAxis_wideFlat() throws Exception
    {
        this.assertHostFlexDeclarations(ComponentShowcaseUI.DIAGRAM_VIEWER_WIDE_FLAT_CARD_TITLE, "wide-flat");
    }

    private void assertHostFlexDeclarations(String cardTitle, String fixtureLabel) throws Exception
    {
        try (BrowserContext context = this.newContext())
        {
            Page page = this.openBoard(context);
            Locator card = this.card(page, cardTitle);
            this.waitForDiagramMounted(card);

            String json = (String) card.locator(".diagram-viewer-svg-host")
                                       .evaluate("(host) => {" + "  const cs = getComputedStyle(host);"
                                                 + "  const viewport = host.parentElement;"
                                                 + "  const vs = getComputedStyle(viewport);"
                                                 + "  const viewer = getComputedStyle(viewport.parentElement);" + "  return JSON.stringify({"
                                                 + "    minHeight: cs.minHeight, minWidth: cs.minWidth,"
                                                 + "    flexGrow: cs.flexGrow, flexShrink: cs.flexShrink, flexBasis: cs.flexBasis,"
                                                 + "    viewportClassName: String(viewport.className || ''),"
                                                 + "    viewportDisplay: vs.display, viewportFlexDirection: vs.flexDirection,"
                                                 + "    viewportMinHeight: vs.minHeight, viewportFlexGrow: vs.flexGrow, viewportFlexShrink: vs.flexShrink,"
                                                 + "    viewerClassName: String(viewport.parentElement.className || ''),"
                                                 + "    parentDisplay: viewer.display, parentFlexDirection: viewer.flexDirection" + "  });" + "}");
            JsonNode f = OBJECT_MAPPER.readTree(json);

            System.out.println("[plan-266 S1 AC-5] " + fixtureLabel + " - host min-height=" + f.get("minHeight")
                                                                                               .asText()
                               + " min-width=" + f.get("minWidth")
                                                  .asText()
                               + " flex=" + f.get("flexGrow")
                                             .asText()
                               + " " + f.get("flexShrink")
                                        .asText()
                               + " " + f.get("flexBasis")
                                        .asText()
                               + "; viewport=." + f.get("viewportClassName")
                                                   .asText()
                               + " display=" + f.get("viewportDisplay")
                                                .asText()
                               + " flex-direction=" + f.get("viewportFlexDirection")
                                                       .asText()
                               + " min-height=" + f.get("viewportMinHeight")
                                                   .asText()
                               + " flex=" + f.get("viewportFlexGrow")
                                             .asText()
                               + " " + f.get("viewportFlexShrink")
                                        .asText()
                               + "; viewer=." + f.get("viewerClassName")
                                                 .asText()
                               + " display=" + f.get("parentDisplay")
                                                .asText()
                               + " flex-direction=" + f.get("parentFlexDirection")
                                                       .asText());

            assertAll(fixtureLabel + " plan-266 AC-5",
                      () -> assertEquals("0px", f.get("minHeight")
                                                 .asText(),
                                         fixtureLabel + ": plan-266 AC-5 - the host must compute min-height: 0px"),
                      () -> assertEquals("0px", f.get("minWidth")
                                                 .asText(),
                                         fixtureLabel + ": plan-266 AC-5 - the host must compute min-width: 0px"),
                      () -> assertEquals("1", f.get("flexGrow")
                                               .asText(),
                                         fixtureLabel + ": plan-266 AC-5 - the host must compute flex-grow: 1"),
                      () -> assertEquals("1", f.get("flexShrink")
                                               .asText(),
                                         fixtureLabel + ": plan-266 AC-5 - the host must compute flex-shrink: 1"),
                      () -> assertEquals("auto", f.get("flexBasis")
                                                  .asText(),
                                         fixtureLabel + ": plan-266 AC-5 - the host must compute flex-basis: auto"),
                      // The flex declarations above are inert unless the chain around them is right, and the
                      // shape of that chain is the one thing a CSS-only reader of the host's own rules cannot
                      // see. Both levels are asserted because they do DIFFERENT jobs and swapping them is the
                      // arrangement that measurably failed: .diagram-viewer is the COLUMN that stacks the band
                      // over the diagram, and .diagram-viewer-viewport is the ROW whose cross-axis stretch is
                      // what makes the host's height definite for the svg's percentage (see DiagramViewer.css,
                      // "WHY .diagram-viewer-viewport EXISTS AT ALL").
                      () -> assertEquals("flex", f.get("parentDisplay")
                                                  .asText(),
                                         fixtureLabel + ": plan-266 AC-5 - .diagram-viewer must be a flex container"),
                      () -> assertEquals("column", f.get("parentFlexDirection")
                                                    .asText(),
                                         fixtureLabel + ": plan-266 AC-5 - .diagram-viewer must be flex-direction: column"),
                      () -> assertEquals("diagram-viewer-viewport", f.get("viewportClassName")
                                                                     .asText(),
                                         fixtureLabel + ": plan-266 AC-5 - the host's direct parent must be .diagram-viewer-viewport"),
                      () -> assertEquals("diagram-viewer", f.get("viewerClassName")
                                                            .asText(),
                                         fixtureLabel + ": plan-266 AC-5 - the viewport's direct parent must be .diagram-viewer"),
                      () -> assertEquals("flex", f.get("viewportDisplay")
                                                  .asText(),
                                         fixtureLabel + ": plan-266 AC-5 - .diagram-viewer-viewport must be a flex container"),
                      () -> assertEquals("row", f.get("viewportFlexDirection")
                                                 .asText(),
                                         fixtureLabel + ": plan-266 AC-5 - .diagram-viewer-viewport must be flex-direction: row, which is what puts the "
                                                            + "host's HEIGHT on the cross axis and therefore makes it definite"),
                      () -> assertEquals("0px", f.get("viewportMinHeight")
                                                 .asText(),
                                         fixtureLabel + ": plan-266 AC-5 - .diagram-viewer-viewport must compute min-height: 0px so it can shrink to make "
                                                            + "room for the band"),
                      () -> assertEquals("1", f.get("viewportFlexGrow")
                                               .asText(),
                                         fixtureLabel + ": plan-266 AC-5 - .diagram-viewer-viewport must compute flex-grow: 1"),
                      () -> assertEquals("1", f.get("viewportFlexShrink")
                                               .asText(),
                                         fixtureLabel + ": plan-266 AC-5 - .diagram-viewer-viewport must compute flex-shrink: 1"));
        }
    }

    // ------------------------------------------------------------------------------------------------
    // plan-266 AC-6 - the band is OUTSIDE the scroll region, and there is exactly ONE scroll bar in the
    // component. Three independent limbs, because each catches a different way of getting this wrong:
    // rect non-intersection catches a band still painted over the diagram; rect-invariance-under-scroll
    // catches a band nested inside the scrolling element (where it would look right until the user
    // scrolls); and the bar COUNT catches an extra scroll region introduced anywhere in between.
    //
    // The count is scoped to the component boundary for the reason given at the top of this section - the
    // showcase page itself legitimately scrolls, and counting that would make the criterion permanently
    // unsatisfiable rather than strict.
    // ------------------------------------------------------------------------------------------------

    @Test
    void plan266_at400Percent_bandIsOutsideTheScrollRegionAndExactlyOneBarExists_tallNarrow() throws Exception
    {
        this.assertBandOutsideScrollRegion(ComponentShowcaseUI.DIAGRAM_VIEWER_TALL_NARROW_CARD_TITLE, "tall-narrow");
    }

    @Test
    void plan266_at400Percent_bandIsOutsideTheScrollRegionAndExactlyOneBarExists_wideFlat() throws Exception
    {
        this.assertBandOutsideScrollRegion(ComponentShowcaseUI.DIAGRAM_VIEWER_WIDE_FLAT_CARD_TITLE, "wide-flat");
    }

    private void assertBandOutsideScrollRegion(String cardTitle, String fixtureLabel) throws Exception
    {
        try (BrowserContext context = this.newContext())
        {
            Page page = this.openBoard(context);
            Locator card = this.card(page, cardTitle);
            this.waitForDiagramMounted(card);
            this.setZoomRatio(card, "4");
            this.setScroll(card, 0.0, 0.0);

            JsonNode before = this.measureBandAndHost(card);
            double bandTop = before.get("bandTop")
                                   .asDouble();
            double bandBottom = before.get("bandBottom")
                                      .asDouble();
            double bandLeft = before.get("bandLeft")
                                    .asDouble();
            double bandRight = before.get("bandRight")
                                     .asDouble();
            double hostTop = before.get("hostTop")
                                   .asDouble();
            double hostBottom = before.get("hostBottom")
                                      .asDouble();
            double hostLeft = before.get("hostLeft")
                                    .asDouble();
            double hostRight = before.get("hostRight")
                                     .asDouble();
            boolean intersects = bandRight > hostLeft + 0.5 && bandLeft < hostRight - 0.5 && bandBottom > hostTop + 0.5 && bandTop < hostBottom - 0.5;

            // Drive the host to its maximum on BOTH axes - the state in which a band placed inside the scroll
            // region would have travelled furthest, i.e. the state that most cheaply falsifies the claim.
            JsonNode g = this.measureGeometry(card);
            this.setScroll(card, g.get("scrollWidth")
                                  .asDouble(),
                           g.get("scrollHeight")
                            .asDouble());
            JsonNode scrolled = this.measureScroll(card);
            JsonNode after = this.measureBandAndHost(card);

            JsonNode chain = this.measureAncestorChain(card);
            List<String> scrollRegions = new ArrayList<>();
            JsonNode hostEntry = this.measureHostAsChainElement(card);
            if (this.isActiveScrollRegion(hostEntry))
            {
                scrollRegions.add(this.describeChainElement(hostEntry));
            }
            for (JsonNode element : chain)
            {
                if (element.get("withinComponentBoundary")
                           .asBoolean()
                    && this.isActiveScrollRegion(element))
                {
                    scrollRegions.add(this.describeChainElement(element));
                }
            }

            System.out.println("[plan-266 S1 AC-6] " + fixtureLabel + " @400% - band rect=[" + bandLeft + "," + bandTop + "," + bandRight + "," + bandBottom
                               + "], host rect=[" + hostLeft + "," + hostTop + "," + hostRight + "," + hostBottom + "], intersects=" + intersects);
            System.out.println("[plan-266 S1 AC-6] " + fixtureLabel + " @400% - band rect after scrolling host to scrollLeft=" + scrolled.get("left")
                                                                                                                                         .asDouble()
                               + " scrollTop=" + scrolled.get("top")
                                                         .asDouble()
                               + " => [" + after.get("bandLeft")
                                                .asDouble()
                               + "," + after.get("bandTop")
                                            .asDouble()
                               + "," + after.get("bandRight")
                                            .asDouble()
                               + "," + after.get("bandBottom")
                                            .asDouble()
                               + "]");
            System.out.println("[plan-266 S1 AC-6] " + fixtureLabel + " @400% - active scroll regions within the component boundary = " + scrollRegions);

            assertAll(fixtureLabel + " plan-266 AC-6",
                      () -> assertTrue(!intersects,
                                       fixtureLabel + ": plan-266 AC-6 - the band's rect must not intersect the host's; band=[" + bandLeft + "," + bandTop
                                                    + "," + bandRight + "," + bandBottom + "] host=[" + hostLeft + "," + hostTop + "," + hostRight + ","
                                                    + hostBottom + "]"),
                      () -> assertTrue(bandBottom <= hostTop + 0.5,
                                       fixtureLabel + ": plan-266 AC-6 - the band must sit ABOVE the host, not merely beside it; band bottom=" + bandBottom
                                                                    + " host top=" + hostTop),
                      // The precondition for the invariance limb below: if the host did not actually move, an
                      // unchanged band rect proves nothing at all.
                      () -> assertTrue(scrolled.get("left")
                                               .asDouble() > 0
                                       && scrolled.get("top")
                                                  .asDouble() > 0,
                                       fixtureLabel + ": plan-266 AC-6 precondition - the host must genuinely have scrolled on both axes; scrollLeft="
                                                                   + scrolled.get("left")
                                                                             .asDouble()
                                                                   + " scrollTop=" + scrolled.get("top")
                                                                                             .asDouble()),
                      () -> assertEquals(bandTop, after.get("bandTop")
                                                       .asDouble(),
                                         0.5, fixtureLabel + ": plan-266 AC-6 - the band must not move vertically when the host is scrolled"),
                      () -> assertEquals(bandLeft, after.get("bandLeft")
                                                        .asDouble(),
                                         0.5, fixtureLabel + ": plan-266 AC-6 - the band must not move horizontally when the host is scrolled"),
                      () -> assertEquals(1, scrollRegions.size(),
                                         fixtureLabel + ": plan-266 AC-6 - exactly ONE element in the component must be an active scroll region (the host); "
                                                                  + "found " + scrollRegions),
                      () -> assertTrue(scrollRegions.size() == 1 && scrollRegions.get(0)
                                                                                 .contains("diagram-viewer-svg-host"),
                                       fixtureLabel + ": plan-266 AC-6 - the single active scroll region must be .diagram-viewer-svg-host itself; found "
                                                                                                                       + scrollRegions));
        }
    }

    // ------------------------------------------------------------------------------------------------
    // plan-266 AC-7 - the non-interactive thumbnail. It renders no cluster today and must render no band
    // element and reserve no height: on a 220px on-card thumbnail a 38px band is a 17% loss. Asserting
    // ".diagram-viewer's height is unchanged" alone would NOT catch that - the viewer is max-height-bound
    // and would stay 600px while the host inside it silently shrank. The biting assertion is that the
    // HOST still gets the viewer's whole content box.
    //
    // The 600.0px baseline is not a figure taken from a document: it was measured in this same harness,
    // at this same viewport, on UNMODIFIED HEAD (commit 7be4771) before this slice's first edit.
    // ------------------------------------------------------------------------------------------------

    private static final double THUMBNAIL_HOST_HEIGHT_AT_HEAD = 600.0;

    @Test
    void plan266_nonInteractiveThumbnail_hasNoBandAndReservesNoBandHeight() throws Exception
    {
        try (BrowserContext context = this.newContext())
        {
            Page page = this.openBoard(context);
            Locator card = this.card(page, ComponentShowcaseUI.DIAGRAM_VIEWER_NON_INTERACTIVE_CARD_TITLE);
            this.waitForDiagramMounted(card);

            String json = (String) card.locator(".diagram-viewer")
                                       .evaluate("(viewer) => {" + "  const host = viewer.querySelector('.diagram-viewer-svg-host');" + "  return JSON.stringify({"
                                                 + "    bandCount: viewer.querySelectorAll('.diagram-viewer-controls').length,"
                                                 + "    childCount: viewer.children.length," + "    viewerClientHeight: viewer.clientHeight,"
                                                 + "    viewerClientWidth: viewer.clientWidth," + "    hostClientHeight: host.clientHeight,"
                                                 + "    hostClientWidth: host.clientWidth," + "    hostOffsetHeight: host.offsetHeight" + "  });" + "}");
            JsonNode t = OBJECT_MAPPER.readTree(json);

            System.out.println("[plan-266 S1 AC-7] non-interactive thumbnail - band elements=" + t.get("bandCount")
                                                                                                  .asInt()
                               + ", .diagram-viewer children=" + t.get("childCount")
                                                                  .asInt()
                               + ", .diagram-viewer content box " + t.get("viewerClientWidth")
                                                                     .asDouble()
                               + "x" + t.get("viewerClientHeight")
                                        .asDouble()
                               + ", host content box " + t.get("hostClientWidth")
                                                          .asDouble()
                               + "x" + t.get("hostClientHeight")
                                        .asDouble()
                               + " (measured at HEAD before this slice: " + THUMBNAIL_HOST_HEIGHT_AT_HEAD + "px tall)");

            assertAll("non-interactive thumbnail plan-266 AC-7",
                      () -> assertEquals(0, t.get("bandCount")
                                             .asInt(),
                                         "plan-266 AC-7 - a non-interactive thumbnail must contain no .diagram-viewer-controls element"),
                      () -> assertEquals(1, t.get("childCount")
                                             .asInt(),
                                         "plan-266 AC-7 - a non-interactive .diagram-viewer must have exactly one child, the scroll viewport"),
                      () -> assertEquals(t.get("viewerClientHeight")
                                          .asDouble(),
                                         t.get("hostOffsetHeight")
                                          .asDouble(),
                                         0.5,
                                         "plan-266 AC-7 - the host must still occupy .diagram-viewer's whole content height; no band height may be reserved"),
                      () -> assertEquals(THUMBNAIL_HOST_HEIGHT_AT_HEAD, t.get("hostClientHeight")
                                                                         .asDouble(),
                                         0.5, "plan-266 AC-7 - the thumbnail's rendered host height must equal the value measured at HEAD"));
        }
    }

    // ------------------------------------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------------------------------------

    /**
     * Every ancestor of {@code .diagram-viewer-svg-host}, innermost first, up to and including
     * {@code documentElement}. {@code withinComponentBoundary} marks the prefix from {@code .diagram-viewer}
     * up to and including the containing {@code .card} - see the plan-266 section header for why the
     * assertions are scoped to that prefix while the whole chain is still dumped.
     */
    private JsonNode measureAncestorChain(Locator card) throws Exception
    {
        String json = (String) card.locator(".diagram-viewer-svg-host")
                                   .evaluate("(host) => {" + "  const describe = (el) => {" + "    const cs = getComputedStyle(el);"
                                             + "    const cls = (el.className && el.className.baseVal !== undefined) ? el.className.baseVal : el.className;"
                                             + "    return { tag: el.tagName, className: String(cls || ''),"
                                             + "             overflowX: cs.overflowX, overflowY: cs.overflowY,"
                                             + "             clientWidth: el.clientWidth, clientHeight: el.clientHeight,"
                                             + "             scrollWidth: el.scrollWidth, scrollHeight: el.scrollHeight,"
                                             + "             overflowsX: el.scrollWidth > el.clientWidth,"
                                             + "             overflowsY: el.scrollHeight > el.clientHeight," + "             withinComponentBoundary: false };"
                                             + "  };" + "  const chain = [];" + "  let withinBoundary = true;" + "  let el = host.parentElement;"
                                             + "  while (el) {" + "    const entry = describe(el);" + "    entry.withinComponentBoundary = withinBoundary;"
                                             + "    if (el.classList && el.classList.contains('card')) { withinBoundary = false; }" + "    chain.push(entry);"
                                             + "    el = el.parentElement;" + "  }" + "  return JSON.stringify(chain);" + "}");
        return OBJECT_MAPPER.readTree(json);
    }

    /** The host itself, described in the same shape {@link #measureAncestorChain(Locator)} uses, so the bar count can include it. */
    private JsonNode measureHostAsChainElement(Locator card) throws Exception
    {
        String json = (String) card.locator(".diagram-viewer-svg-host")
                                   .evaluate("(el) => {" + "  const cs = getComputedStyle(el);" + "  return JSON.stringify({ tag: el.tagName,"
                                             + "    className: String(el.className || ''), overflowX: cs.overflowX, overflowY: cs.overflowY,"
                                             + "    clientWidth: el.clientWidth, clientHeight: el.clientHeight,"
                                             + "    scrollWidth: el.scrollWidth, scrollHeight: el.scrollHeight,"
                                             + "    overflowsX: el.scrollWidth > el.clientWidth, overflowsY: el.scrollHeight > el.clientHeight,"
                                             + "    withinComponentBoundary: true });" + "}");
        return OBJECT_MAPPER.readTree(json);
    }

    /** The band's and the host's border-box rects plus {@code .diagram-viewer}'s content box - for AC-4 and AC-6. */
    private JsonNode measureBandAndHost(Locator card) throws Exception
    {
        String json = (String) card.locator(".diagram-viewer")
                                   .evaluate("(viewer) => {" + "  const band = viewer.querySelector('.diagram-viewer-controls');"
                                             + "  const host = viewer.querySelector('.diagram-viewer-svg-host');" + "  const b = band.getBoundingClientRect();"
                                             + "  const h = host.getBoundingClientRect();" + "  return JSON.stringify({"
                                             + "    bandLeft: b.left, bandTop: b.top, bandRight: b.right, bandBottom: b.bottom, bandHeight: b.height,"
                                             + "    hostLeft: h.left, hostTop: h.top, hostRight: h.right, hostBottom: h.bottom,"
                                             + "    viewerClientWidth: viewer.clientWidth, viewerClientHeight: viewer.clientHeight" + "  });" + "}");
        return OBJECT_MAPPER.readTree(json);
    }

    /**
     * An element is a scroll BAR, not merely a scroll-capable box, only when both halves hold: its computed
     * overflow arms a bar on that axis AND it genuinely overflows on it. {@code auto} paints nothing at zero
     * overflow, which is exactly why the count has to be a conjunction rather than a reading of either half.
     */
    private boolean isActiveScrollRegion(JsonNode element)
    {
        boolean armedX = this.arms(element.get("overflowX")
                                          .asText());
        boolean armedY = this.arms(element.get("overflowY")
                                          .asText());
        return (armedX && element.get("overflowsX")
                                 .asBoolean())
               || (armedY && element.get("overflowsY")
                                    .asBoolean());
    }

    private boolean arms(String overflowValue)
    {
        return "auto".equals(overflowValue) || "scroll".equals(overflowValue);
    }

    private String describeChainElement(JsonNode element)
    {
        return element.get("tag")
                      .asText()
               + (element.get("className")
                         .asText()
                         .isEmpty() ? ""
                                 : "." + element.get("className")
                                                .asText()
                                                .trim()
                                                .replace(" ", "."))
               + " overflow=" + element.get("overflowX")
                                       .asText()
               + "/" + element.get("overflowY")
                              .asText()
               + " client=" + element.get("clientWidth")
                                     .asDouble()
               + "x" + element.get("clientHeight")
                              .asDouble()
               + " scroll=" + element.get("scrollWidth")
                                     .asDouble()
               + "x" + element.get("scrollHeight")
                              .asDouble()
               + " overflows=" + element.get("overflowsX")
                                        .asBoolean()
               + "/" + element.get("overflowsY")
                              .asBoolean()
               + (element.get("withinComponentBoundary")
                         .asBoolean() ? " [within component boundary]" : "");
    }

    private JsonNode chainElementByClass(JsonNode chain, String className)
    {
        for (JsonNode element : chain)
        {
            if (element.get("className")
                       .asText()
                       .equals(className))
            {
                return element;
            }
        }
        return null;
    }

    private BrowserContext newContext()
    {
        return this.browser.newContext(new Browser.NewContextOptions().setViewportSize(new ViewportSize(1600, 1000)));
    }

    private Page openBoard(BrowserContext context)
    {
        Page page = context.newPage();
        page.navigate("http://localhost:" + this.port + "/");
        page.locator(".card", new Page.LocatorOptions().setHasText(ComponentShowcaseUI.DIAGRAM_VIEWER_TALL_NARROW_CARD_TITLE))
            .first()
            .waitFor(new Locator.WaitForOptions().setTimeout(10000));
        this.waitForSettled(page);
        return page;
    }

    private void waitForSettled(Page page)
    {
        page.waitForFunction("document.querySelector('.App')?.getAttribute('data-inflight-count') === '0'");
    }

    private Locator card(Page page, String cardTitle)
    {
        return page.locator(".card", new Page.LocatorOptions().setHasText(cardTitle));
    }

    /** Waits for the injected {@code <svg>} to be present - {@code hasViewBox} is resolved client-side, on mount, regardless of interactivity. */
    private void waitForDiagramMounted(Locator card)
    {
        card.locator(".diagram-viewer-svg-host svg")
            .waitFor(new Locator.WaitForOptions().setTimeout(10000));
    }

    /** Waits for the zoom/pan controls (interactive-only) then selects the given ratio value on the {@code aria-label="Zoom ratio"} select. */
    private void setZoomRatio(Locator card, String ratioValue)
    {
        Locator controls = card.locator(".diagram-viewer-controls");
        controls.waitFor(new Locator.WaitForOptions().setTimeout(10000));
        controls.locator("select[aria-label='Zoom ratio']")
                .selectOption(ratioValue);
    }

    private void setScroll(Locator card, double left, double top)
    {
        Locator host = card.locator(".diagram-viewer-svg-host");
        host.evaluate("(host, args) => { host.scrollLeft = args.left; host.scrollTop = args.top; }", Map.of("left", left, "top", top));
    }

    /**
     * Blocks until the host's scroll offsets stop changing (plan-265 S1). Chromium animates a KEYBOARD or WHEEL
     * scroll over several frames, while {@code keyboard().press(...)} returns as soon as the event is
     * dispatched - so an immediate read lands mid-animation and sees the PRE-gesture value. Measured: an
     * ArrowRight at 400% read back {@code before=2349.0 after=2349.0} (apparently inert) while the same offset
     * was observed at {@code 2377.0} two measurements later - the key had worked all along. A programmatic
     * {@code scrollLeft =} assignment is instant and needs none of this; a synthesized gesture does.
     *
     * <p>
     * This is a STABILITY wait, not a wait for the asserted outcome: it is blind to direction and magnitude,
     * so it cannot manufacture the result the assertion then checks. A gesture that genuinely does nothing
     * settles immediately at the unchanged value and still reds the assertion.
     */
    private void waitForScrollToSettle(Locator card)
    {
        card.locator(".diagram-viewer-svg-host")
            .evaluate("(host) => new Promise((resolve) => {" + "  let lastLeft = host.scrollLeft;" + "  let lastTop = host.scrollTop;" + "  let stableFrames = 0;"
                      + "  const tick = () => {" + "    if (host.scrollLeft === lastLeft && host.scrollTop === lastTop) {"
                      + "      if (++stableFrames >= 5) { resolve(null); return; }" + "    } else {"
                      + "      stableFrames = 0; lastLeft = host.scrollLeft; lastTop = host.scrollTop;" + "    }" + "    requestAnimationFrame(tick);" + "  };"
                      + "  requestAnimationFrame(tick);" + "})");
    }

    private JsonNode measureScroll(Locator card) throws Exception
    {
        Locator host = card.locator(".diagram-viewer-svg-host");
        String json = (String) host.evaluate("(host) => JSON.stringify({ left: host.scrollLeft, top: host.scrollTop })");
        return OBJECT_MAPPER.readTree(json);
    }

    private JsonNode measureMarkerRect(Locator card) throws Exception
    {
        Locator host = card.locator(".diagram-viewer-svg-host");
        String json = (String) host.evaluate("(host) => {" + "  const marker = host.querySelector('#marker-rect');"
                                             + "  const r = marker.getBoundingClientRect();" + "  return JSON.stringify({ x: r.x, y: r.y });" + "}");
        return OBJECT_MAPPER.readTree(json);
    }

    /** {@code .diagram-viewer-svg-host}'s own overflow/box metrics, plus the injected {@code <svg>}'s rect. */
    private JsonNode measureGeometry(Locator card) throws Exception
    {
        Locator viewer = card.locator(".diagram-viewer");
        String json = (String) viewer.evaluate("(viewer) => {" + "  const host = viewer.querySelector('.diagram-viewer-svg-host');"
                                               + "  const svg = host.querySelector('svg');" + "  const hostRect = host.getBoundingClientRect();"
                                               + "  const svgRect = svg.getBoundingClientRect();" + "  return JSON.stringify({" + "    scrollWidth: host.scrollWidth,"
                                               + "    clientWidth: host.clientWidth," + "    scrollHeight: host.scrollHeight," + "    clientHeight: host.clientHeight,"
                                               + "    hostLeft: hostRect.left," + "    hostTop: hostRect.top," + "    hostWidth: hostRect.width," + "    hostHeight: hostRect.height,"
                                               + "    svgLeft: svgRect.left," + "    svgTop: svgRect.top," + "    svgWidth: svgRect.width," + "    svgHeight: svgRect.height" + "  });"
                                               + "}");
        return OBJECT_MAPPER.readTree(json);
    }

    /** The diagram viewer's own box, the host's content-box metrics, and the control cluster's box - for AC-8/AC-8b. */
    private JsonNode measureControlsAndBoxes(Locator card) throws Exception
    {
        Locator viewer = card.locator(".diagram-viewer");
        String json = (String) viewer.evaluate("(viewer) => {" + "  const host = viewer.querySelector('.diagram-viewer-svg-host');"
                                               + "  const controls = viewer.querySelector('.diagram-viewer-controls');" + "  const v = viewer.getBoundingClientRect();"
                                               + "  const h = host.getBoundingClientRect();" + "  const c = controls.getBoundingClientRect();" + "  return JSON.stringify({"
                                               + "    viewerLeft: v.left," + "    viewerTop: v.top," + "    viewerWidth: v.width," + "    viewerHeight: v.height," + "    hostLeft: h.left,"
                                               + "    hostTop: h.top," + "    hostClientWidth: host.clientWidth," + "    hostClientHeight: host.clientHeight,"
                                               + "    controlsLeft: c.left," + "    controlsTop: c.top," + "    controlsRight: c.right," + "    controlsBottom: c.bottom" + "  });" + "}");
        return OBJECT_MAPPER.readTree(json);
    }
}
