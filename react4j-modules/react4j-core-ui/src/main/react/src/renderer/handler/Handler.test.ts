import { Handler, HandlerFactory, ServerHandler } from "./Handler";
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

test("handleEvent(null) is a graceful no-op and does not call Backend.sendEvent", () => {
    expect(() => HandlerFactory.handleEvent(null as unknown as Handler)).not.toThrow();
    expect(mockedSendEvent).not.toHaveBeenCalled();
});

test("handleEvent(undefined) is a graceful no-op and does not call Backend.sendEvent", () => {
    expect(() => HandlerFactory.handleEvent(undefined as unknown as Handler)).not.toThrow();
    expect(mockedSendEvent).not.toHaveBeenCalled();
});

test("handleEvent dispatches a SERVER handler to Backend.sendEvent", () => {
    const serverHandler: ServerHandler = {
        type: "SERVER",
        target: ["form", "command", "input"],
        contextId: "ctx1"
    };

    HandlerFactory.handleEvent(serverHandler);

    expect(mockedSendEvent).toHaveBeenCalledTimes(1);
    expect(mockedSendEvent).toHaveBeenCalledWith(
        serverHandler.target,
        serverHandler.contextId,
        undefined,
        undefined
    );
});

test("handleEvent still throws for a genuinely unsupported non-null handler type", () => {
    const unsupportedHandler = { type: "CLIENT" } as Handler;

    expect(() => HandlerFactory.handleEvent(unsupportedHandler)).toThrow("Handler type CLIENT not supported");
    expect(mockedSendEvent).not.toHaveBeenCalled();
});
