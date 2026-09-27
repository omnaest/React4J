import React from "react";
import { render } from "@testing-library/react";
import { Modal, ModalNode } from "./Modal";
import { Text } from "./Text";

function createNode(overrides: Partial<ModalNode> = {}): ModalNode {
    return {
        target: [],
        type: Modal.TYPE,
        title: { DEFAULT: "Title" },
        content: { target: [], type: Text.TYPE, texts: [{ DEFAULT: "Body" }] } as any,
        visible: true,
        centered: true,
        fullscreen: false,
        ...overrides
    };
}

// react-bootstrap's Modal portals its dialog directly onto document.body (not into RTL's own render
// container), so every assertion below queries document.body rather than the `container` `render()`
// returns.
function getDialog(): HTMLElement | null {
    return document.body.querySelector(".modal-dialog");
}

describe("Modal", () => {
    test("fullscreen: true carries react-bootstrap's fullscreen dialog class", () => {
        const node = createNode({ fullscreen: true });
        render(<Modal node={node} />);

        const dialog = getDialog();
        expect(dialog).not.toBeNull();
        // Read from the pinned react-bootstrap 2.10.1 source (ModalDialog.js): for `fullscreen === true`
        // (not a string breakpoint variant), the emitted class is exactly `${bsPrefix}-fullscreen`, i.e.
        // "modal-fullscreen" - plan-261 C3-PROV: this is the class actually emitted, not an assumption.
        expect((dialog as HTMLElement).className.split(" ")).toContain("modal-fullscreen");
    });

    test("fullscreen absent does not carry the fullscreen dialog class, and markup is otherwise unchanged", () => {
        const node = createNode();
        render(<Modal node={node} />);

        const dialog = getDialog();
        expect(dialog).not.toBeNull();
        const classes = (dialog as HTMLElement).className.split(" ");
        expect(classes).not.toContain("modal-fullscreen");
        expect(classes).toEqual(expect.arrayContaining(["modal-dialog", "modal-dialog-centered"]));
    });

    test("fullscreen: false (explicit) does not carry the fullscreen dialog class either", () => {
        const node = createNode({ fullscreen: false });
        render(<Modal node={node} />);

        const dialog = getDialog();
        expect((dialog as HTMLElement).className.split(" ")).not.toContain("modal-fullscreen");
    });

    test("size is still forwarded (test discriminator KanbanBoardServer relies on)", () => {
        const node = createNode({ size: "xl", fullscreen: true });
        render(<Modal node={node} />);

        const dialog = getDialog();
        const classes = (dialog as HTMLElement).className.split(" ");
        expect(classes).toContain("modal-xl");
        expect(classes).toContain("modal-fullscreen");
    });
});
