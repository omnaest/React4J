import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import { DropTarget, DropTargetNode } from "./DropTarget";
import { Draggable, DraggableNode } from "./Draggable";
import { RenderingSupportContext } from "../support/RenderingSupportContext";
import { NodeContextAccessor } from "../Renderer";
import { UIContext, UIContextAccessor, UIContextDataNode } from "../data/DataContextManager";
import { KeyboardDragState } from "./KeyboardDragState";
import { Backend } from "../../backend/Backend";

// Same reasoning as TreeTable.test.tsx: Renderer.tsx transitively imports Handler.ts -> Backend.ts's
// real axios ESM build, which CRA's default jest config cannot parse - an explicit factory mock avoids
// evaluating that import graph. Because resetMocks:true re-arms sendEvent before every test, no test
// needs a specific resolved value, only call assertions.
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

const mockedSendEvent = Backend.sendEvent as jest.MockedFunction<typeof Backend.sendEvent>;

beforeEach(() => {
    jest.clearAllMocks();
    KeyboardDragState.clear();
});

/**
 * Real in-memory UIContextAccessor -- NOT a mock -- so the write is genuinely observable through
 * getUIContextById("").data afterwards, mirroring TreeTable.test.tsx's createUIContextAccessor exactly.
 */
function createUIContextAccessor(): UIContextAccessor {
    const contexts: { [contextId: string]: UIContext } = {
        "": { contextId: "", data: {}, internalData: {}, updateCounter: 0 }
    };
    return {
        getUIContextById: (contextId: string) => contexts[contextId],
        getAllUIContexts: () => Object.values(contexts),
        updateUIContext: (uiContext: UIContext) => {
            contexts[uiContext.contextId] = uiContext;
        },
        initializeUIContext: (uiContext?: UIContextDataNode) => {
            if (uiContext) {
                contexts[uiContext.contextId] = { ...uiContext, updateCounter: 0 } as UIContext;
            }
        }
    };
}

function realDropTargetNode(overrides?: Partial<DropTargetNode>): DropTargetNode {
    return {
        type: DropTarget.TYPE,
        target: [],
        dragIdFieldKey: "droptarget.root.droptargetimpl.dragId",
        relationFieldKey: "droptarget.root.droptargetimpl.relation",
        dropTarget: ["root", "droptargetimpl"],
        content: { type: "PARAGRAPH", target: [], elements: [], bold: false } as any,
        ...overrides
    };
}

function dataTransferWithDragId(dragId: string) {
    return { getData: jest.fn(() => dragId), setData: jest.fn(), effectAllowed: "" };
}

/**
 * jsdom's synthetic "drop"/"dragover" events are plain Events, not real DragEvents -- fireEvent's
 * eventInit shape does not attach a working `dataTransfer` (or, for a plain Event, `clientY`) onto them,
 * so both are defined explicitly here via a hand-built Event, mirroring the AC-S1-5 dragover test's own
 * technique.
 */
function fireDrop(element: HTMLElement, dragId: string, clientY?: number): void {
    const dropEvent = new (window as any).Event("drop", { bubbles: true, cancelable: true });
    Object.defineProperty(dropEvent, "dataTransfer", { value: dataTransferWithDragId(dragId) });
    if (clientY !== undefined) {
        Object.defineProperty(dropEvent, "clientY", { value: clientY });
    }
    fireEvent(element, dropEvent);
}

// ---------------------------------------------------------------------------------------------
// AC-S1-5: dragover MUST call preventDefault() -- otherwise no drop ever fires (plan-235 S9.5c).
// ---------------------------------------------------------------------------------------------

test("dragover calls preventDefault (AC-S1-5)", () => {
    render(<DropTarget node={realDropTargetNode()} />);

    const element = screen.getByTestId("drop-target");
    const dragOverEvent = new (window as any).Event("dragover", { bubbles: true, cancelable: true });
    Object.defineProperty(dragOverEvent, "dataTransfer", { value: dataTransferWithDragId("") });
    const preventDefaultSpy = jest.spyOn(dragOverEvent, "preventDefault");
    fireEvent(element, dragOverEvent);

    expect(preventDefaultSpy).toHaveBeenCalledTimes(1);
});

// ---------------------------------------------------------------------------------------------
// AC-S1-4: a drop writes BOTH field keys into the "" context's data, THEN dispatches.
// ---------------------------------------------------------------------------------------------

test("a drop writes both dragIdFieldKey and relationFieldKey into the root context THEN dispatches dropTarget (AC-S1-4)", () => {
    const uiContextAccessor = createUIContextAccessor();
    const nodeContextAccessor = {} as NodeContextAccessor;
    jest.spyOn(uiContextAccessor, "updateUIContext");
    const callOrder: string[] = [];
    mockedSendEvent.mockImplementation(() => {
        callOrder.push("dispatch");
        return Promise.resolve() as any;
    });

    render(
        <RenderingSupportContext.Provider value={{ uiContextAccessor, nodeContextAccessor }}>
            <DropTarget node={realDropTargetNode()} />
        </RenderingSupportContext.Provider>
    );

    const element = screen.getByTestId("drop-target");
    // A zero-size jsdom bounding rect makes relationForPointer's offset(0) < third(0) false and
    // offset(0) > third*2(0) false, so it falls through to INTO -- exactly what this test needs, since
    // AC-S1-4 is about write-then-dispatch ordering, not relation fidelity (covered separately below).
    fireDrop(element, "card-7");

    expect(uiContextAccessor.getUIContextById("").data["droptarget.root.droptargetimpl.dragId"]).toBe("card-7");
    expect(uiContextAccessor.getUIContextById("").data["droptarget.root.droptargetimpl.relation"]).toBe("INTO");

    expect(mockedSendEvent).toHaveBeenCalledTimes(1);
    expect(mockedSendEvent).toHaveBeenCalledWith(["root", "droptargetimpl"], "", uiContextAccessor, nodeContextAccessor);

    const updateUIContextCallOrderIndex = (uiContextAccessor.updateUIContext as jest.Mock).mock.invocationCallOrder[0];
    const sendEventCallOrderIndex = mockedSendEvent.mock.invocationCallOrder[0];
    expect(updateUIContextCallOrderIndex).toBeLessThan(sendEventCallOrderIndex);
});

// ---------------------------------------------------------------------------------------------
// AC-S1-6: relation fidelity -- BEFORE/AFTER/INTO travel verbatim uppercase, computed from pointer
// position within the target's own bounds.
// ---------------------------------------------------------------------------------------------

function withStubbedRect(height: number, top: number, run: () => void) {
    const original = HTMLElement.prototype.getBoundingClientRect;
    HTMLElement.prototype.getBoundingClientRect = jest.fn(() => ({
        height, top, bottom: top + height, left: 0, right: 0, width: 0, x: 0, y: top, toJSON: () => ({})
    })) as any;
    try {
        run();
    } finally {
        HTMLElement.prototype.getBoundingClientRect = original;
    }
}

test.each([
    [10, "BEFORE"],
    [50, "INTO"],
    [90, "AFTER"]
])("a drop at clientY=%s (expected relation %s) writes that relation verbatim uppercase (AC-S1-6)", (clientY, expectedRelation) => {
    withStubbedRect(90, 0, () => {
        const uiContextAccessor = createUIContextAccessor();
        render(
            <RenderingSupportContext.Provider value={{ uiContextAccessor, nodeContextAccessor: {} as NodeContextAccessor }}>
                <DropTarget node={realDropTargetNode()} />
            </RenderingSupportContext.Provider>
        );

        fireDrop(screen.getByTestId("drop-target"), "card-1", Number(clientY));

        expect(uiContextAccessor.getUIContextById("").data["droptarget.root.droptargetimpl.relation"]).toBe(expectedRelation);
    });
});

// ---------------------------------------------------------------------------------------------
// AC-S1-3 (client half): a DropTarget with no server-side onDrop (null field keys / empty target) never
// writes nor dispatches.
// ---------------------------------------------------------------------------------------------

test("a DropTarget with null field keys and an empty dropTarget never writes nor dispatches on drop", () => {
    const uiContextAccessor = createUIContextAccessor();
    render(
        <RenderingSupportContext.Provider value={{ uiContextAccessor, nodeContextAccessor: {} as NodeContextAccessor }}>
            <DropTarget node={realDropTargetNode({ dragIdFieldKey: null, relationFieldKey: null, dropTarget: [] })} />
        </RenderingSupportContext.Provider>
    );

    fireDrop(screen.getByTestId("drop-target"), "card-1");

    expect(Object.keys(uiContextAccessor.getUIContextById("").data)).toHaveLength(0);
    expect(mockedSendEvent).not.toHaveBeenCalled();
});

test("an ungated DropTarget is not focusable and has no keyboard handler wired", () => {
    render(<DropTarget node={realDropTargetNode({ dragIdFieldKey: null, relationFieldKey: null, dropTarget: [] })} />);

    const element = screen.getByTestId("drop-target");
    expect(element).not.toHaveAttribute("tabIndex");
});

// ---------------------------------------------------------------------------------------------
// AC-S1-7 (keyboard, DropTarget half): Space/Enter drops whatever was picked up, always as INTO.
// ---------------------------------------------------------------------------------------------

test("Space/Enter on a focused DropTarget drops the KeyboardDragState-picked-up item as relation=INTO", () => {
    const uiContextAccessor = createUIContextAccessor();
    KeyboardDragState.setGrabbedDragId("card-3");

    render(
        <RenderingSupportContext.Provider value={{ uiContextAccessor, nodeContextAccessor: {} as NodeContextAccessor }}>
            <DropTarget node={realDropTargetNode()} />
        </RenderingSupportContext.Provider>
    );

    fireEvent.keyDown(screen.getByTestId("drop-target"), { key: "Enter" });

    expect(uiContextAccessor.getUIContextById("").data["droptarget.root.droptargetimpl.dragId"]).toBe("card-3");
    expect(uiContextAccessor.getUIContextById("").data["droptarget.root.droptargetimpl.relation"]).toBe("INTO");
    expect(mockedSendEvent).toHaveBeenCalledTimes(1);
    expect(KeyboardDragState.getGrabbedDragId()).toBeNull();
});

test("Space/Enter on a focused DropTarget with nothing picked up does nothing", () => {
    render(
        <RenderingSupportContext.Provider value={{ uiContextAccessor: createUIContextAccessor(), nodeContextAccessor: {} as NodeContextAccessor }}>
            <DropTarget node={realDropTargetNode()} />
        </RenderingSupportContext.Provider>
    );

    fireEvent.keyDown(screen.getByTestId("drop-target"), { key: "Enter" });

    expect(mockedSendEvent).not.toHaveBeenCalled();
});

// ---------------------------------------------------------------------------------------------
// plan-235 S1 corrective round 2 (Defects A/B): propagation contract (AC-R5) plus the nested
// DropTarget(Draggable(content)) integration proof of AC-R1's falsifiable prediction and AC-R3's
// zero-round-trip-at-pick-up requirement.
// ---------------------------------------------------------------------------------------------

test("a drop stops propagation when the target is gated and genuinely consumed the event (AC-R5)", () => {
    render(
        <RenderingSupportContext.Provider
            value={{ uiContextAccessor: createUIContextAccessor(), nodeContextAccessor: {} as NodeContextAccessor }}
        >
            <DropTarget node={realDropTargetNode()} />
        </RenderingSupportContext.Provider>
    );

    const element = screen.getByTestId("drop-target");
    const dropEvent = new (window as any).Event("drop", { bubbles: true, cancelable: true });
    Object.defineProperty(dropEvent, "dataTransfer", { value: dataTransferWithDragId("card-1") });
    const stopPropagationSpy = jest.spyOn(dropEvent, "stopPropagation");
    fireEvent(element, dropEvent);

    expect(stopPropagationSpy).toHaveBeenCalledTimes(1);
});

test("a drop does NOT stop propagation when the DropTarget has no registered handler (AC-R5)", () => {
    render(
        <DropTarget node={realDropTargetNode({ dragIdFieldKey: null, relationFieldKey: null, dropTarget: [] })} />
    );

    const element = screen.getByTestId("drop-target");
    const dropEvent = new (window as any).Event("drop", { bubbles: true, cancelable: true });
    Object.defineProperty(dropEvent, "dataTransfer", { value: dataTransferWithDragId("card-1") });
    const stopPropagationSpy = jest.spyOn(dropEvent, "stopPropagation");
    fireEvent(element, dropEvent);

    expect(stopPropagationSpy).not.toHaveBeenCalled();
});

test("Space/Enter on a gated, grabbed DropTarget stops propagation once it genuinely consumes the drop (AC-R5)", () => {
    KeyboardDragState.setGrabbedDragId("card-3");
    render(
        <RenderingSupportContext.Provider
            value={{ uiContextAccessor: createUIContextAccessor(), nodeContextAccessor: {} as NodeContextAccessor }}
        >
            <DropTarget node={realDropTargetNode()} />
        </RenderingSupportContext.Provider>
    );

    const event = new KeyboardEvent("keydown", { key: "Enter", bubbles: true, cancelable: true });
    const stopPropagationSpy = jest.spyOn(event, "stopPropagation");
    screen.getByTestId("drop-target").dispatchEvent(event);

    expect(stopPropagationSpy).toHaveBeenCalledTimes(1);
});

test("Space/Enter on a DropTarget with nothing picked up does NOT stop propagation (AC-R5)", () => {
    render(
        <RenderingSupportContext.Provider
            value={{ uiContextAccessor: createUIContextAccessor(), nodeContextAccessor: {} as NodeContextAccessor }}
        >
            <DropTarget node={realDropTargetNode()} />
        </RenderingSupportContext.Provider>
    );

    const event = new KeyboardEvent("keydown", { key: "Enter", bubbles: true, cancelable: true });
    const stopPropagationSpy = jest.spyOn(event, "stopPropagation");
    screen.getByTestId("drop-target").dispatchEvent(event);

    expect(stopPropagationSpy).not.toHaveBeenCalled();
});

/**
 * The real, as-deployed composition (ComponentShowcaseUI.renderDragDropCard): every demo card is a
 * DropTarget wrapping a Draggable. Builds the DropTarget's `content` as an actual DRAGGABLE node so
 * Renderer.render dispatches to a real nested `<Draggable>` inside the `<DropTarget>` - not a fake React
 * child - reproducing the exact DOM nesting the browser IT drives.
 */
function nestedDropTargetWrappingDraggable(dragId: string, overrides?: Partial<DropTargetNode>): DropTargetNode {
    const draggableNode: DraggableNode = {
        type: Draggable.TYPE,
        target: [],
        dragId,
        content: { type: "PARAGRAPH", target: [], elements: [], bold: false } as any
    };
    return realDropTargetNode({ content: draggableNode as any, ...overrides });
}

/**
 * AC-R1's falsifiable prediction, proven directly: picking up a Draggable nested inside its OWN
 * DropTarget must not itself fire a POST /ui/event (a self-drop). Before the fix (Draggable.handleKeyDown
 * omitting stopPropagation), the pick-up keydown bubbles into the enclosing DropTarget's handleKeyDown,
 * which reads the JUST-SET KeyboardDragState synchronously and self-drops - this test is RED against the
 * unfixed source (Backend.sendEvent IS called once) and GREEN once Draggable.tsx stops propagation on a
 * handled key. Also discharges AC-R3 (zero round trips at pick-up) at the unit level.
 */
test("picking up a Draggable nested inside its own DropTarget fires ZERO /ui/event round trips (AC-R1/AC-R3)", () => {
    render(
        <RenderingSupportContext.Provider
            value={{ uiContextAccessor: createUIContextAccessor(), nodeContextAccessor: {} as NodeContextAccessor }}
        >
            <DropTarget node={nestedDropTargetWrappingDraggable("card-k1")} />
        </RenderingSupportContext.Provider>
    );

    const draggableElement = screen.getByTestId("draggable");
    expect(draggableElement).toHaveAttribute("aria-grabbed", "false");

    fireEvent.keyDown(draggableElement, { key: "Enter" });

    expect(draggableElement).toHaveAttribute("aria-grabbed", "true");
    expect(KeyboardDragState.getGrabbedDragId()).toBe("card-k1");
    expect(mockedSendEvent).not.toHaveBeenCalled();
});
