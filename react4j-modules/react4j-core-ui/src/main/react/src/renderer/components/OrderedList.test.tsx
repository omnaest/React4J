import React from "react";
import { render } from "@testing-library/react";
import { OrderedList, OrderedListNode } from "./OrderedList";
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

function textNode(text: string): Node
{
    return { target: [], type: "TEXT", texts: [{ DEFAULT: text }] } as unknown as Node;
}

function listNode(texts: string[], startNumber: number): OrderedListNode
{
    return { target: [], type: OrderedList.TYPE, elements: texts.map(textNode), startNumber: startNumber } as unknown as OrderedListNode;
}

test("an ORDEREDLIST node renders an ol with one li per entry, in order", () => {
    const { container } = render(<>{Renderer.render(listNode(["one", "two", "three"], 1))}</>);

    const lists = container.querySelectorAll("ol");
    expect(lists).toHaveLength(1);
    const items = lists[0].querySelectorAll(":scope > li");
    expect(Array.from(items).map(item => item.textContent)).toEqual(["one", "two", "three"]);
});

test("startNumber 3 renders start=3 on the ol", () => {
    const { container } = render(<OrderedList node={listNode(["a", "b"], 3)} />);

    expect(container.querySelector("ol")?.getAttribute("start")).toBe("3");
});

test("startNumber 1 renders no start attribute", () => {
    const { container } = render(<OrderedList node={listNode(["a", "b"], 1)} />);

    expect(container.querySelector("ol")?.hasAttribute("start")).toBe(false);
});

test("a composite entry holding a paragraph and a nested list renders both inside one li", () => {
    const nested = { target: [], type: "UNORDEREDLIST", enableBulletPoints: true, elements: [textNode("b")] } as unknown as Node;
    const composite = { target: [], type: "COMPOSITE", elements: [textNode("a"), nested] } as unknown as Node;
    const node = { target: [], type: OrderedList.TYPE, elements: [composite], startNumber: 1 } as unknown as OrderedListNode;

    const { container } = render(<OrderedList node={node} />);

    const items = container.querySelectorAll("ol > li");
    expect(items).toHaveLength(1);
    expect(items[0].querySelector("ul > li")?.textContent).toBe("b");
});
