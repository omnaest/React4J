import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import { Breadcrumb, BreadcrumbNode } from "./Breadcrumb";
import { Backend } from "../../backend/Backend";

// Explicit factory mock (no automock) - see react4j-core-ui-jest-mock-backend-factory-not-automock:
// automocking would require Jest to load the real Backend module, which transitively pulls in axios's
// ESM-only build; CRA's default transformIgnorePatterns does not transform node_modules, so that path
// fails with "Cannot use import statement outside a module". A factory mock never evaluates the real
// module, so it sidesteps that pre-existing jest-config gap.
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

function createNode(entries: BreadcrumbNode["entries"], locator?: string): BreadcrumbNode {
    return {
        target: [],
        type: Breadcrumb.TYPE,
        entries: entries,
        locator: locator
    };
}

test("an active entry renders as breadcrumb-item active with aria-current=page and no anchor", () => {
    const node = createNode([
        { text: { DEFAULT: "Current" }, link: undefined as unknown as string, linkedId: undefined as unknown as string, active: true }
    ]);

    render(<Breadcrumb node={node} />);

    const item = screen.getByText("Current");
    expect(item.tagName).toBe("LI");
    expect(item).toHaveClass("breadcrumb-item", "active");
    expect(item).toHaveAttribute("aria-current", "page");
    expect(item.querySelector("a")).toBeNull();
});

test("an onClick entry's handler is reachable from the anchor and dispatches through HandlerFactory/Backend.sendEvent", () => {
    const node = createNode([
        {
            text: { DEFAULT: "Root" },
            link: undefined as unknown as string,
            linkedId: undefined as unknown as string,
            active: false,
            onClick: { type: "SERVER", target: ["breadcrumb", "0"], contextId: "ctx1" } as any
        }
    ]);

    render(<Breadcrumb node={node} />);

    const anchor = screen.getByText("Root").closest("a") as HTMLAnchorElement;
    expect(anchor).not.toBeNull();

    fireEvent.click(anchor);

    expect(mockedSendEvent).toHaveBeenCalledTimes(1);
    expect(mockedSendEvent).toHaveBeenCalledWith(["breadcrumb", "0"], "ctx1", undefined, undefined);
});

test("a link entry gets href={link}", () => {
    const node = createNode([
        { text: { DEFAULT: "Home" }, link: "/home", linkedId: undefined as unknown as string, active: false }
    ]);

    render(<Breadcrumb node={node} />);

    expect(screen.getByText("Home").closest("a")).toHaveAttribute("href", "/home");
});

test("a linkedId-only entry gets href='#'+linkedId", () => {
    const node = createNode([
        { text: { DEFAULT: "Section" }, link: undefined as unknown as string, linkedId: "target-id", active: false }
    ]);

    render(<Breadcrumb node={node} />);

    expect(screen.getByText("Section").closest("a")).toHaveAttribute("href", "#target-id");
});

test("onClick beats link: the handler still dispatches even though a link is also set", () => {
    const node = createNode([
        {
            text: { DEFAULT: "Both" },
            link: "/somewhere",
            linkedId: undefined as unknown as string,
            active: false,
            onClick: { type: "SERVER", target: ["breadcrumb", "1"], contextId: "ctx2" } as any
        }
    ]);

    render(<Breadcrumb node={node} />);

    const anchor = screen.getByText("Both").closest("a") as HTMLAnchorElement;
    fireEvent.click(anchor);

    expect(mockedSendEvent).toHaveBeenCalledTimes(1);
    expect(mockedSendEvent).toHaveBeenCalledWith(["breadcrumb", "1"], "ctx2", undefined, undefined);
});

test("link beats linkedId: href resolves to link when both are set", () => {
    const node = createNode([
        { text: { DEFAULT: "LinkWins" }, link: "/preferred", linkedId: "fallback-id", active: false }
    ]);

    render(<Breadcrumb node={node} />);

    expect(screen.getByText("LinkWins").closest("a")).toHaveAttribute("href", "/preferred");
});

// AC-6, React half - the criterion-2a drift guard: for a node with locator and onClick both unset, the
// <nav> must carry NO id attribute at all (not an empty one), and the anchor must carry no click behaviour.
test("with locator and onClick both unset, the nav has no id attribute at all and the anchor has no click behaviour", () => {
    const node = createNode([
        { text: { DEFAULT: "Plain" }, link: "/plain", linkedId: undefined as unknown as string, active: false }
    ]);

    render(<Breadcrumb node={node} />);

    expect(screen.getByRole("navigation")).not.toHaveAttribute("id");

    const anchor = screen.getByText("Plain").closest("a") as HTMLAnchorElement;
    fireEvent.click(anchor);
    expect(mockedSendEvent).not.toHaveBeenCalled();
});

// AC-7, React half - with locator set, the <nav> carries id="<locator>".
test("with locator set, the nav carries id equal to the locator", () => {
    const node = createNode(
        [{ text: { DEFAULT: "Root" }, link: "/root", linkedId: undefined as unknown as string, active: false }],
        "ancestor-trail"
    );

    render(<Breadcrumb node={node} />);

    expect(screen.getByRole("navigation")).toHaveAttribute("id", "ancestor-trail");
});

// H5 - REFUTED on the exact "href is undefined" wording, CONFIRMED on the accessibility outcome. With
// onClick set and neither link nor linkedId, @restart/ui's Anchor (via useButtonProps) treats a missing
// href as "trivial" and rewrites it to "#" (Button.js:53, "Ensure there's a href so Enter can trigger
// anchor button") rather than leaving it undefined - but it still applies role="button" and tabIndex=0,
// so the entry IS keyboard-reachable and announced as a control, which is the substance H5 predicted.
test("H5: an onClick-only entry (no link, no linkedId) is announced as a button and keyboard-reachable", () => {
    const node = createNode([
        {
            text: { DEFAULT: "ClickOnly" },
            link: undefined as unknown as string,
            linkedId: undefined as unknown as string,
            active: false,
            onClick: { type: "SERVER", target: ["breadcrumb", "2"], contextId: "ctx3" } as any
        }
    ]);

    render(<Breadcrumb node={node} />);

    const control = screen.getByRole("button", { name: "ClickOnly" });
    expect(control).toHaveAttribute("href", "#");
    expect(control).toHaveAttribute("tabindex", "0");
});
