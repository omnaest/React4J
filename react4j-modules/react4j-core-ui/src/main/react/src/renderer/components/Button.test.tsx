import React from "react";
import { render, fireEvent, screen, act } from "@testing-library/react";
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
    // sendEvent has always returned a Promise; the mock used to return undefined and got away with it because
    // nothing looked at the result. Button now waits on it to know when to stop being busy, so the double must
    // honour the same contract the real one does.
    mockedSendEvent.mockResolvedValue(undefined as never);
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

// The server serialises Button.Style as the LOWER-CASE enum name (ButtonImpl: style.name().toLowerCase()).
const SERVER_STYLE_TOKENS = ["primary", "secondary", "success", "danger", "warning", "info", "light", "dark", "link"];

describe("Button variant class list", () => {
    test.each(SERVER_STYLE_TOKENS)("style '%s' renders btn, its btn-<token> class and no bare 'btn-' token", (token) => {
        const node = { ...createNode(undefined), style: token };

        render(<Button node={node} />);

        const classes = Array.from(screen.getByRole("button").classList);
        expect(classes).toContain("btn");
        expect(classes).toContain("btn-" + token);
        expect(classes).not.toContain("btn-");
    });

    test.each([undefined, null, ""])("unset style (%p) renders plain btn with no btn-* variant class", (unset) => {
        const node = { ...createNode(undefined), style: unset as unknown as string };

        render(<Button node={node} />);

        const classes = Array.from(screen.getByRole("button").classList);
        expect(classes).toContain("btn");
        expect(classes.filter((c) => c.startsWith("btn-"))).toEqual([]);
    });
});

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

test("a Button disables itself and shows a spinner while its own round trip is in flight, and recovers afterwards", async () => {
    let settleRoundTrip!: () => void;
    mockedSendEvent.mockImplementation(() => new Promise<void>((resolve) => {
        settleRoundTrip = resolve;
    }) as never);

    const node = createNode({ type: "SERVER", target: ["t"], contextId: "ctx1" } as ServerHandler);
    render(<Button node={node} />);

    const button = await screen.findByRole("button");
    expect(button).not.toBeDisabled();

    await act(async () => {
        fireEvent.click(button);
    });

    // Disabled is the part that prevents a second submission of a mutating handler; the spinner only explains it.
    expect(button).toBeDisabled();
    expect(button).toHaveAttribute("aria-busy", "true");
    expect(button.querySelector(".spinner-border")).not.toBeNull();

    await act(async () => {
        settleRoundTrip();
    });

    expect(button).not.toBeDisabled();
    expect(button.querySelector(".spinner-border")).toBeNull();
});

test("a Button whose round trip FAILS still becomes usable again", async () => {
    let rejectRoundTrip!: (reason?: unknown) => void;
    mockedSendEvent.mockImplementation(() => new Promise<void>((resolve, reject) => {
        rejectRoundTrip = reject;
    }) as never);

    const node = createNode({ type: "SERVER", target: ["t"], contextId: "ctx1" } as ServerHandler);
    render(<Button node={node} />);

    const button = await screen.findByRole("button");
    await act(async () => {
        fireEvent.click(button);
    });
    expect(button).toBeDisabled();

    await act(async () => {
        rejectRoundTrip(new Error("network down"));
    });

    // The decisive one. A control left disabled after an error is a dead end on the single occasion the user most
    // needs to retry - which is why handleEvent resolves rather than rejects when a round trip fails.
    expect(button).not.toBeDisabled();
});
