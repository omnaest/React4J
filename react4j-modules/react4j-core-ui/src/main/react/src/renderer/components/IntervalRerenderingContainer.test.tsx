import React from "react";
import { render, fireEvent, screen, cleanup, act } from "@testing-library/react";
import { Provider } from "react-redux";
import { createStore } from "redux";
import { rootReducer } from "../../reducer/Reducer";
import IntervalRerenderingContainer, { IntervalRerenderingContainerNode } from "./IntervalRerenderingContainer";
import { Button, ButtonNode } from "./Button";
import { CompositeNode } from "./Composite";
import { ModalNode } from "./Modal";
import { ServerHandler } from "../handler/Handler";
import { TextNode } from "./Text";
import { Backend } from "../../backend/Backend";

// Same rationale as RerenderingContainer.test.tsx: explicit factory mock, no automock (axios ESM
// build is not transformable by CRA's default jest config).
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
const mockedGetUISubNode = Backend.getUISubNode as jest.MockedFunction<typeof Backend.getUISubNode>;

// Real captured-payload shape: positional component{i} segments in the target array.
const CONTAINER_TARGET = ["compositeimpl", "component9", "cardimpl", "intervalrerenderingcontainerimpl"];

// See react4j-core-ui-jest-createstore-shared-reducer-init-singleton: createStore(rootReducer) with NO
// preloadedState shares Reducer.ts's module-level `init.nodes` object across every store created that
// way in this process, leaking one test's dispatched update into the next test's "fresh" store. Always
// pass an explicit, distinct preloadedState.
function newStore() {
    return createStore(rootReducer, { uiContexts: {}, nodes: {} });
}

beforeEach(() => {
    jest.clearAllMocks();
});

afterEach(() => {
    cleanup();
    jest.useRealTimers();
});

test("AC-S2-1: clicking a server-driven Button inside a real Redux IntervalRerenderingContainer applies the server patch and opens a Modal", async () => {
    const buttonTarget = [...CONTAINER_TARGET, "content", "button"];
    const onClickHandler: ServerHandler = {
        type: "SERVER",
        target: [...buttonTarget, "onclick"],
        contextId: "ctx-interval-modal-trigger"
    };
    const buttonNode: ButtonNode = {
        target: buttonTarget,
        type: Button.TYPE,
        name: { DEFAULT: "Open modal" },
        style: "primary",
        onClick: onClickHandler
    };
    const initialContent: CompositeNode = {
        target: [...CONTAINER_TARGET, "content"],
        type: "COMPOSITE",
        elements: [buttonNode]
    };
    // A long interval duration so the polling timer never fires during this (real-timer) test.
    const containerNode: IntervalRerenderingContainerNode = {
        target: CONTAINER_TARGET,
        type: "INTERVALRERENDERINGCONTAINER",
        content: initialContent,
        intervalDuration: 999999,
        active: true
    };

    const modalContentNode: TextNode = {
        target: [...CONTAINER_TARGET, "content", "modal", "text"],
        type: "TEXT",
        texts: [{ DEFAULT: "Modal body" }]
    };
    const modalNode: ModalNode = {
        target: [...CONTAINER_TARGET, "content", "modal"],
        type: "MODAL",
        title: { DEFAULT: "My Modal" },
        content: modalContentNode,
        visible: true,
        centered: false,
        fullscreen: false
    };
    const updatedContent: CompositeNode = {
        target: [...CONTAINER_TARGET, "content"],
        type: "COMPOSITE",
        elements: [buttonNode, modalNode]
    };
    // The server's response targetNode.node: the IntervalRerenderingContainer's OWN node, with the SAME
    // target (so the Redux store lookup keyed by target.join(".") matches and the connected container
    // re-renders) -- exactly the RerenderingContainer precedent.
    const updatedContainerNode: IntervalRerenderingContainerNode = {
        target: CONTAINER_TARGET,
        type: "INTERVALRERENDERINGCONTAINER",
        content: updatedContent,
        intervalDuration: 999999,
        active: true
    };

    // Replicates Backend.sendEvent's real .then() behaviour: it calls nodeContextAccessor?.updateNode(targetNode.node).
    mockedSendEvent.mockImplementation((_target, _contextId, _uiContextAccessor, nodeContextAccessor) => {
        nodeContextAccessor?.updateNode(updatedContainerNode as any);
        return Promise.resolve() as any;
    });

    const store = newStore();
    render(
        <Provider store={store}>
            <IntervalRerenderingContainer node={containerNode} />
        </Provider>
    );

    const button = await screen.findByText("Open modal");

    // Sanity: the modal must not be present before the click.
    expect(screen.queryByText("My Modal")).toBeNull();

    fireEvent.click(button);

    expect(mockedSendEvent).toHaveBeenCalledTimes(1);

    // The decisive DOM-level assertion: the patch was actually APPLIED, the Modal now renders.
    // THIS is Cause A -- before the fix, the response is written into Redux under a key nothing reads,
    // and this assertion times out (console logs "Invalid node undefined").
    await screen.findByText("My Modal");
});

test("AC-S2-2: the container establishes RenderingSupportContext and supplies it to a clicked child's handler (independent of AC-S2-1)", async () => {
    const buttonTarget = [...CONTAINER_TARGET, "content", "button"];
    const onClickHandler: ServerHandler = {
        type: "SERVER",
        target: [...buttonTarget, "onclick"],
        contextId: "ctx-interval-context-probe"
    };
    const buttonNode: ButtonNode = {
        target: buttonTarget,
        type: Button.TYPE,
        name: { DEFAULT: "Probe" },
        style: "primary",
        onClick: onClickHandler
    };
    const initialContent: CompositeNode = {
        target: [...CONTAINER_TARGET, "content"],
        type: "COMPOSITE",
        elements: [buttonNode]
    };
    const containerNode: IntervalRerenderingContainerNode = {
        target: CONTAINER_TARGET,
        type: "INTERVALRERENDERINGCONTAINER",
        content: initialContent,
        intervalDuration: 999999,
        active: true
    };

    mockedSendEvent.mockResolvedValue(undefined as any);

    const store = newStore();
    render(
        <Provider store={store}>
            <IntervalRerenderingContainer node={containerNode} />
        </Provider>
    );

    const button = await screen.findByText("Probe");
    fireEvent.click(button);

    expect(mockedSendEvent).toHaveBeenCalledTimes(1);
    // This is Cause B, checked independently of Cause A / AC-S2-1's DOM assertion: without a
    // RenderingSupportContext.Provider supplying renderingSupport, Button.tsx's this.context resolves
    // to undefined and Backend.sendEvent is invoked with undefined,undefined for the last two args --
    // exactly what RerenderingContainer.test.tsx's "no provider" case pins. Here we assert the OPPOSITE:
    // both accessor args must be defined, proving the Provider/renderingSupport wiring exists.
    const call = mockedSendEvent.mock.calls[0];
    expect(call[2]).toBeDefined();
    expect(call[3]).toBeDefined();
});

test("AC-S2-3: the interval tick dispatches updateNodeAction into the store rather than local setState", async () => {
    jest.useFakeTimers();

    const initialContent: TextNode = {
        target: [...CONTAINER_TARGET, "content"],
        type: "TEXT",
        texts: [{ DEFAULT: "initial" }]
    };
    const containerNode: IntervalRerenderingContainerNode = {
        target: CONTAINER_TARGET,
        type: "INTERVALRERENDERINGCONTAINER",
        content: initialContent,
        intervalDuration: 2000,
        active: true
    };

    const polledContent: TextNode = {
        target: [...CONTAINER_TARGET, "content"],
        type: "TEXT",
        texts: [{ DEFAULT: "polled" }]
    };
    const fetchedNode: IntervalRerenderingContainerNode = {
        target: CONTAINER_TARGET,
        type: "INTERVALRERENDERINGCONTAINER",
        content: polledContent,
        intervalDuration: 2000,
        active: true
    };
    mockedGetUISubNode.mockResolvedValue(fetchedNode as any);

    const store = newStore();
    render(
        <Provider store={store}>
            <IntervalRerenderingContainer node={containerNode} />
        </Provider>
    );

    const key = CONTAINER_TARGET.join(".");
    // Before the tick, nothing has been dispatched into the store under this key.
    expect(store.getState().nodes[key]).toBeUndefined();

    await act(async () => {
        jest.advanceTimersByTime(containerNode.intervalDuration);
        // Flush the microtask queue so Backend.getUISubNode's resolved promise .then() (the dispatch)
        // has run before we assert.
        await Promise.resolve();
        await Promise.resolve();
    });

    expect(mockedGetUISubNode).toHaveBeenCalledWith(CONTAINER_TARGET);
    // The decisive assertion for AC-S2-3: the fetched node landed in the STORE under the target's key --
    // i.e. it was dispatched via updateNodeAction, not merely assigned to local component state.
    expect(store.getState().nodes[key]).toEqual(fetchedNode);
});
