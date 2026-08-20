import React from "react";
import { render, screen, act } from "@testing-library/react";
import { PendingContent, PendingContentNode } from "./PendingContent";
import { InFlightTracker } from "../../backend/InFlightTracker";

/**
 * The pending block, and the claim it makes on the round trip it is reporting.
 *
 * The claim is the part worth pinning. Every round trip should produce exactly ONE indicator - the most local one
 * available - and this component suppressing the page-level bar is how that holds for anything with a local
 * placeholder. Get the release wrong and the page-level indicator stays suppressed forever, which is a silent
 * failure: the app simply stops reporting that it is working, and nothing points at this component.
 */
describe("PendingContent", () => {
    const node: PendingContentNode = {
        target: [],
        type: PendingContent.TYPE,
        // A COMPOSITE with no elements: the smallest node the real Renderer will render without needing a
        // component-specific shape. A hand-rolled TEXT node looks simpler and is not - it fails inside the Text
        // renderer on a field this test does not care about.
        content: { target: [], type: "COMPOSITE", elements: [] } as unknown as PendingContentNode["content"],
        appearAfterMillis: 0
    } as PendingContentNode;

    beforeEach(() => {
        jest.useFakeTimers();
        InFlightTracker.resetForTests();
    });

    afterEach(() => {
        jest.runOnlyPendingTimers();
        jest.useRealTimers();
    });

    test("shows nothing while the application is idle", () => {
        render(<PendingContent node={node} />);
        expect(screen.queryByTestId("react4j-pending-content")).toBeNull();
    });

    test("appears while a round trip is outstanding and claims it, so the page-level indicator stays quiet", () => {
        render(<PendingContent node={node} />);

        act(() => {
            InFlightTracker.increment();
            jest.advanceTimersByTime(10);
        });

        expect(screen.getByTestId("react4j-pending-content")).not.toBeNull();
        expect(InFlightTracker.getCount()).toBe(1);
        expect(InFlightTracker.getUnclaimedCount()).toBe(0);
    });

    test("releases its claim when the round trip settles, so later requests are reported again", () => {
        render(<PendingContent node={node} />);

        act(() => {
            InFlightTracker.increment();
            jest.advanceTimersByTime(10);
        });
        act(() => {
            InFlightTracker.decrement();
        });

        expect(screen.queryByTestId("react4j-pending-content")).toBeNull();
        // The decisive one: a claim that outlives its request would suppress the page-level indicator for the rest
        // of the session, and nothing would point here.
        expect(InFlightTracker.getUnclaimedCount()).toBe(0);

        act(() => {
            InFlightTracker.increment();
        });
        expect(InFlightTracker.getUnclaimedCount()).toBe(1);
    });

    test("stays hidden, and claims nothing, for a round trip that settles before it would appear", () => {
        render(<PendingContent node={{ ...node, appearAfterMillis: 300 }} />);

        act(() => {
            InFlightTracker.increment();
            jest.advanceTimersByTime(100);
        });
        // While waiting to appear it must NOT have claimed: the page-level bar is the only feedback during that
        // window, and suppressing it early would leave the user with nothing at all.
        expect(InFlightTracker.getUnclaimedCount()).toBe(1);

        act(() => {
            InFlightTracker.decrement();
            jest.advanceTimersByTime(500);
        });

        expect(screen.queryByTestId("react4j-pending-content")).toBeNull();
    });

    test("scrolls the enclosing scroll container to the bottom when it appears", () => {
        // A real scrolling ancestor: overflow-y auto AND content taller than the box. Both conditions are what
        // ScrollSupport looks for, and a fixture satisfying only the first would make this pass against a helper
        // that had stopped working.
        const scroller = document.createElement("div");
        scroller.style.overflowY = "auto";
        Object.defineProperty(scroller, "scrollHeight", { value: 1000, configurable: true });
        Object.defineProperty(scroller, "clientHeight", { value: 200, configurable: true });
        document.body.appendChild(scroller);

        render(<PendingContent node={node} />, { container: scroller });
        expect(scroller.scrollTop).toBe(0);

        act(() => {
            InFlightTracker.increment();
            jest.advanceTimersByTime(10);
        });

        // Nothing else would do this. The enclosing container follows the transcript on every SERVER update, and
        // this block appears without one - so an indicator below the fold is indistinguishable from nothing
        // happening, which is the problem it exists to solve.
        expect(scroller.scrollTop).toBe(1000);

        document.body.removeChild(scroller);
    });

    test("releases its claim if it is unmounted while still showing", () => {
        const { unmount } = render(<PendingContent node={node} />);

        act(() => {
            InFlightTracker.increment();
            jest.advanceTimersByTime(10);
        });
        expect(InFlightTracker.getUnclaimedCount()).toBe(0);

        unmount();

        // A re-render can replace this component mid-request. Without the release on unmount the claim would leak
        // and the count would drift permanently out of step with reality.
        expect(InFlightTracker.getUnclaimedCount()).toBe(1);
    });
});
