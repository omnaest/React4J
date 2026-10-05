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

function renderNode(node: Node): HTMLElement
{
    return render(<>{Renderer.render(node)}</>).container;
}

// The Renderer stamps data-location on the root element of a node; it is not part of what these tests pin.
function html(container: HTMLElement): string
{
    return container.innerHTML.replace(/ data-location=""/g, "");
}
function textNode(text: string): Node
{
    return { target: [], type: "TEXT", texts: [{ DEFAULT: text }] } as unknown as Node;
}

function alignmentNode(fields: { [key: string]: unknown }): Node
{
    return { target: [], type: "TEXTALIGNMENTCONTAINER", content: textNode("cell"), ...fields } as unknown as Node;
}

function renderedSpan(fields: { [key: string]: unknown }): HTMLElement
{
    return renderNode(alignmentNode(fields)).querySelector("span") as HTMLElement;
}

test("a container with nothing set renders today's markup: an empty class attribute (pin)", () => {
    expect(html(renderNode(alignmentNode({})))).toBe("<span class=\"\">cell</span>");
});

test("a falsy flags / empty alignments container renders the same empty class attribute (pin)", () => {
    expect(html(renderNode(alignmentNode({ ellipsis: false, nowrap: false, verticalAlignment: null, horizontalAlignment: null })))).toBe("<span class=\"\">cell</span>");
});

test("a lone center alignment already renders text-center (pin)", () => {
    expect(Array.from(renderedSpan({ horizontalAlignment: "center" }).classList)).toEqual(["text-center"]);
});

test("a lone vertical alignment already renders its align- class (pin)", () => {
    expect(Array.from(renderedSpan({ verticalAlignment: "top" }).classList)).toEqual(["align-top"]);
});

test("horizontal alignment left maps to text-start", () => {
    expect(Array.from(renderedSpan({ horizontalAlignment: "left" }).classList)).toEqual(["text-start"]);
});

test("horizontal alignment right maps to text-end", () => {
    expect(Array.from(renderedSpan({ horizontalAlignment: "right" }).classList)).toEqual(["text-end"]);
});

test("horizontal alignment center maps to text-center", () => {
    expect(Array.from(renderedSpan({ horizontalAlignment: "center" }).classList)).toEqual(["text-center"]);
});

test("vertical, horizontal, ellipsis and nowrap together each render as their own class token", () => {
    const span = renderedSpan({ verticalAlignment: "top", horizontalAlignment: "right", ellipsis: true, nowrap: true });

    expect(Array.from(span.classList).sort()).toEqual(["align-top", "text-end", "text-nowrap", "text-truncate"]);
    expect(span.className.split(" ")).toHaveLength(4);
});

test("two classes together are separated, not glued (vertical + horizontal)", () => {
    const span = renderedSpan({ verticalAlignment: "middle", horizontalAlignment: "left" });

    expect(span.className).not.toMatch(/align-middletext/);
    expect(Array.from(span.classList).sort()).toEqual(["align-middle", "text-start"]);
});

test("the dead Bootstrap 4 text-left and text-right classes are never emitted", () => {
    expect(renderedSpan({ horizontalAlignment: "left" }).classList.contains("text-left")).toBe(false);
    expect(renderedSpan({ horizontalAlignment: "right" }).classList.contains("text-right")).toBe(false);
});
