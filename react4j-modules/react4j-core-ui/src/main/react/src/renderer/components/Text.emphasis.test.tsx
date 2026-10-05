import React from "react";
import { render } from "@testing-library/react";
import { Renderer, Node } from "../Renderer";

// Explicit factory mock, same reason as in Breadcrumb.test.tsx (axios ESM build is not transformable).
jest.mock("../../backend/Backend", () => ({
    Backend: {
        uploadFile: jest.fn(),
        sendEvent: jest.fn(),
        getUI: jest.fn(),
        getUISubNode: jest.fn(),
        fetchData: jest.fn()
    },
    BackendUri: {
        URI_UI: "ui",
        URI_UI_HANDLER: "ui/event",
        URI_UI_DATA_SOURCE: "ui/data/query",
        URI_UPLOAD: "ui/upload",
        resolve: jest.fn((uri: string) => uri)
    }
}));

function textNode(text: string, extra: { [key: string]: unknown } = {}): Node
{
    return { target: [], type: "TEXT", texts: [{ DEFAULT: text }], ...extra } as unknown as Node;
}

function renderNode(node: Node): HTMLElement
{
    return render(<>{Renderer.render(node)}</>).container;
}

// The Renderer stamps data-location on the root element of a node; it is not part of what these tests pin.
function html(container: HTMLElement): string
{
    return container.innerHTML.replace(/ data-location=""/g, "");
}

test("a plain TEXT node renders a bare fragment: no element of its own (pin)", () => {
    const container = renderNode(textNode("plain"));

    expect(html(container)).toBe("plain");
    expect(container.children).toHaveLength(0);
});

test("a TEXT node with null emphasis renders the same bare fragment", () => {
    const container = renderNode(textNode("plain", { emphasis: null, style: null }));

    expect(html(container)).toBe("plain");
});

test("an empty emphasis array renders the bare fragment", () => {
    expect(html(renderNode(textNode("plain", { emphasis: [] })))).toBe("plain");
});

test("emphasis BOLD and STRIKETHROUGH render strong around del around the text (enum order, outermost first)", () => {
    const container = renderNode(textNode("x", { emphasis: ["BOLD", "STRIKETHROUGH"] }));

    expect(html(container)).toBe("<strong><del>x</del></strong>");
});

test("the nesting follows the enum order even when the array arrives in another order", () => {
    const container = renderNode(textNode("x", { emphasis: ["STRIKETHROUGH", "ITALIC", "BOLD"] }));

    expect(html(container)).toBe("<strong><em><del>x</del></em></strong>");
});

test.each([
    ["BOLD", "<strong>x</strong>"],
    ["ITALIC", "<em>x</em>"],
    ["STRIKETHROUGH", "<del>x</del>"]
])("emphasis %s alone renders %s", (emphasis, expected) => {
    expect(html(renderNode(textNode("x", { emphasis: [emphasis] })))).toBe(expected);
});

test("an unknown emphasis member is ignored rather than breaking the render", () => {
    expect(html(renderNode(textNode("x", { emphasis: ["BOLD", "SPARKLY"] })))).toBe("<strong>x</strong>");
});

test("style MUTED plus emphasis keeps the span outermost, the emphasis elements inside it", () => {
    const container = renderNode(textNode("x", { style: "MUTED", emphasis: ["BOLD", "STRIKETHROUGH"] }));

    expect(html(container)).toBe("<span class=\"text-body-secondary\"><strong><del>x</del></strong></span>");
});

test("style MUTED without emphasis stays today's span (pin)", () => {
    expect(html(renderNode(textNode("x", { style: "MUTED" })))).toBe("<span class=\"text-body-secondary\">x</span>");
});

test("several texts all sit inside the innermost emphasis element", () => {
    const node = { target: [], type: "TEXT", texts: [{ DEFAULT: "a" }, { DEFAULT: "b" }], emphasis: ["ITALIC"] } as unknown as Node;

    expect(html(renderNode(node))).toBe("<em>ab</em>");
});
