import React from "react";
import { render, screen, act } from "@testing-library/react";
import { BusyIndicator } from "./BusyIndicator";

/**
 * The two properties that decide whether this component helps or annoys.
 *
 * A busy indicator is easy to get wrong in one of two opposite directions: showing on every fast interaction (pure
 * flicker, on exactly the requests nobody was waiting for), or lingering after the answer arrived (which reads as
 * the page being stuck). Both are pinned here, along with the announcement a screen-reader user gets instead of
 * the bar.
 */
describe("BusyIndicator", () => {
    beforeEach(() => {
        jest.useFakeTimers();
    });

    afterEach(() => {
        jest.runOnlyPendingTimers();
        jest.useRealTimers();
    });

    test("shows nothing at rest", () => {
        render(<BusyIndicator inFlightCount={0} />);
        expect(screen.queryByTestId("react4j-busy-indicator")).toBeNull();
    });

    test("stays hidden for a round trip that settles before the delay elapses", () => {
        const { rerender } = render(<BusyIndicator inFlightCount={0} appearAfterMillis={200} />);

        rerender(<BusyIndicator inFlightCount={1} appearAfterMillis={200} />);
        act(() => {
            jest.advanceTimersByTime(150);
        });
        rerender(<BusyIndicator inFlightCount={0} appearAfterMillis={200} />);
        act(() => {
            jest.advanceTimersByTime(500);
        });

        // The decisive assertion: a fast request must never flash the bar, not even after its timer would have
        // fired. Cancelling on settle is what makes that true.
        expect(screen.queryByTestId("react4j-busy-indicator")).toBeNull();
    });

    test("appears once a round trip has been outstanding longer than the delay", () => {
        const { rerender } = render(<BusyIndicator inFlightCount={0} appearAfterMillis={200} />);

        rerender(<BusyIndicator inFlightCount={1} appearAfterMillis={200} />);
        act(() => {
            jest.advanceTimersByTime(250);
        });

        expect(screen.getByTestId("react4j-busy-indicator")).not.toBeNull();
    });

    test("disappears immediately when the round trip settles, with no delay on the way out", () => {
        const { rerender } = render(<BusyIndicator inFlightCount={1} appearAfterMillis={0} />);
        act(() => {
            jest.advanceTimersByTime(10);
        });
        expect(screen.getByTestId("react4j-busy-indicator")).not.toBeNull();

        rerender(<BusyIndicator inFlightCount={0} appearAfterMillis={0} />);

        // No timer advanced: hiding must be synchronous. A bar still up after the answer arrived is worse than
        // one that never appeared, because it says the page is still working when it is not.
        expect(screen.queryByTestId("react4j-busy-indicator")).toBeNull();
    });

    test("announces itself politely to a screen reader rather than only drawing a bar", () => {
        const { rerender } = render(<BusyIndicator inFlightCount={0} appearAfterMillis={0} />);
        rerender(<BusyIndicator inFlightCount={1} appearAfterMillis={0} />);
        act(() => {
            jest.advanceTimersByTime(10);
        });

        const indicator = screen.getByRole("status");
        expect(indicator).toHaveAttribute("aria-live", "polite");
        expect(indicator.textContent).toContain("Working");
    });
});
