// A separate suite from App.test.tsx: mocks axios itself via an explicit factory (never
// evaluates the real, ESM-only axios build) so the real App/Backend/InFlightTracker wiring loads
// and runs under Jest -- see react4j-core-ui-jest-use-npm-test-and-run-build-for-tsc. This is
// what App.test.tsx (pre-existing, unmocked) cannot do, and is unrelated to this slice's change.
jest.mock("axios", () => ({
    __esModule: true,
    default: {
        get: jest.fn(),
        post: jest.fn()
    }
}));

import React from "react";
import { render, act } from "@testing-library/react";
import Axios from "axios";
import App from "./App";
import { Backend } from "./backend/Backend";
import { InFlightTracker } from "./backend/InFlightTracker";

const mockedGet = Axios.get as jest.MockedFunction<typeof Axios.get>;
const mockedPost = Axios.post as jest.MockedFunction<typeof Axios.post>;

beforeEach(() => {
    // NOTE: CRA's baked-in Jest config sets resetMocks:true, which wipes any mockImplementation
    // set at jest.mock() factory time BEFORE every test -- so the home-page GET stub must be
    // (re-)installed here, not only in the factory literal above.
    InFlightTracker.resetForTests();
    mockedGet.mockResolvedValue({ data: { root: { target: [], type: "COMPOSITE", elements: [] } } });
});

test("the App root element exposes data-inflight-count=0 and data-rerender-pending=false at rest", () => {
    const { container } = render(<App />);

    const root = container.querySelector(".App");
    expect(root).not.toBeNull();
    expect(root).toHaveAttribute("data-inflight-count", "0");
    expect(root).toHaveAttribute("data-rerender-pending", "false");
});

test("the App root element's settle signal goes to 1/true while a round-trip is pending, and back to 0/false once it settles (even on failure)", async () => {
    let resolvePost!: (value: unknown) => void;
    let rejectPost!: (reason?: unknown) => void;
    mockedPost.mockImplementation(() => new Promise((resolve, reject) => {
        resolvePost = resolve;
        rejectPost = reject;
    }));

    const { container } = render(<App />);
    const root = () => container.querySelector(".App") as HTMLElement;

    expect(root()).toHaveAttribute("data-inflight-count", "0");

    // InFlightTracker.increment() runs synchronously inside Backend.sendEvent, and its
    // subscriber synchronously calls App.setState in this legacy (non-concurrent) render mode,
    // so the DOM already reflects the new count right after this call.
    let pending!: Promise<unknown>;
    act(() => {
        pending = Backend.sendEvent(["some", "target"], "ctx1");
    });

    expect(root()).toHaveAttribute("data-inflight-count", "1");
    expect(root()).toHaveAttribute("data-rerender-pending", "true");

    await act(async () => {
        resolvePost({ data: {} });
        await pending;
    });

    expect(root()).toHaveAttribute("data-inflight-count", "0");
    expect(root()).toHaveAttribute("data-rerender-pending", "false");

    // CRITICAL path: a second, FAILING round-trip must also settle the root back to 0/false.
    let secondPending!: Promise<unknown>;
    act(() => {
        secondPending = Backend.sendEvent(["some", "other-target"], "ctx2");
    });

    expect(root()).toHaveAttribute("data-inflight-count", "1");

    await act(async () => {
        rejectPost(new Error("network error"));
        await expect(secondPending).rejects.toThrow("network error");
    });

    expect(root()).toHaveAttribute("data-inflight-count", "0");
    expect(root()).toHaveAttribute("data-rerender-pending", "false");
});
