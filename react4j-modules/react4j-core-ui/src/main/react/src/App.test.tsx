// Replaces the Create React App template test ("renders learn react link"), which looked for text App never
// renders and so failed forever. Why this test and not deletion: App.inFlight.test.tsx already pins App's
// in-flight attributes (data-inflight-count / data-rerender-pending), but nothing pinned App's other real
// contract - on mount it fetches the UI from the backend (Backend.getUI -> GET BackendUri.URI_UI) and renders the
// returned root node through the Renderer. That is what this file pins; the attributes stay with the sibling suite.
//
// Axios is mocked with an explicit factory (never the real ESM-only build), see
// react4j-core-ui-jest-use-npm-test-and-run-build-for-tsc. The real App, Backend and Renderer run.
jest.mock("axios", () => ({
    __esModule: true,
    default: {
        get: jest.fn(),
        post: jest.fn()
    }
}));

import React from "react";
import { render, screen } from "@testing-library/react";
import Axios from "axios";
import App from "./App";
import { BackendUri } from "./backend/Backend";
import { InFlightTracker } from "./backend/InFlightTracker";

const mockedGet = Axios.get as jest.MockedFunction<typeof Axios.get>;

beforeEach(() => {
    // CRA sets resetMocks:true, so the stub must be re-armed per test (cra-jest-resetmocks-wipes-mock-factory-implementations).
    InFlightTracker.resetForTests();
});

test("on mount App fetches the UI from the backend and renders the returned root node inside its root element", async () => {
    mockedGet.mockResolvedValue({
        data: { root: { type: "TEXT", texts: [{ DEFAULT: "Hello from the server" }] } }
    });

    const { container } = render(<App />);

    // Before the response arrives the root is rendered empty (initial state is an empty node).
    expect(screen.queryByText("Hello from the server")).toBeNull();

    // The fetched root node is rendered by the Renderer, inside the App root element.
    const text = await screen.findByText("Hello from the server", { exact: false });
    expect(container.querySelector(".App")).toContainElement(text);

    // The UI was requested once, from the UI endpoint.
    expect(mockedGet).toHaveBeenCalledTimes(1);
    expect(mockedGet).toHaveBeenCalledWith(BackendUri.URI_UI);
});
