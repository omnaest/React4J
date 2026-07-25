import React from "react";
import { render, fireEvent, screen, cleanup } from "@testing-library/react";
import { Provider } from "react-redux";
import { createStore } from "redux";
import { rootReducer } from "../../reducer/Reducer";
import RerenderingContainer, { RerenderingContainerNode } from "./RerenderingContainer";
import { Button, ButtonNode } from "./Button";
import { CompositeNode } from "./Composite";
import { ModalNode } from "./Modal";
import { ToggleButtonNode } from "./ToggleButton";
import { ServerHandler } from "../handler/Handler";
import { Node } from "../Renderer";
import { TextNode } from "./Text";
import { Backend } from "../../backend/Backend";

// Explicit factory mock (no automock): automocking would require Jest to load the real Backend module,
// which transitively pulls in axios's ESM-only build; CRA's default transformIgnorePatterns does not
// transform node_modules, so that path fails with "Cannot use import statement outside a module".
// A factory mock never evaluates the real module, so it sidesteps that pre-existing jest-config gap.
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

// Real captured-payload shape: positional component{i} segments in the target array.
const CONTAINER_TARGET = ["compositeimpl", "component18", "cardimpl", "rerenderingcontainerimpl"];

// IMPORTANT: pass an explicit, distinct preloadedState per store. Reducer.ts's UPDATE_NODE case does
// `const newState = { ...state }; newState.nodes[key] = node;` -- a shallow copy of the top-level state
// that still MUTATES the nested `nodes` object in place. createStore(rootReducer) with NO preloaded
// state falls back to the reducer's module-level singleton `init` object as the initial state, so every
// store created that way in the same test run/process shares the SAME underlying `nodes` object --
// one test's dispatched update leaks into the next test's "fresh" store. A real app is unaffected (only
// one app-wide store is ever created via StoreFactory.ts), so this is a test-isolation concern only,
// not a production bug to fix here.
function newStore() {
    return createStore(rootReducer, { uiContexts: {}, nodes: {} });
}

beforeEach(() => {
    jest.clearAllMocks();
});

afterEach(() => {
    cleanup();
});

test("clicking a server-driven Button inside a real Redux RerenderingContainer applies the server patch and opens a Modal", async () => {
    const buttonTarget = [...CONTAINER_TARGET, "content", "button"];
    const onClickHandler: ServerHandler = {
        type: "SERVER",
        target: [...buttonTarget, "onclick"],
        contextId: "ctx-modal-trigger"
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
    const containerNode: RerenderingContainerNode = {
        target: CONTAINER_TARGET,
        type: "RERENDERINGCONTAINER",
        content: initialContent,
        enableNodeReload: false
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
        centered: false
    };
    const updatedContent: CompositeNode = {
        target: [...CONTAINER_TARGET, "content"],
        type: "COMPOSITE",
        elements: [buttonNode, modalNode]
    };
    // The server's response targetNode.node: the RerenderingContainer's OWN node, with the SAME target
    // (so the Redux store lookup keyed by target.join(".") matches and the connected container re-renders).
    const updatedContainerNode: RerenderingContainerNode = {
        target: CONTAINER_TARGET,
        type: "RERENDERINGCONTAINER",
        content: updatedContent,
        enableNodeReload: false
    };

    // Replicates Backend.sendEvent's real .then() behaviour: it calls nodeContextAccessor?.updateNode(targetNode.node).
    mockedSendEvent.mockImplementation((_target, _contextId, _uiContextAccessor, nodeContextAccessor) => {
        nodeContextAccessor?.updateNode(updatedContainerNode as unknown as Node);
        return Promise.resolve() as any;
    });

    const store = newStore();
    render(
        <Provider store={store}>
            <RerenderingContainer node={containerNode} />
        </Provider>
    );

    const button = await screen.findByText("Open modal");

    // Sanity: the modal must not be present before the click.
    expect(screen.queryByText("My Modal")).toBeNull();

    fireEvent.click(button);

    expect(mockedSendEvent).toHaveBeenCalledTimes(1);
    expect(mockedSendEvent).toHaveBeenCalledWith(
        onClickHandler.target,
        onClickHandler.contextId,
        expect.anything(),
        expect.anything()
    );

    // The decisive DOM-level assertion: the patch was actually APPLIED, the Modal now renders.
    await screen.findByText("My Modal");
});

test("clicking a server-driven Button inside a real Redux RerenderingContainer toggles a sibling ToggleButton's checked state", async () => {
    const buttonTarget = [...CONTAINER_TARGET, "content", "button"];
    const toggleTarget = [...CONTAINER_TARGET, "content", "toggle"];
    const onClickHandler: ServerHandler = {
        type: "SERVER",
        target: [...buttonTarget, "onclick"],
        contextId: "ctx-toggle-trigger"
    };
    const buttonNode: ButtonNode = {
        target: buttonTarget,
        type: Button.TYPE,
        name: { DEFAULT: "Press me" },
        style: "primary",
        onClick: onClickHandler
    };
    const toggleNodeInitial: ToggleButtonNode = {
        target: toggleTarget,
        type: "TOGGLEBUTTON",
        text: { DEFAULT: "Enabled" },
        style: "primary",
        pressed: false
    };
    const initialContent: CompositeNode = {
        target: [...CONTAINER_TARGET, "content"],
        type: "COMPOSITE",
        elements: [buttonNode, toggleNodeInitial]
    };
    const containerNode: RerenderingContainerNode = {
        target: CONTAINER_TARGET,
        type: "RERENDERINGCONTAINER",
        content: initialContent,
        enableNodeReload: false
    };

    const toggleNodeUpdated: ToggleButtonNode = {
        ...toggleNodeInitial,
        pressed: true
    };
    const updatedContent: CompositeNode = {
        target: [...CONTAINER_TARGET, "content"],
        type: "COMPOSITE",
        elements: [buttonNode, toggleNodeUpdated]
    };
    const updatedContainerNode: RerenderingContainerNode = {
        target: CONTAINER_TARGET,
        type: "RERENDERINGCONTAINER",
        content: updatedContent,
        enableNodeReload: false
    };

    mockedSendEvent.mockImplementation((_target, _contextId, _uiContextAccessor, nodeContextAccessor) => {
        nodeContextAccessor?.updateNode(updatedContainerNode as unknown as Node);
        return Promise.resolve() as any;
    });

    const store = newStore();
    render(
        <Provider store={store}>
            <RerenderingContainer node={containerNode} />
        </Provider>
    );

    const button = await screen.findByText("Press me");

    expect(screen.getByRole("checkbox")).not.toBeChecked();

    fireEvent.click(button);

    // The decisive DOM-level assertion: the patch was actually applied, the checkbox is now checked.
    await screen.findByRole("checkbox", { checked: true });
});

test("clicking a server-driven Button rendered WITHOUT any RenderingSupportContext provider does not throw (graceful no-op)", async () => {
    const onClickHandler: ServerHandler = {
        type: "SERVER",
        target: ["standalone", "button", "onclick"],
        contextId: "ctx-standalone"
    };
    const buttonNode: ButtonNode = {
        target: ["standalone", "button"],
        type: Button.TYPE,
        name: { DEFAULT: "Standalone" },
        style: "primary",
        onClick: onClickHandler
    };

    // No Redux Provider, no RerenderingContainer/LocalRerenderingContainer ancestor -> no RenderingSupportContext.
    render(<Button node={buttonNode} />);

    const button = await screen.findByText("Standalone");

    expect(() => fireEvent.click(button)).not.toThrow();
    expect(mockedSendEvent).toHaveBeenCalledTimes(1);
    expect(mockedSendEvent).toHaveBeenCalledWith(
        onClickHandler.target,
        onClickHandler.contextId,
        undefined,
        undefined
    );
});
