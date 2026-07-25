// Mock axios itself (not the Backend module) via an explicit factory: this lets the REAL
// Backend.ts (and its real InFlightTracker wiring) load and run under Jest, while swapping out
// only axios's network calls -- axios's real ESM build is never evaluated because the factory
// never imports it (see react4j-core-ui-jest-use-npm-test-and-run-build-for-tsc: automocking a
// module that transitively pulls in real axios fails babel-jest's CJS-only transform; mocking
// axios directly with a factory sidesteps that entirely).
jest.mock("axios", () => ({
    __esModule: true,
    default: {
        post: jest.fn(),
        get: jest.fn()
    }
}));

import Axios from "axios";
import { Backend } from "./Backend";
import { InFlightTracker } from "./InFlightTracker";

const mockedPost = Axios.post as jest.MockedFunction<typeof Axios.post>;

beforeEach(() => {
    jest.clearAllMocks();
    InFlightTracker.resetForTests();
});

test("sendEvent increments the in-flight count while pending and settles back to 0 on a successful round-trip", async () => {
    let resolvePost!: (value: unknown) => void;
    mockedPost.mockImplementation(() => new Promise((resolve) => {
        resolvePost = resolve;
    }));

    const pending = Backend.sendEvent(["form", "submitButton"], "ctx1");

    expect(InFlightTracker.getCount()).toBe(1);
    expect(InFlightTracker.isRerenderPending()).toBe(true);

    resolvePost({ data: {} });
    await pending;

    expect(InFlightTracker.getCount()).toBe(0);
    expect(InFlightTracker.isRerenderPending()).toBe(false);
});

test("sendEvent settles the in-flight count back to 0 even when the round-trip FAILS (finally, not just then)", async () => {
    let rejectPost!: (reason?: unknown) => void;
    mockedPost.mockImplementation(() => new Promise((_resolve, reject) => {
        rejectPost = reject;
    }));

    const pending = Backend.sendEvent(["form", "submitButton"], "ctx1");

    expect(InFlightTracker.getCount()).toBe(1);

    rejectPost(new Error("network error"));
    await expect(pending).rejects.toThrow("network error");

    expect(InFlightTracker.getCount()).toBe(0);
    expect(InFlightTracker.isRerenderPending()).toBe(false);
});

test("uploadFile increments the in-flight count while pending and settles back to 0 on success", async () => {
    let resolvePost!: (value: unknown) => void;
    mockedPost.mockImplementation(() => new Promise((resolve) => {
        resolvePost = resolve;
    }));

    const file = new File(["content"], "test.txt", { type: "text/plain" });
    const pending = Backend.uploadFile("ui/upload", "upload-1", file);

    expect(InFlightTracker.getCount()).toBe(1);

    resolvePost({ data: { uploadId: "upload-1", filename: "test.txt", size: 7, contentType: "text/plain" } });
    await pending;

    expect(InFlightTracker.getCount()).toBe(0);
});

test("uploadFile settles the in-flight count back to 0 even when the round-trip FAILS", async () => {
    let rejectPost!: (reason?: unknown) => void;
    mockedPost.mockImplementation(() => new Promise((_resolve, reject) => {
        rejectPost = reject;
    }));

    const file = new File(["content"], "test.txt", { type: "text/plain" });
    const pending = Backend.uploadFile("ui/upload", "upload-1", file);

    expect(InFlightTracker.getCount()).toBe(1);

    rejectPost(new Error("upload failed"));
    await expect(pending).rejects.toThrow("upload failed");

    expect(InFlightTracker.getCount()).toBe(0);
});

test("two concurrent round-trips keep data-rerender-pending true until BOTH settle", async () => {
    let resolveFirst!: (value: unknown) => void;
    let resolveSecond!: (value: unknown) => void;
    mockedPost
        .mockImplementationOnce(() => new Promise((resolve) => {
            resolveFirst = resolve;
        }))
        .mockImplementationOnce(() => new Promise((resolve) => {
            resolveSecond = resolve;
        }));

    const first = Backend.sendEvent(["a"], "ctx1");
    const second = Backend.sendEvent(["b"], "ctx2");

    expect(InFlightTracker.getCount()).toBe(2);

    resolveFirst({ data: {} });
    await first;

    expect(InFlightTracker.getCount()).toBe(1);
    expect(InFlightTracker.isRerenderPending()).toBe(true);

    resolveSecond({ data: {} });
    await second;

    expect(InFlightTracker.getCount()).toBe(0);
    expect(InFlightTracker.isRerenderPending()).toBe(false);
});
