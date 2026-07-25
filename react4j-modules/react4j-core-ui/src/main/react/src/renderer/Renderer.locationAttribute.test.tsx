// Renderer.tsx transitively imports Button.tsx -> Handler.ts -> the real Backend.ts, which
// imports real axios. Mock axios itself via an explicit factory (never evaluates the real,
// ESM-only axios build) so the rest of the real Renderer/component tree loads unmocked under
// Jest -- see react4j-core-ui-jest-use-npm-test-and-run-build-for-tsc.
jest.mock("axios", () => ({
    __esModule: true,
    default: {
        post: jest.fn(),
        get: jest.fn()
    }
}));

import { render, screen } from "@testing-library/react";
import { Renderer, Node } from "./Renderer";
import { HeadingNode } from "./components/Heading";
import { ContainerNode } from "./components/Container";

/**
 * C1 (plan-74 Goal 3a): the shared Renderer.renderNode dispatch point must systematically stamp
 * a `data-location` DOM attribute -- node.target.join(".") -- onto the real rendered host
 * element of every node, WITHOUT introducing a wrapper DOM node (no layout/CSS drift) and
 * WITHOUT requiring per-component edits, even for a leaf node that itself renders a plain native
 * tag (Heading -> <h3>) and a node whose own component nests further Renderer.render calls
 * (Container -> <div> with child rows).
 */

test("a rendered leaf node exposes the expected data-location selector derived from node.target", async () => {
    const node: HeadingNode = {
        target: ["page", "form", "submitButton"],
        type: "HEADING",
        level: 3,
        text: { DEFAULT: "Hi" }
    };

    render(Renderer.render(node as unknown as Node));

    const heading = await screen.findByText("Hi");

    expect(heading.tagName.toLowerCase()).toBe("h3");
    expect(heading).toHaveAttribute("data-location", "page.form.submitButton");
});

test("data-location is queryable via document.querySelector using the exact target-joined selector", async () => {
    const node: HeadingNode = {
        target: ["page", "heading"],
        type: "HEADING",
        level: 1,
        text: { DEFAULT: "Title" }
    };

    const { container } = render(Renderer.render(node as unknown as Node));

    const located = container.querySelector('[data-location="page.heading"]');
    expect(located).not.toBeNull();
    expect(located?.textContent).toBe("Title");
});

test("a container node's own root element carries its own data-location, independent of its nested children", async () => {
    const node: ContainerNode = {
        target: ["page", "mainContainer"],
        type: "CONTAINER",
        locator: "",
        unlimitedColumns: false,
        rows: []
    };

    const { container } = render(Renderer.render(node as unknown as Node));

    const located = container.querySelector('[data-location="page.mainContainer"]');
    expect(located).not.toBeNull();
    expect(located?.className).toContain("container-fluid");
});
