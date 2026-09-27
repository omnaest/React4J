import React from "react";
import { render, screen } from "@testing-library/react";
import { Pagination, PaginationNode } from "./Pagination";

function createNode(entries: PaginationNode["entries"]): PaginationNode {
    return {
        target: [],
        type: Pagination.TYPE,
        entries: entries
    };
}

test("an item with an ariaLabel gets an aria-label attribute on the DOM element", () => {
    const node = createNode([
        { label: { DEFAULT: "1" }, active: false, disabled: false, ariaLabel: "Switch to BACKLOG view" }
    ]);

    render(<Pagination node={node} />);

    expect(screen.getByText("1")).toHaveAttribute("aria-label", "Switch to BACKLOG view");
});

test("an item without an ariaLabel has no aria-label attribute at all - not an empty one", () => {
    const node = createNode([
        { label: { DEFAULT: "1" }, active: false, disabled: false }
    ]);

    render(<Pagination node={node} />);

    expect(screen.getByText("1")).not.toHaveAttribute("aria-label");
});

test("an active item's ariaLabel is still wired through, even though it renders as a span rather than a link", () => {
    const node = createNode([
        { label: { DEFAULT: "1" }, active: true, disabled: false, ariaLabel: "Currently viewing BOARD view" }
    ]);

    render(<Pagination node={node} />);

    expect(screen.getByText("1")).toHaveAttribute("aria-label", "Currently viewing BOARD view");
});
