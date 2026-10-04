import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import { NavigationBar, NavigationBarEntry, NavigationBarNode } from "./NavigationBar";

// NavigationBar imports nothing that reaches Backend/axios, so no module mock is needed here (compare Breadcrumb.test.tsx).

function createNode(entries: NavigationBarEntry[]): NavigationBarNode {
    return {
        target: [],
        type: NavigationBar.TYPE,
        entries: entries
    };
}

function text(value: string) {
    return { DEFAULT: value };
}

test("a plain entry stays an anchor in a list item: href from linkedId, _self, active and disabled classes", () => {
    render(<NavigationBar node={createNode([
        { text: text("First"), linkedId: "target-a", active: true },
        { text: text("Second"), linkedId: "target-b", disabled: true }
    ])} />);

    const first = screen.getByText("First");
    expect(first.tagName).toBe("A");
    expect(first.parentElement?.tagName).toBe("LI");
    expect(first.parentElement).toHaveClass("nav-item");
    expect(first).toHaveClass("nav-link", "active");
    expect(first).toHaveAttribute("href", "#target-a");
    expect(first).toHaveAttribute("target", "_self");
    expect(screen.getByText("Second")).toHaveClass("nav-link", "disabled");
    expect(document.querySelector("#navbarContent ul.nav.nav-pills.page-navigation")).not.toBeNull();
    expect(document.querySelector(".dropdown-toggle")).toBeNull();
});

test("a plain entry with a link opens it in a new tab and an explicit null dropdownEntries is still a plain entry", () => {
    render(<NavigationBar node={createNode([
        { text: text("External"), link: "https://example.org/", dropdownEntries: null }
    ])} />);

    const anchor = screen.getByText("External");
    expect(anchor.tagName).toBe("A");
    expect(anchor).toHaveAttribute("href", "https://example.org/");
    expect(anchor).toHaveAttribute("target", "_blank");
    expect(document.querySelector(".dropdown-toggle")).toBeNull();
});

test("a dropdown renders a toggle inside the list and its items, with the hrefs of plain entries, once opened", () => {
    render(<NavigationBar node={createNode([
        { text: text("Plain"), linkedId: "plain" },
        {
            text: text("Quick links"),
            dropdownEntries: [
                { text: text("Go to A"), linkedId: "target-a" },
                { text: text("Docs"), link: "https://example.org/docs" },
                { text: text("Archived"), linkedId: "old", disabled: true, active: true }
            ]
        }
    ])} />);

    const toggle = screen.getByText("Quick links");
    expect(toggle.tagName).toBe("BUTTON");
    expect(toggle).toHaveClass("nav-link", "dropdown-toggle");
    expect(toggle.closest("ul.page-navigation > li.nav-item")).not.toBeNull();
    expect(screen.getByText("Plain").tagName).toBe("A");

    fireEvent.click(toggle);

    const menu = document.querySelector(".dropdown-menu.show") as HTMLElement;
    expect(menu).not.toBeNull();
    expect(toggle.closest("li")?.contains(menu)).toBe(true);
    const items = Array.from(menu.querySelectorAll("a.dropdown-item"));
    expect(items.map(item => item.textContent)).toEqual(["Go to A", "Docs", "Archived"]);
    expect(items[0]).toHaveAttribute("href", "#target-a");
    expect(items[0]).toHaveAttribute("target", "_self");
    expect(items[1]).toHaveAttribute("href", "https://example.org/docs");
    expect(items[1]).toHaveAttribute("target", "_blank");
    expect(items[2]).toHaveClass("disabled", "active");
});

test("an empty dropdown renders its toggle and an open menu without items", () => {
    render(<NavigationBar node={createNode([
        { text: text("Nothing yet"), dropdownEntries: [] }
    ])} />);

    const toggle = screen.getByText("Nothing yet");
    expect(toggle).toHaveClass("dropdown-toggle");
    fireEvent.click(toggle);

    const menu = document.querySelector(".dropdown-menu.show") as HTMLElement;
    expect(menu).not.toBeNull();
    expect(menu.querySelectorAll(".dropdown-item").length).toBe(0);
});

test("a disabled dropdown toggle cannot open its menu and is marked disabled", () => {
    render(<NavigationBar node={createNode([
        { text: text("Locked"), disabled: true, dropdownEntries: [{ text: text("Hidden"), linkedId: "x" }] }
    ])} />);

    const toggle = screen.getByText("Locked");
    expect(toggle).toBeDisabled();
    expect(toggle).toHaveClass("disabled");
    fireEvent.click(toggle);
    expect(document.querySelector(".dropdown-menu.show")).toBeNull();
});
