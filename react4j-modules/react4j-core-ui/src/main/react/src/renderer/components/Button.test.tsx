import React from "react";
import { render, fireEvent, screen } from "@testing-library/react";
import { Button, ButtonNode } from "./Button";
import { ServerHandler } from "../handler/Handler";
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

beforeEach(() => {
    jest.clearAllMocks();
});

function createNode(onClick: ButtonNode["onClick"]): ButtonNode {
    return {
        target: [],
        type: Button.TYPE,
        name: { DEFAULT: "Click me" },
        style: "primary",
        onClick: onClick
    };
}

test("clicking a Button whose node.onClick is null does not throw and does not call Backend.sendEvent", async () => {
    const node = createNode(null as unknown as ButtonNode["onClick"]);

    render(<Button node={node} />);

    const button = await screen.findByText("Click me");

    expect(() => fireEvent.click(button)).not.toThrow();
    expect(mockedSendEvent).not.toHaveBeenCalled();
});

test("clicking a Button whose node.onClick is undefined does not throw and does not call Backend.sendEvent", async () => {
    const node = createNode(undefined);

    render(<Button node={node} />);

    const button = await screen.findByText("Click me");

    expect(() => fireEvent.click(button)).not.toThrow();
    expect(mockedSendEvent).not.toHaveBeenCalled();
});

test("clicking a Button with a SERVER onClick handler dispatches to Backend.sendEvent", async () => {
    const serverHandler: ServerHandler = {
        type: "SERVER",
        target: ["form", "submit", "button"],
        contextId: "ctx1"
    };
    const node = createNode(serverHandler);

    render(<Button node={node} />);

    const button = await screen.findByText("Click me");
    fireEvent.click(button);

    expect(mockedSendEvent).toHaveBeenCalledTimes(1);
    expect(mockedSendEvent).toHaveBeenCalledWith(
        ["form", "submit", "button"],
        "ctx1",
        undefined,
        undefined
    );
});
