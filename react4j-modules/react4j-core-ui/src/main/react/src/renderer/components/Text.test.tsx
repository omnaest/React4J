import React from "react";
import { render } from "@testing-library/react";
import { Text, TextNode } from "./Text";

function node(texts: string[], style?: string | null): TextNode
{
    return { target: [], type: Text.TYPE, texts: texts.map(text => ({ DEFAULT: text })), style: style } as unknown as TextNode;
}

test("a TEXT node without style renders no element of its own: the container holds exactly the text nodes", () => {
    const { container } = render(<Text node={node(["alpha", "beta"])} />);

    const children = Array.from(container.childNodes);
    expect(children.length).toBeGreaterThan(0);
    children.forEach(child => expect(child.nodeType).toBe(Node.TEXT_NODE));
    expect(container.textContent).toBe("alphabeta");
});

test("a null style behaves like no style", () => {
    const { container } = render(<Text node={node(["alpha"], null)} />);

    expect(container.querySelector("*")).toBeNull();
});

test("an unknown style value falls back to the unstyled fragment", () => {
    const { container } = render(<Text node={node(["alpha"], "SOMETHING_NEW")} />);

    expect(container.querySelector("*")).toBeNull();
    expect(container.textContent).toBe("alpha");
});

test("style MUTED renders exactly one span with class text-body-secondary containing the texts", () => {
    const { container } = render(<Text node={node(["alpha", "beta"], "MUTED")} />);

    expect(container.childNodes).toHaveLength(1);
    const span = container.firstChild as HTMLElement;
    expect(span.tagName).toBe("SPAN");
    expect(span.className).toBe("text-body-secondary");
    expect(span.textContent).toBe("alphabeta");
});
