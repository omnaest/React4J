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

function paragraphNode(bold: boolean): Node
{
    return { target: [], type: "PARAGRAPH", bold, elements: [textNode("one"), textNode("two")] } as unknown as Node;
}

test("a non-bold paragraph renders today's markup: an empty class attribute (pin)", () => {
    expect(html(renderNode(paragraphNode(false)))).toBe("<p class=\"\"><span>one</span><span>two</span></p>");
});

test("a bold paragraph renders the Bootstrap 5 fw-bold class with the same body", () => {
    expect(html(renderNode(paragraphNode(true)))).toBe("<p class=\"fw-bold\"><span>one</span><span>two</span></p>");
});

test("a bold paragraph no longer carries the Bootstrap 4 font-weight-bold class", () => {
    expect(renderNode(paragraphNode(true)).querySelector("p")?.classList.contains("font-weight-bold")).toBe(false);
});
