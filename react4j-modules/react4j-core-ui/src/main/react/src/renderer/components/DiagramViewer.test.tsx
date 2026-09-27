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

function stubHostBoundingClientRect(container: HTMLElement, size: number): void {
    const host = container.querySelector(".diagram-viewer-svg-host") as HTMLElement;
    host.getBoundingClientRect = () => ({
        width: size,
        height: size,
        top: 0,
        left: 0,
        right: size,
        bottom: size,
        x: 0,
        y: 0,
        toJSON: () => ({})
    });
}

function getViewBox(container: HTMLElement): string | null {
    return container.querySelector("svg")?.getAttribute("viewBox") ?? null;
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

    test("zooming in shrinks the rendered viewBox around its center", () => {
        const node = createNode(SVG_WITH_VIEWBOX);
        const { container } = render(<DiagramViewer node={node} />);

        const before = getViewBox(container);
        expect(before).toBe("0 0 100 100");

        fireEvent.click(screen.getByLabelText("Zoom in"));

        const after = getViewBox(container);
        expect(after).not.toBe(before);
        const [, , width, height] = (after as string).split(" ").map(Number);
        expect(width).toBeLessThan(100);
        expect(height).toBeLessThan(100);
    });

    test("zooming out grows the rendered viewBox around its center", () => {
        const node = createNode(SVG_WITH_VIEWBOX);
        const { container } = render(<DiagramViewer node={node} />);

        fireEvent.click(screen.getByLabelText("Zoom out"));

        const after = getViewBox(container);
        const [, , width, height] = (after as string).split(" ").map(Number);
        expect(width).toBeGreaterThan(100);
        expect(height).toBeGreaterThan(100);
    });

    test("panning (dragging) changes the viewBox origin", () => {
        const node = createNode(SVG_WITH_VIEWBOX);
        const { container } = render(<DiagramViewer node={node} />);
        stubHostBoundingClientRect(container, 100);

        const host = container.querySelector(".diagram-viewer-svg-host") as HTMLElement;
        fireEvent.mouseDown(host, { clientX: 50, clientY: 50 });
        fireEvent.mouseMove(window, { clientX: 30, clientY: 40 });
        fireEvent.mouseUp(window);

        const after = getViewBox(container);
        const [x, y, width, height] = (after as string).split(" ").map(Number);
        // dragged left/up by (20, 10) screen px at 1:1 scale -> origin moves by -(-20,-10) = (+20,+10)... i.e.
        // dragging the pointer LEFT pans the visible origin to the RIGHT (content follows the pointer).
        expect(x).toBe(20);
        expect(y).toBe(10);
        expect(width).toBe(100);
        expect(height).toBe(100);
    });

    test("reset restores the captured base viewBox exactly", () => {
        const node = createNode(SVG_WITH_VIEWBOX);
        const { container } = render(<DiagramViewer node={node} />);
        stubHostBoundingClientRect(container, 100);

        const host = container.querySelector(".diagram-viewer-svg-host") as HTMLElement;
        fireEvent.mouseDown(host, { clientX: 50, clientY: 50 });
        fireEvent.mouseMove(window, { clientX: 10, clientY: 10 });
        fireEvent.mouseUp(window);
        fireEvent.click(screen.getByLabelText("Zoom in"));

        expect(getViewBox(container)).not.toBe("0 0 100 100");

        fireEvent.click(screen.getByLabelText("Reset zoom"));

        expect(getViewBox(container)).toBe("0 0 100 100");
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

    describe("fixed zoom-ratio control", () => {
        test.each([
            ["50", 0.5, "-50 -50 200 200"],
            ["150", 1.5, "16.666666666666664 16.666666666666664 66.66666666666667 66.66666666666667"],
            ["200", 2, "25 25 50 50"],
            ["400", 4, "37.5 37.5 25 25"]
        ])("selecting %s%% sets the viewBox to a base-centered box at that ratio", (_label, ratio, expectedViewBox) => {
            const node = createNode(SVG_WITH_VIEWBOX);
            const { container } = render(<DiagramViewer node={node} />);

            const select = screen.getByLabelText("Zoom ratio") as HTMLSelectElement;
            fireEvent.change(select, { target: { value: String(ratio) } });

            expect(getViewBox(container)).toBe(expectedViewBox);
        });

        test("selecting the Fit/100% entry restores baseViewBox exactly, wired to the existing Reset", () => {
            const node = createNode(SVG_WITH_VIEWBOX);
            const { container } = render(<DiagramViewer node={node} />);

            const select = screen.getByLabelText("Zoom ratio") as HTMLSelectElement;
            fireEvent.change(select, { target: { value: "2" } });
            expect(getViewBox(container)).not.toBe("0 0 100 100");

            fireEvent.change(select, { target: { value: "1" } });

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
});
