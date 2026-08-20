/**
 * C2 testability hook (plan-74 Goal 3b): a render-settled signal.
 *
 * A global in-flight-request counter, incremented/decremented around every client->server
 * round-trip XHR (Backend.sendEvent for /ui/event, Backend.uploadFile for /ui/upload). Surfaced
 * on the app's stable root element (see App.tsx) as `data-inflight-count` (numeric) and
 * `data-rerender-pending` (boolean, count > 0), so a browser test can `waitFor` the count to
 * settle back to 0 instead of sleeping.
 *
 * CRITICAL: callers must decrement in a `finally` so a FAILED round-trip still settles the
 * counter back down — see Backend.ts.
 */
export type InFlightListener = (count: number) => void;

export class InFlightTracker {
    private static count = 0;
    private static claimedCount = 0;
    private static listeners: InFlightListener[] = [];

    public static increment(): void {
        InFlightTracker.count += 1;
        InFlightTracker.notifyListeners();
    }

    public static decrement(): void {
        InFlightTracker.count = Math.max(0, InFlightTracker.count - 1);
        InFlightTracker.notifyListeners();
    }

    public static getCount(): number {
        return InFlightTracker.count;
    }

    public static isRerenderPending(): boolean {
        return InFlightTracker.count > 0;
    }

    /**
     * Reports that a round trip is already represented by a local indicator - a form disabling itself, a button
     * showing a spinner - and therefore needs no page-level one.
     *
     * WHY THIS EXISTS. Every round trip should produce exactly ONE indicator: the most local one available. Before
     * this, a chat submission produced three at once - the bar across the top of the window, a spinner in the
     * button and a greyed-out field - all saying the same thing about the same request. Redundant signals are not
     * extra reassurance; they train a user to stop reading any of them.
     *
     * Claiming is counted rather than flagged for the same reason the total is: several claimed requests can
     * overlap, and a boolean would let the first to settle un-claim the rest.
     */
    public static claim(): void {
        InFlightTracker.claimedCount += 1;
        InFlightTracker.notifyListeners();
    }

    public static release(): void {
        InFlightTracker.claimedCount = Math.max(0, InFlightTracker.claimedCount - 1);
        InFlightTracker.notifyListeners();
    }

    /**
     * Round trips that nothing local is reporting - what the page-level indicator shows.
     *
     * Never negative: a claim is made by a control that then starts a request, so for a moment the claim can
     * outnumber the requests. Clamping keeps that ordering detail from surfacing as a flicker.
     */
    public static getUnclaimedCount(): number {
        return Math.max(0, InFlightTracker.count - InFlightTracker.claimedCount);
    }

    /**
     * Subscribes to count changes; immediately invoked once with the current count. Returns an
     * unsubscribe function.
     */
    public static subscribe(listener: InFlightListener): () => void {
        InFlightTracker.listeners.push(listener);
        listener(InFlightTracker.count);
        return () => {
            InFlightTracker.listeners = InFlightTracker.listeners.filter((candidate) => candidate !== listener);
        };
    }

    private static notifyListeners(): void {
        InFlightTracker.listeners.forEach((listener) => listener(InFlightTracker.count));
    }

    /**
     * Test-only helper: this is a module-level singleton (mirrors the app's single Backend
     * instance), so tests that exercise it directly or via Backend must reset it between cases.
     */
    public static resetForTests(): void {
        InFlightTracker.count = 0;
        InFlightTracker.claimedCount = 0;
        InFlightTracker.listeners = [];
    }
}
