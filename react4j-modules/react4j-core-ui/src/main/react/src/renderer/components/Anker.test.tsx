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
function ankerNode(fields: { [key: string]: unknown } = {}): Node
{
    return { target: [], type: "ANKER", text: { DEFAULT: "the runbook" }, title: { DEFAULT: "Open it" }, link: "https://example.org/runbook", page: "BLANK", ...fields } as unknown as Node;
}

test("an anker without elements renders today's markup (pin)", () => {
    expect(html(renderNode(ankerNode()))).toBe(
        "<a href=\"https://example.org/runbook\" target=\"_blank\" rel=\"noopener noreferrer\" title=\"Open it\">the runbook</a>");
});

test("an anker on the same page renders target _self (pin)", () => {
    expect(renderNode(ankerNode({ page: "SELF" })).querySelector("a")?.getAttribute("target")).toBe("_self");
});

test("an anker with a null elements value renders the same markup as one without (pin)", () => {
    expect(html(renderNode(ankerNode({ elements: null })))).toBe(html(renderNode(ankerNode())));
});

// React logs a missing-key warning once per process, so this must stay the first test that renders elements.
test("anker elements render without React key warnings", () => {
    const spy = jest.spyOn(console, "error").mockImplementation(() => undefined);
    try
    {
        const first = { target: [], type: "TEXT", texts: [{ DEFAULT: "a" }], emphasis: ["BOLD"] };
        const second = { target: [], type: "TEXT", texts: [{ DEFAULT: "b" }], emphasis: ["BOLD"] };
        renderNode(ankerNode({ elements: [first, second] }));

        expect(spy.mock.calls.filter(call => String(call[0]).includes("unique \"key\""))).toHaveLength(0);
    }
    finally
    {
        spy.mockRestore();
    }
});

test("an anker with a BOLD text element renders the strong inside the anchor, after the text", () => {
    const bold = { target: [], type: "TEXT", texts: [{ DEFAULT: "linked" }], emphasis: ["BOLD"] };
    const container = renderNode(ankerNode({ text: { DEFAULT: "lead " }, elements: [bold] }));

    const anchor = container.querySelector("a") as HTMLElement;
    expect(anchor.querySelector("strong")?.textContent).toBe("linked");
    expect(anchor.textContent).toBe("lead linked");
    expect(html(container)).toBe(
        "<a href=\"https://example.org/runbook\" target=\"_blank\" rel=\"noopener noreferrer\" title=\"Open it\">lead <strong>linked</strong></a>");
});

test("an anker with an empty text and elements only renders just the elements", () => {
    const bold = { target: [], type: "TEXT", texts: [{ DEFAULT: "b" }], emphasis: ["BOLD"] };
    const plain = { target: [], type: "TEXT", texts: [{ DEFAULT: " x" }] };
    const anchor = renderNode(ankerNode({ text: { DEFAULT: "" }, elements: [bold, plain] })).querySelector("a") as HTMLElement;

    expect(anchor.innerHTML.replace(/ data-location=""/g, "")).toBe("<strong>b</strong> x");
});

test("an anker with an empty elements array renders today's markup", () => {
    expect(html(renderNode(ankerNode({ elements: [] })))).toBe(html(renderNode(ankerNode())));
});
