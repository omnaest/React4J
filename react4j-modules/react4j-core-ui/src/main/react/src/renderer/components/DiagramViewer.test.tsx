import React from "react";
import { render, fireEvent, screen } from "@testing-library/react";
import { DiagramViewer, DiagramViewerNode } from "./DiagramViewer";

function createNode(svg: string, overrides: Partial<DiagramViewerNode> = {}): DiagramViewerNode {
    return {
        target: [],
        type: DiagramViewer.TYPE,
        svg: svg,
        maxHeight: "400px",
        width: "100%",
        interactive: true,
        ...overrides
    };
}

const SVG_WITH_VIEWBOX = "<svg viewBox=\"0 0 100 100\" width=\"100\" height=\"100\"><circle cx=\"50\" cy=\"50\" r=\"10\"/></svg>";
const SVG_WITHOUT_VIEWBOX = "<svg width=\"100\" height=\"100\"><circle cx=\"50\" cy=\"50\" r=\"10\"/></svg>";

/**
 * NOTHING IN THIS FILE MAY ASSERT A LAYOUT MEASUREMENT (plan-265 §4.4 / C4-A). jsdom implements no layout
 * engine: `scrollWidth`/`clientWidth`/`scrollHeight`/`clientHeight` all read `0` and
 * `getBoundingClientRect()` returns zeroes. So "at 400% the host overflows" (AC-1) would be the unprovable
 * `0 > 0`, and "at Fit it does not" (AC-2) would be the VACUOUSLY TRUE `0 === 0` - green with no
 * implementation at all. Stubbing those numbers is worse than omitting them, because the oracle would then
 * be reading back the very value the subject is supposed to produce. AC-1/AC-2/AC-3/AC-6/AC-10 are therefore
 * measured in real Chromium by `DiagramViewerZoomOverflowIT` (react4j-ui-test) and are deliberately absent
 * here. What jest owns is the non-geometric half: the scale algebra, the viewBox invariant (AC-4), that a
 * drag writes the scroll offsets at all (AC-7's jest half), the node-to-prop plumbing, and that the
 * non-interactive rendering is unchanged.
 *
 * Writing and reading `scrollLeft`/`scrollTop` is admissible and is not a stub: they are plain writable
 * properties in jsdom, so the assertion observes the component's own write rather than a value the test
 * supplied. It is only whether the CONTENT then moves that needs a layout engine - which is the IT's half.
 */
function getViewBox(container: HTMLElement): string | null {
    return container.querySelector("svg")?.getAttribute("viewBox") ?? null;
}

function getHost(container: HTMLElement): HTMLElement {
    return container.querySelector(".diagram-viewer-svg-host") as HTMLElement;
}

function getViewport(container: HTMLElement): HTMLElement {
    return container.querySelector(".diagram-viewer-viewport") as HTMLElement;
}

function getCanvas(container: HTMLElement): HTMLElement {
    return container.querySelector(".diagram-viewer-canvas") as HTMLElement;
}

/**
 * The scale as it actually reaches the stylesheet - the `--diagram-viewer-scale` custom property on the
 * scroll host, which is the whole of seam D1 (component to stylesheet). An EMPTY string is the meaningful
 * "Fit" value, not a missing assertion: the component deliberately omits the property rather than emitting
 * `1`, so that DiagramViewer.css's `var(..., 1)` fallback makes an unzoomed viewer structurally identical
 * to the pre-plan-265 rendering.
 */
function getEmittedScale(container: HTMLElement): string {
    return getHost(container).style.getPropertyValue("--diagram-viewer-scale");
}

function dragBy(container: HTMLElement, deltaX: number, deltaY: number): void {
    const host = getHost(container);
    fireEvent.mouseDown(host, { clientX: 50, clientY: 50 });
    fireEvent.mouseMove(window, { clientX: 50 + deltaX, clientY: 50 + deltaY });
    fireEvent.mouseUp(window);
}

describe("DiagramViewer", () => {
    test("renders the SVG markup", () => {
        const node = createNode(SVG_WITH_VIEWBOX);
        const { container } = render(<DiagramViewer node={node} />);

        expect(container.querySelector("svg")).not.toBeNull();
        expect(container.querySelector("circle")).not.toBeNull();
    });

    test("the root element carries the node's sizing as an inline style", () => {
        const node = createNode(SVG_WITH_VIEWBOX, { width: "77%", maxHeight: "321px", height: "600px" });
        const { container } = render(<DiagramViewer node={node} />);

        const root = container.firstElementChild as HTMLElement;
        expect(root.className).toBe("diagram-viewer");
        expect(root.style.width).toBe("77%");
        expect(root.style.maxHeight).toBe("321px");
        expect(root.style.height).toBe("600px");
    });

    test("an absent height leaves no height in the inline style", () => {
        const node = createNode(SVG_WITH_VIEWBOX, { width: "77%", maxHeight: "321px" });
        const { container } = render(<DiagramViewer node={node} />);

        const root = container.firstElementChild as HTMLElement;
        expect(root.style.getPropertyValue("height")).toBe("");
        // "max-height" alone must not be mistaken for a set "height" - check the raw style attribute text
        // for a bare "height:" declaration distinct from "max-height:".
        const styleAttr = root.getAttribute("style") || "";
        expect(/(^|;|\s)height\s*:/.test(styleAttr.replace(/max-height\s*:[^;]*;?/g, ""))).toBe(false);
    });

    // Replaces "zooming in shrinks the rendered viewBox around its center". Zoom no longer touches the
    // viewBox at all, so the old assertion is unsatisfiable by the current mechanism. The replacement pins
    // BOTH halves of the swap - the scale went up AND the viewBox did not move - where the old one pinned
    // only that some number got smaller.
    test("zooming in raises the emitted scale and leaves the viewBox at the base", () => {
        const node = createNode(SVG_WITH_VIEWBOX);
        const { container } = render(<DiagramViewer node={node} />);

        expect(getEmittedScale(container)).toBe("");

        fireEvent.click(screen.getByLabelText("Zoom in"));

        expect(Number(getEmittedScale(container))).toBeGreaterThan(1);
        expect(getViewBox(container)).toBe("0 0 100 100");
    });

    // Replaces "zooming out grows the rendered viewBox around its center", for the same reason.
    test("zooming out lowers the emitted scale and leaves the viewBox at the base", () => {
        const node = createNode(SVG_WITH_VIEWBOX);
        const { container } = render(<DiagramViewer node={node} />);

        fireEvent.click(screen.getByLabelText("Zoom out"));

        const scale = Number(getEmittedScale(container));
        expect(scale).toBeGreaterThan(0);
        expect(scale).toBeLessThan(1);
        expect(getViewBox(container)).toBe("0 0 100 100");
    });

    // Replaces "panning (dragging) changes the viewBox origin" - AC-7's jest half. A SYNTHESIZED drag, not a
    // direct scrollLeft assignment: the numbers asserted are the component's own writes, derived from the
    // pointer deltas, so the assertion would still fail if the retarget were dropped or inverted. Whether the
    // content then visibly moves needs a layout engine and is the IT's half of AC-7.
    test("panning (dragging) scrolls the host instead of rewriting the viewBox", () => {
        const node = createNode(SVG_WITH_VIEWBOX);
        const { container } = render(<DiagramViewer node={node} />);
        const host = getHost(container);
        expect(host.scrollLeft).toBe(0);
        expect(host.scrollTop).toBe(0);

        // Dragging the pointer LEFT/UP moves the diagram left/up, i.e. reveals content further right/down,
        // i.e. INCREASES both offsets - content follows the pointer, as the old viewBox pan also did.
        dragBy(container, -20, -10);

        expect(host.scrollLeft).toBe(20);
        expect(host.scrollTop).toBe(10);
        expect(getViewBox(container)).toBe("0 0 100 100");
    });

    // AC-4, and the non-regression guard against a quiet slide back to viewBox rewriting. Deliberately
    // separate from the reset test below: once the viewBox is invariant by construction, "reset restores the
    // base viewBox" is VACUOUSLY true - true whether or not resetZoom does anything at all - so it cannot be
    // reset's own assertion. Here the invariance is the subject, exercised across every mechanism that used
    // to rewrite it.
    test("the viewBox is never rewritten - it stays the base across ratio, zoom, drag and reset", () => {
        const node = createNode(SVG_WITH_VIEWBOX);
        const { container } = render(<DiagramViewer node={node} />);
        const select = screen.getByLabelText("Zoom ratio") as HTMLSelectElement;

        fireEvent.change(select, { target: { value: "4" } });
        expect(getViewBox(container)).toBe("0 0 100 100");

        fireEvent.click(screen.getByLabelText("Zoom in"));
        expect(getViewBox(container)).toBe("0 0 100 100");

        dragBy(container, -30, -30);
        expect(getViewBox(container)).toBe("0 0 100 100");

        fireEvent.click(screen.getByLabelText("Reset zoom"));
        expect(getViewBox(container)).toBe("0 0 100 100");
    });

    // Replaces "reset restores the captured base viewBox exactly", retargeted to what reset now DOES. See the
    // comment on the invariance test above for why the old assertion could not simply be kept.
    test("reset returns the emitted scale to Fit", () => {
        const node = createNode(SVG_WITH_VIEWBOX);
        const { container } = render(<DiagramViewer node={node} />);

        fireEvent.change(screen.getByLabelText("Zoom ratio"), { target: { value: "4" } });
        fireEvent.click(screen.getByLabelText("Zoom in"));
        expect(getEmittedScale(container)).not.toBe("");

        fireEvent.click(screen.getByLabelText("Reset zoom"));

        // Back to the structural Fit rendering: the property is omitted entirely, not emitted as "1", so the
        // stylesheet's var(..., 1) fallback reproduces the pre-plan-265 box exactly.
        expect(getEmittedScale(container)).toBe("");
    });

    test("an SVG with no viewBox does not crash and hides the zoom/pan controls", () => {
        const node = createNode(SVG_WITHOUT_VIEWBOX);

        const { container } = render(<DiagramViewer node={node} />);

        expect(container.querySelector("svg")).not.toBeNull();
        expect(screen.queryByLabelText("Zoom in")).toBeNull();
        expect(screen.queryByLabelText("Zoom out")).toBeNull();
        expect(screen.queryByLabelText("Reset zoom")).toBeNull();
        // The ratio control is gated by the SAME showControls flag as the buttons above, not a second
        // condition (plan-261 AC-S2-6) - assert its absence too, not just the buttons'.
        expect(screen.queryByLabelText("Zoom ratio")).toBeNull();
    });

    test("non-interactive rendering hides the zoom/pan controls even with a viewBox", () => {
        const node = createNode(SVG_WITH_VIEWBOX, { interactive: false });
        render(<DiagramViewer node={node} />);

        expect(screen.queryByLabelText("Zoom in")).toBeNull();
        expect(screen.queryByLabelText("Zoom ratio")).toBeNull();
    });

    // The "Fit is unchanged" guarantee at the markup level: a thumbnail must gain NONE of the scroll-region
    // surface, so that DiagramViewer.css's var(..., 1) fallback leaves it at exactly 100% with no overflow,
    // no scrollbar and nothing in the tab order. The overflow half of this is AC-2, measured in the IT.
    test("a non-interactive host gains no scale, no tabIndex and no scroll-region role", () => {
        const node = createNode(SVG_WITH_VIEWBOX, { interactive: false });
        const { container } = render(<DiagramViewer node={node} />);

        const host = getHost(container);
        expect(getEmittedScale(container)).toBe("");
        expect(host.getAttribute("tabindex")).toBeNull();
        expect(host.getAttribute("role")).toBeNull();
        expect(host.getAttribute("aria-label")).toBeNull();
    });

    // AC-11's jest-expressible half: the host must be in the tab order and named, which is what makes the
    // browser's own arrow-key scrolling reach it. That the keys then MOVE it needs a layout engine and a real
    // scroll region, so it is measured in the IT.
    test("an interactive host is focusable and named as a scrollable region", () => {
        const node = createNode(SVG_WITH_VIEWBOX);
        const { container } = render(<DiagramViewer node={node} />);

        const host = getHost(container);
        expect(host.getAttribute("tabindex")).toBe("0");
        expect(host.getAttribute("role")).toBe("region");
        expect(host.getAttribute("aria-label")).toBe("Scrollable diagram");
    });

    // ------------------------------------------------------------------------------------------------
    // plan-266 S1 - the control band. These four are the jest-expressible half of AC-6/AC-7: DOM
    // PRESENCE, DOM ORDER and DOM CONTAINMENT, all of which jsdom answers truthfully because none of
    // them is a layout measurement. The GEOMETRIC half of AC-6 - that the band's rect does not intersect
    // the host's, and does not move when the host is scrolled - is measured in real Chromium by
    // DiagramViewerZoomOverflowIT, and is deliberately NOT stubbed here (see the file note above: two
    // zero rects make a non-intersection assertion vacuously true, which is worse than its absence).
    // ------------------------------------------------------------------------------------------------

    // AC-6, DOM half. "Above the host" is DOM order, because DiagramViewer.css lays .diagram-viewer out as
    // a column flex container with no "order" property anywhere - so document order IS visual order, and
    // pinning it here is pinning the real mechanism rather than a proxy for it. Asserts the INDEX pair, not
    // merely "both exist": a band emitted after the host would satisfy any presence-only assertion while
    // rendering below the diagram.
    test("the control band is a direct child of the viewer, rendered BEFORE the scroll viewport", () => {
        const node = createNode(SVG_WITH_VIEWBOX);
        const { container } = render(<DiagramViewer node={node} />);

        const root = container.firstElementChild as HTMLElement;
        const children = Array.from(root.children);
        const band = root.querySelector(".diagram-viewer-controls") as HTMLElement;
        const viewport = getViewport(container);

        expect(band).not.toBeNull();
        expect(band.parentElement).toBe(root);
        expect(children.indexOf(band)).toBe(0);
        expect(children.indexOf(viewport)).toBe(1);
        expect(children.indexOf(band)).toBeLessThan(children.indexOf(viewport));
    });

    // The viewport is not decoration and is not an arbitrary wrapper - it is the element that carries the
    // ROW flex axis the host's percentage-sized svg needs (plan-266 S1, hypothesis H-COL falsified by
    // measurement; the full mechanism is in DiagramViewer.css). What jest can pin is the SHAPE that
    // mechanism requires: the viewport sits strictly between the viewer and the host, and the host is its
    // only child. Flatten the two back into one and this reds; the browser consequence - the svg taking
    // its intrinsic aspect-ratio height instead of the host's - is measured by the IT.
    test("the scroll host sits inside the viewport, which sits inside the viewer", () => {
        const node = createNode(SVG_WITH_VIEWBOX);
        const { container } = render(<DiagramViewer node={node} />);

        const root = container.firstElementChild as HTMLElement;
        const viewport = getViewport(container);
        const host = getHost(container);

        expect(viewport.parentElement).toBe(root);
        expect(host.parentElement).toBe(viewport);
        expect(viewport.children.length).toBe(1);
    });

    // AC-6, the "outside the scroll region" half that does not need a layout engine. The band scrolling away
    // with the diagram is exactly what happens if it is nested INSIDE the overflow:auto host, and that is a
    // containment fact, not a geometric one - so it is provable here and the assertion bites: move the JSX
    // block inside the host div and this reds immediately.
    test("the control band is NOT inside the scroll host, so it cannot scroll away with the diagram", () => {
        const node = createNode(SVG_WITH_VIEWBOX);
        const { container } = render(<DiagramViewer node={node} />);

        const band = container.querySelector(".diagram-viewer-controls") as HTMLElement;
        const host = getHost(container);

        expect(host.contains(band)).toBe(false);
        expect(band.contains(host)).toBe(false);
    });

    // AC-7, jest half. The existing non-interactive tests assert the absence of each CONTROL by its label;
    // this asserts the absence of the BAND ELEMENT ITSELF, which is the thing that would reserve height.
    // A band rendered empty (or rendered with its children gated instead of the band gated) would pass
    // every label-absence assertion above and still cost a 220px thumbnail the band's height.
    test("a non-interactive viewer emits no band element at all, so it reserves no band height", () => {
        const node = createNode(SVG_WITH_VIEWBOX, { interactive: false });
        const { container } = render(<DiagramViewer node={node} />);

        const root = container.firstElementChild as HTMLElement;
        expect(root.querySelector(".diagram-viewer-controls")).toBeNull();
        // The scroll viewport must be the viewer's ONLY child - that is what makes "flex: 1 1 auto" give it
        // the whole box, and it is the property the IT then measures in pixels.
        expect(root.children.length).toBe(1);
        expect(root.children[0]).toBe(getViewport(container));
    });

    // Same guarantee on the other gate: hasViewBox, not interactive. Both halves of `showControls` must
    // suppress the whole band, not just its contents.
    test("an SVG with no viewBox emits no band element either", () => {
        const node = createNode(SVG_WITHOUT_VIEWBOX);
        const { container } = render(<DiagramViewer node={node} />);

        const root = container.firstElementChild as HTMLElement;
        expect(root.querySelector(".diagram-viewer-controls")).toBeNull();
        expect(root.children.length).toBe(1);
    });

    // The band is a real landmark now that it is a real element rather than a floating overlay, so it is
    // named for a screen reader. Its four controls keep their own labels, asserted throughout this file.
    test("the control band is an accessible, named group", () => {
        const node = createNode(SVG_WITH_VIEWBOX);
        const { container } = render(<DiagramViewer node={node} />);

        const band = container.querySelector(".diagram-viewer-controls") as HTMLElement;
        expect(band.getAttribute("role")).toBe("group");
        expect(band.getAttribute("aria-label")).toBe("Diagram zoom controls");
        expect(screen.getByLabelText("Diagram zoom controls")).toBe(band);
    });

    describe("fixed zoom-ratio control", () => {
        // Replaces the four exact-viewBox-literal cases ("-50 -50 200 200" etc.). One case per ratio, as
        // before, now asserting the emitted scale AND that the viewBox is still the base - strictly stronger
        // than what it replaces, which pinned one half of the mechanism where this pins both.
        test.each([
            ["50", 0.5],
            ["150", 1.5],
            ["200", 2],
            ["400", 4]
        ])("selecting %s%% emits that scale and leaves the viewBox at the base", (_label, ratio) => {
            const node = createNode(SVG_WITH_VIEWBOX);
            const { container } = render(<DiagramViewer node={node} />);

            const select = screen.getByLabelText("Zoom ratio") as HTMLSelectElement;
            fireEvent.change(select, { target: { value: String(ratio) } });

            expect(getEmittedScale(container)).toBe(String(ratio));
            expect(getViewBox(container)).toBe("0 0 100 100");
        });

        test("selecting the Fit/100% entry returns the scale to Fit, wired to the existing Reset", () => {
            const node = createNode(SVG_WITH_VIEWBOX);
            const { container } = render(<DiagramViewer node={node} />);

            const select = screen.getByLabelText("Zoom ratio") as HTMLSelectElement;
            fireEvent.change(select, { target: { value: "2" } });
            expect(getEmittedScale(container)).toBe("2");

            fireEvent.change(select, { target: { value: "1" } });

            expect(getEmittedScale(container)).toBe("");
            expect(getViewBox(container)).toBe("0 0 100 100");
        });

        test("the control starts on the Fit entry", () => {
            const node = createNode(SVG_WITH_VIEWBOX);
            render(<DiagramViewer node={node} />);

            const select = screen.getByLabelText("Zoom ratio") as HTMLSelectElement;
            expect(select.value).toBe("1");
        });

        test("after a zoomIn, the displayed ratio is no longer the Fit entry (scale state stays in sync)", () => {
            const node = createNode(SVG_WITH_VIEWBOX);
            render(<DiagramViewer node={node} />);

            const select = screen.getByLabelText("Zoom ratio") as HTMLSelectElement;
            expect(select.value).toBe("1");

            fireEvent.click(screen.getByLabelText("Zoom in"));

            // zoomIn multiplies by ZOOM_FACTOR (1.2), landing BETWEEN the fixed ratios - the control shows
            // the neutral "custom" marker rather than snapping to a nearest fixed label (see DiagramViewer.tsx
            // CUSTOM_RATIO_VALUE doc for why: a nearest-label snap would misreport the actual scale).
            expect(select.value).toBe("custom");
            expect(select.value).not.toBe("1");
        });

        test("resetting zoom returns the control to the Fit entry", () => {
            const node = createNode(SVG_WITH_VIEWBOX);
            render(<DiagramViewer node={node} />);

            fireEvent.click(screen.getByLabelText("Zoom in"));
            const select = screen.getByLabelText("Zoom ratio") as HTMLSelectElement;
            expect(select.value).toBe("custom");

            fireEvent.click(screen.getByLabelText("Reset zoom"));

            expect(select.value).toBe("1");
        });
    });

    // ------------------------------------------------------------------------------------------------
    // plan-266 Cliff N4 - auto-fit becomes COVER, plus the "Whole diagram" contain entry. The GEOMETRIC
    // claims (which axis overflows, that the fill is exact, that nothing is lost) are unprovable in jsdom
    // (no layout engine - see the file note at the top) and are measured by DiagramViewerZoomOverflowIT.
    // What jest CAN and does pin: the DOM shape the CSS mechanism depends on (the canvas wrapper, the svg
    // moved inside it), the "--cover" class is applied/withheld by the right conditions, the aspect custom
    // property is authored data derived from viewBox (not a measurement), and the ratio control's new
    // sixth entry and its distinct selection state.
    // ------------------------------------------------------------------------------------------------

    test("the injected svg renders inside .diagram-viewer-canvas, which sits inside the scroll host", () => {
        const node = createNode(SVG_WITH_VIEWBOX);
        const { container } = render(<DiagramViewer node={node} />);

        const host = getHost(container);
        const canvas = getCanvas(container);
        expect(canvas).not.toBeNull();
        expect(canvas.parentElement).toBe(host);
        expect(canvas.querySelector("svg")).not.toBeNull();
        expect(canvas.contains(container.querySelector("svg"))).toBe(true);
    });

    test("an interactive viewer at Fit (the default) carries the cover modifier class on the host", () => {
        const node = createNode(SVG_WITH_VIEWBOX);
        const { container } = render(<DiagramViewer node={node} />);

        expect(getHost(container).className).toBe("diagram-viewer-svg-host diagram-viewer-svg-host--cover");
    });

    test("a non-interactive thumbnail never carries the cover modifier class, even with a viewBox", () => {
        const node = createNode(SVG_WITH_VIEWBOX, { interactive: false });
        const { container } = render(<DiagramViewer node={node} />);

        expect(getHost(container).className).toBe("diagram-viewer-svg-host");
    });

    test("an SVG with no viewBox never carries the cover modifier class (no aspect data to cover with)", () => {
        const node = createNode(SVG_WITHOUT_VIEWBOX);
        const { container } = render(<DiagramViewer node={node} />);

        expect(getHost(container).className).toBe("diagram-viewer-svg-host");
    });

    test("the canvas publishes --diagram-viewer-aspect from the parsed base viewBox, not a measurement", () => {
        const node = createNode(SVG_WITH_VIEWBOX);
        const { container } = render(<DiagramViewer node={node} />);

        // SVG_WITH_VIEWBOX is "0 0 100 100" - width/height both 100.
        expect(getCanvas(container).style.getPropertyValue("--diagram-viewer-aspect")).toBe("100 / 100");
    });

    describe("the Whole diagram (contain) ratio entry", () => {
        test("sits between 50% and Fit (100%) in the option list", () => {
            const node = createNode(SVG_WITH_VIEWBOX);
            render(<DiagramViewer node={node} />);

            const select = screen.getByLabelText("Zoom ratio") as HTMLSelectElement;
            const labels = Array.from(select.options).map((option) => option.text);
            expect(labels).toEqual(["50%", "Whole diagram", "Fit (100%)", "150%", "200%", "400%"]);
        });

        test("selecting it drops the cover modifier class - contain, not cover", () => {
            const node = createNode(SVG_WITH_VIEWBOX);
            const { container } = render(<DiagramViewer node={node} />);

            expect(getHost(container).className).toContain("diagram-viewer-svg-host--cover");

            fireEvent.change(screen.getByLabelText("Zoom ratio"), { target: { value: "whole" } });

            expect(getHost(container).className).toBe("diagram-viewer-svg-host");
        });

        test("selecting it emits no --diagram-viewer-scale (its scale is Fit's own value, 1)", () => {
            const node = createNode(SVG_WITH_VIEWBOX);
            const { container } = render(<DiagramViewer node={node} />);

            fireEvent.change(screen.getByLabelText("Zoom ratio"), { target: { value: "whole" } });

            expect(getEmittedScale(container)).toBe("");
            expect(getViewBox(container)).toBe("0 0 100 100");
        });

        test("is a DISTINCT selection state from Fit even though both carry scale 1", () => {
            const node = createNode(SVG_WITH_VIEWBOX);
            render(<DiagramViewer node={node} />);
            const select = screen.getByLabelText("Zoom ratio") as HTMLSelectElement;
            expect(select.value).toBe("1");

            fireEvent.change(select, { target: { value: "whole" } });
            expect(select.value).toBe("whole");
            expect(select.value).not.toBe("1");

            fireEvent.change(select, { target: { value: "1" } });
            expect(select.value).toBe("1");
        });

        test("choosing a fixed ratio after Whole diagram returns to cover", () => {
            const node = createNode(SVG_WITH_VIEWBOX);
            const { container } = render(<DiagramViewer node={node} />);
            const select = screen.getByLabelText("Zoom ratio") as HTMLSelectElement;

            fireEvent.change(select, { target: { value: "whole" } });
            expect(getHost(container).className).toBe("diagram-viewer-svg-host");

            fireEvent.change(select, { target: { value: "2" } });

            expect(getHost(container).className).toBe("diagram-viewer-svg-host diagram-viewer-svg-host--cover");
            expect(getEmittedScale(container)).toBe("2");
        });

        test("zooming in from Whole diagram returns to cover mode", () => {
            const node = createNode(SVG_WITH_VIEWBOX);
            const { container } = render(<DiagramViewer node={node} />);

            fireEvent.change(screen.getByLabelText("Zoom ratio"), { target: { value: "whole" } });
            expect(getHost(container).className).toBe("diagram-viewer-svg-host");

            fireEvent.click(screen.getByLabelText("Zoom in"));

            expect(getHost(container).className).toBe("diagram-viewer-svg-host diagram-viewer-svg-host--cover");
        });

        test("resetting zoom from Whole diagram returns to the Fit entry, in cover mode", () => {
            const node = createNode(SVG_WITH_VIEWBOX);
            const { container } = render(<DiagramViewer node={node} />);
            const select = screen.getByLabelText("Zoom ratio") as HTMLSelectElement;

            fireEvent.change(select, { target: { value: "whole" } });
            fireEvent.click(screen.getByLabelText("Reset zoom"));

            expect(select.value).toBe("1");
            expect(getHost(container).className).toBe("diagram-viewer-svg-host diagram-viewer-svg-host--cover");
        });
    });
});
