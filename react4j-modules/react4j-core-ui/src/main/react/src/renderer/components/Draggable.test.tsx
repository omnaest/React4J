import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import { Draggable, DraggableNode } from "./Draggable";
import { KeyboardDragState } from "./KeyboardDragState";

function realDraggableNode(overrides?: Partial<DraggableNode>): DraggableNode {
    return {
        type: Draggable.TYPE,
        target: [],
        dragId: "card-1",
        content: { type: "PARAGRAPH", target: [], elements: [], bold: false } as any,
        ...overrides
    };
}

beforeEach(() => {
    KeyboardDragState.clear();
});

test("renders the wrapped content with draggable=true and the dragId carried as a data attribute", () => {
    render(<Draggable node={realDraggableNode()} />);

    const element = screen.getByTestId("draggable");
    expect(element).toHaveAttribute("draggable", "true");
    expect(element).toHaveAttribute("data-drag-id", "card-1");
});

test("is keyboard-focusable and carries an aria-label describing the pick-up action (AC-S1-7)", () => {
    render(<Draggable node={realDraggableNode()} />);

    const element = screen.getByTestId("draggable");
    expect(element).toHaveAttribute("tabIndex", "0");
    expect(element.getAttribute("aria-label")).toMatch(/card-1/);
});

test("dragstart writes the node's dragId onto the native DataTransfer payload as text/plain", () => {
    render(<Draggable node={realDraggableNode({ dragId: "card-99" })} />);

    const element = screen.getByTestId("draggable");
    const setData = jest.fn();
    // jsdom's synthetic "dragstart" is a plain Event, not a real DragEvent -- fireEvent's eventInit shape
    // does not attach a working dataTransfer onto it, so it is defined explicitly here (mirrors the
    // dragover test's own technique below in DropTarget.test.tsx).
    const dragStartEvent = new (window as any).Event("dragstart", { bubbles: true, cancelable: true });
    Object.defineProperty(dragStartEvent, "dataTransfer", { value: { setData, effectAllowed: "" } });
    fireEvent(element, dragStartEvent);

    expect(setData).toHaveBeenCalledWith("text/plain", "card-99");
});

test("Space/Enter toggles aria-grabbed and records the picked-up dragId in KeyboardDragState (AC-S1-7)", () => {
    render(<Draggable node={realDraggableNode({ dragId: "card-5" })} />);

    const element = screen.getByTestId("draggable");
    expect(element).toHaveAttribute("aria-grabbed", "false");
    expect(KeyboardDragState.getGrabbedDragId()).toBeNull();

    fireEvent.keyDown(element, { key: "Enter" });

    expect(element).toHaveAttribute("aria-grabbed", "true");
    expect(KeyboardDragState.getGrabbedDragId()).toBe("card-5");

    // pressing again puts it back down
    fireEvent.keyDown(element, { key: " " });

    expect(element).toHaveAttribute("aria-grabbed", "false");
    expect(KeyboardDragState.getGrabbedDragId()).toBeNull();
});

test("an unrelated key does not toggle grabbed state", () => {
    render(<Draggable node={realDraggableNode()} />);

    fireEvent.keyDown(screen.getByTestId("draggable"), { key: "Tab" });

    expect(KeyboardDragState.getGrabbedDragId()).toBeNull();
});

// ---------------------------------------------------------------------------------------------
// plan-235 S1 corrective round 2 (Defect A): a handled pick-up/put-down keydown must stop
// propagation so it cannot reach an enclosing DropTarget (AC-R5); an unhandled key must not.
// ---------------------------------------------------------------------------------------------

test("Space/Enter (a handled key) stops propagation (AC-R5)", () => {
    render(<Draggable node={realDraggableNode()} />);

    const event = new KeyboardEvent("keydown", { key: "Enter", bubbles: true, cancelable: true });
    const stopPropagationSpy = jest.spyOn(event, "stopPropagation");
    screen.getByTestId("draggable").dispatchEvent(event);

    expect(stopPropagationSpy).toHaveBeenCalledTimes(1);
});

test("an unhandled key does NOT stop propagation (AC-R5)", () => {
    render(<Draggable node={realDraggableNode()} />);

    const event = new KeyboardEvent("keydown", { key: "Tab", bubbles: true, cancelable: true });
    const stopPropagationSpy = jest.spyOn(event, "stopPropagation");
    screen.getByTestId("draggable").dispatchEvent(event);

    expect(stopPropagationSpy).not.toHaveBeenCalled();
});
