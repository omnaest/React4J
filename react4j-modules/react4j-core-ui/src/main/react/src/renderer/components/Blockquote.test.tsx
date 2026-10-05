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

function quoteNode(fields: { [key: string]: unknown }): Node
{
    return { target: [], type: "BLOCKQUOTE", texts: [], ...fields } as unknown as Node;
}

function linkParagraph(): Node
{
    const anker = { target: [], type: "ANKER", text: { DEFAULT: "the runbook" }, title: { DEFAULT: "" }, link: "https://example.org/runbook", page: "BLANK" };
    const composite = { target: [], type: "COMPOSITE", elements: [textNode("See ")] };
    return { target: [], type: "PARAGRAPH", bold: false, elements: [composite, anker] } as unknown as Node;
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

test("texts plus footer and no elements render exactly today's markup (pin)", () => {
    const container = renderNode(quoteNode({ texts: [{ DEFAULT: "one" }, { DEFAULT: "two" }], footer: { DEFAULT: "Someone" } }));

    expect(html(container)).toBe(
        "<blockquote class=\"blockquote\">"
        + "<p class=\"mb-00\">one</p><p class=\"mb-01\">two</p>"
        + "<footer class=\"blockquote-footer\"><cite>Someone</cite></footer>"
        + "</blockquote>");
});

test("a null footer renders no footer element", () => {
    const container = renderNode(quoteNode({ texts: [{ DEFAULT: "one" }], footer: null }));

    expect(container.querySelector("blockquote")).not.toBeNull();
    expect(container.querySelector("footer")).toBeNull();
    expect(container.querySelector("cite")).toBeNull();
});

test("an absent footer renders no footer element", () => {
    expect(renderNode(quoteNode({ texts: [{ DEFAULT: "one" }] })).querySelector("footer")).toBeNull();
});

test("an empty footer value (older servers sent DEFAULT empty) renders no footer element", () => {
    expect(renderNode(quoteNode({ texts: [{ DEFAULT: "one" }], footer: { DEFAULT: "" } })).querySelector("footer")).toBeNull();
});

test("elements holding a PARAGRAPH with an ANKER render the paragraph and the link inside the blockquote", () => {
    const container = renderNode(quoteNode({ texts: [], elements: [linkParagraph()], footer: null }));

    const quote = container.querySelector("blockquote");
    expect(quote).not.toBeNull();
    expect(quote!.querySelector("p")?.textContent).toBe("See the runbook");
    const link = quote!.querySelector("a[href]");
    expect(link?.getAttribute("href")).toBe("https://example.org/runbook");
    expect(link?.textContent).toBe("the runbook");
    expect(quote!.querySelector("footer")).toBeNull();
});

test("elements render after the texts, inside the blockquote", () => {
    const container = renderNode(quoteNode({ texts: [{ DEFAULT: "lead" }], elements: [textNode("tail")], footer: null }));

    const quote = container.querySelector("blockquote")!;
    expect(quote.textContent).toBe("leadtail");
    expect(quote.querySelector("p")?.textContent).toBe("lead");
});

test("a null elements value renders like an absent one", () => {
    expect(renderNode(quoteNode({ texts: [{ DEFAULT: "one" }], elements: null, footer: null })).querySelectorAll("blockquote")).toHaveLength(1);
});

test("elements together with a footer keep the footer last", () => {
    const container = renderNode(quoteNode({ texts: [], elements: [textNode("body")], footer: { DEFAULT: "Who" } }));

    const quote = container.querySelector("blockquote")!;
    expect(quote.lastElementChild?.tagName).toBe("FOOTER");
    expect(quote.textContent).toBe("bodyWho");
});
