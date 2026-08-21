import React from "react";
import { render, fireEvent, cleanup } from "@testing-library/react";
import { Provider } from "react-redux";
import { createStore } from "redux";
import { rootReducer } from "../../../reducer/Reducer";
import { Form, FormNode } from "./Form";
import { Input } from "./elements/Input";
import { Backend } from "../../../backend/Backend";
import { DataContextManager, UIContext, UIContextAccessor, UIContextDataNode } from "../../data/DataContextManager";
import { NodeContextAccessor } from "../../Renderer";

// Explicit factory mock, matching Input.test.tsx: automocking would load the real Backend, which pulls in
// axios's ESM-only build that CRA's transformIgnorePatterns does not transform.
jest.mock("../../../backend/Backend", () => ({
    Backend: {
        uploadFile: jest.fn(),
        sendEvent: jest.fn(() => Promise.resolve()),
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

/**
 * Whether typing in a form reaches the server, and whether it should.
 *
 * A form dispatches its onChange on every keystroke. For a form that registered a change handler that is the
 * whole point. For a form whose only interactivity is its submit button it is a request per character that
 * notifies nobody - measured on a chat box at three in flight at once while typing five characters.
 *
 * The value is never what depended on that traffic: handleInputChange writes it into the form's own data
 * context locally, and that context travels with the next real event.
 *
 * These two tests are a pair with FormRendererImplTest on the server. The guard here is what allows the server
 * to stop emitting an onChange target at all; before it existed, a missing target threw client-side on every
 * keystroke, which is why the server had to emit one unconditionally.
 */
function createUIContextAccessor(): UIContextAccessor {
    const contexts: { [contextId: string]: UIContext } = {
        ctx1: { contextId: "ctx1", data: {}, internalData: {}, updateCounter: 0 }
    };
    return {
        getUIContextById: (contextId: string) => contexts[contextId],
        getAllUIContexts: () => Object.values(contexts),
        updateUIContext: (uiContext: UIContext) => {
            contexts[uiContext.contextId] = uiContext;
        },
        initializeUIContext: (uiContext?: UIContextDataNode) => {
            if (uiContext) {
                contexts[uiContext.contextId] = { ...uiContext, updateCounter: 0 };
            }
        }
    };
}

function createFormNode(withOnChange: boolean): FormNode {
    return {
        target: [],
        type: Form.TYPE,
        contextId: "ctx1",
        inlineControls: false,
        elements: [
            {
                field: "message",
                contextId: "ctx1",
                type: Input.TYPE,
                label: { DEFAULT: "Message" },
                description: { DEFAULT: "" },
                disabled: false,
                readonly: false,
                required: false,
                input: { type: "text", placeholder: { DEFAULT: "" } }
            }
        ],
        onChange: withOnChange
            ? ({ type: "SERVER", target: ["form"], contextId: "ctx1" } as unknown as FormNode["onChange"])
            : undefined
    } as unknown as FormNode;
}

function typeInto(node: FormNode) {
    const uiContextAccessor = createUIContextAccessor();
    const nodeContextAccessor = {} as NodeContextAccessor;

    // A real store, because Form renders its elements inside a redux-connected rerendering container. Distinct
    // preloadedState per store, matching RerenderingContainer.test.tsx's note on cross-test leakage.
    const store = createStore(rootReducer, { uiContexts: {}, nodes: {} });
    const { container } = render(
        <Provider store={store}>
            <Form node={node} renderingSupport={{ uiContextAccessor, nodeContextAccessor }} />
        </Provider>
    );

    const input = container.querySelector("input") as HTMLInputElement;
    expect(input).not.toBeNull();
    fireEvent.change(input, { target: { value: "hello" } });
    return { uiContextAccessor, input };
}

let updateFieldByContext: jest.SpyInstance;

beforeEach(() => {
    jest.clearAllMocks();
    updateFieldByContext = jest.spyOn(DataContextManager, "updateFieldByContext");
});

afterEach(() => {
    cleanup();
    updateFieldByContext.mockRestore();
});

describe("Form onChange dispatch", () => {
    test("typing in a form with NO change handler reaches no server", () => {
        const { uiContextAccessor } = typeInto(createFormNode(false));

        expect(mockedSendEvent).not.toHaveBeenCalled();

        // Decisive companion assertion: the keystroke WAS handled, the value WAS written - only the
        // notification was skipped. Without this, "no request" would also pass if the change never reached the
        // component at all, which is the failure this guard could plausibly introduce.
        //
        // Asserted on the write rather than by reading a context back, because the rerendering container
        // supplies its own store-derived accessor rather than the one passed in as a prop - so reading the
        // prop's context proves nothing either way.
        expect(updateFieldByContext).toHaveBeenCalledWith("ctx1", "message", "hello", expect.anything());
    });

    test("typing in a form WITH a change handler still dispatches", () => {
        typeInto(createFormNode(true));

        // The other direction. Without this, guarding the dispatch would satisfy the test above while silently
        // disabling onChange for every application that actually asked for it.
        expect(mockedSendEvent).toHaveBeenCalledTimes(1);
    });
});
