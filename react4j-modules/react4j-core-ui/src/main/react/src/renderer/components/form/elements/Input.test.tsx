import React from "react";
import { render, fireEvent } from "@testing-library/react";
import { Input, InputFormElement } from "./Input";
import { Backend } from "../../../../backend/Backend";
import { UIContext, UIContextAccessor, UIContextDataNode } from "../../../data/DataContextManager";
import { NodeContextAccessor } from "../../../Renderer";

// Explicit factory mock (no automock): automocking would require Jest to load the real Backend module,
// which transitively pulls in axios's ESM-only build; CRA's default transformIgnorePatterns does not
// transform node_modules, so that path fails with "Cannot use import statement outside a module".
// A factory mock never evaluates the real module, so it sidesteps that pre-existing jest-config gap.
jest.mock("../../../../backend/Backend", () => ({
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

function createUIContextAccessor(): UIContextAccessor {
    const contexts: { [contextId: string]: UIContext } = {
        ctx1: { contextId: "ctx1", data: {}, internalData: {}, updateCounter: 0 }
    };
    return {
        getUIContextById: (contextId: string) => contexts[contextId],
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

function createElement(withSubmitOnEnter: boolean): InputFormElement {
    return {
        field: "command",
        contextId: "ctx1",
        type: Input.TYPE,
        label: { DEFAULT: "Command" },
        description: { DEFAULT: "" },
        disabled: false,
        readonly: false,
        required: false,
        input: {
            type: "text",
            placeholder: { DEFAULT: "" },
            submitOnEnter: withSubmitOnEnter
                ? {
                    type: "SERVER",
                    target: ["form", "command", "input"],
                    contextId: "ctx1"
                }
                : undefined
        }
    };
}

beforeEach(() => {
    jest.clearAllMocks();
});

test("Enter with submitOnEnter fires handleEvent exactly once and prevents default", () => {
    const element = createElement(true);
    const uiContextAccessor = createUIContextAccessor();
    const nodeContextAccessor = {} as NodeContextAccessor;

    render(
        <Input
            id="command"
            element={element}
            onUpdate={jest.fn()}
            updateCounter={0}
            renderingSupport={{ uiContextAccessor, nodeContextAccessor }}
        />
    );

    const input = document.getElementById("command") as HTMLInputElement;
    const keyDownEvent = new (window as any).KeyboardEvent("keydown", { key: "Enter", bubbles: true, cancelable: true });
    const preventDefaultSpy = jest.spyOn(keyDownEvent, "preventDefault");

    fireEvent(input, keyDownEvent);

    expect(preventDefaultSpy).toHaveBeenCalledTimes(1);
    expect(mockedSendEvent).toHaveBeenCalledTimes(1);
    expect(mockedSendEvent).toHaveBeenCalledWith(
        ["form", "command", "input"],
        "ctx1",
        uiContextAccessor,
        nodeContextAccessor
    );
});

test("Enter without submitOnEnter is a no-op", () => {
    const element = createElement(false);
    const uiContextAccessor = createUIContextAccessor();
    const nodeContextAccessor = {} as NodeContextAccessor;

    render(
        <Input
            id="command"
            element={element}
            onUpdate={jest.fn()}
            updateCounter={0}
            renderingSupport={{ uiContextAccessor, nodeContextAccessor }}
        />
    );

    const input = document.getElementById("command") as HTMLInputElement;
    fireEvent.keyDown(input, { key: "Enter" });

    expect(mockedSendEvent).not.toHaveBeenCalled();
});

test("a non-Enter key is a no-op even with submitOnEnter present", () => {
    const element = createElement(true);
    const uiContextAccessor = createUIContextAccessor();
    const nodeContextAccessor = {} as NodeContextAccessor;

    render(
        <Input
            id="command"
            element={element}
            onUpdate={jest.fn()}
            updateCounter={0}
            renderingSupport={{ uiContextAccessor, nodeContextAccessor }}
        />
    );

    const input = document.getElementById("command") as HTMLInputElement;
    fireEvent.keyDown(input, { key: "a" });

    expect(mockedSendEvent).not.toHaveBeenCalled();
});
