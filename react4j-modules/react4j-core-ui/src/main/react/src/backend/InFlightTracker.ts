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
        InFlightTracker.listeners = [];
    }
}
