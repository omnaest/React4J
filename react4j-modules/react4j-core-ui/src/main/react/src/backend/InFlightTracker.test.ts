import { InFlightTracker } from "./InFlightTracker";

beforeEach(() => {
    InFlightTracker.resetForTests();
});

test("increment raises the count and isRerenderPending becomes true", () => {
    expect(InFlightTracker.getCount()).toBe(0);
    expect(InFlightTracker.isRerenderPending()).toBe(false);

    InFlightTracker.increment();

    expect(InFlightTracker.getCount()).toBe(1);
    expect(InFlightTracker.isRerenderPending()).toBe(true);
});

test("decrement never goes below 0, even when called more often than increment", () => {
    InFlightTracker.decrement();
    InFlightTracker.decrement();

    expect(InFlightTracker.getCount()).toBe(0);
});

test("count settles back to 0 after matching increment/decrement pairs (multiple in-flight requests)", () => {
    InFlightTracker.increment();
    InFlightTracker.increment();
    expect(InFlightTracker.getCount()).toBe(2);

    InFlightTracker.decrement();
    expect(InFlightTracker.getCount()).toBe(1);
    expect(InFlightTracker.isRerenderPending()).toBe(true);

    InFlightTracker.decrement();
    expect(InFlightTracker.getCount()).toBe(0);
    expect(InFlightTracker.isRerenderPending()).toBe(false);
});

test("subscribe is invoked immediately with the current count, and again on every change", () => {
    const observed: number[] = [];
    const unsubscribe = InFlightTracker.subscribe((count) => observed.push(count));

    expect(observed).toEqual([0]);

    InFlightTracker.increment();
    InFlightTracker.decrement();

    expect(observed).toEqual([0, 1, 0]);

    unsubscribe();
    InFlightTracker.increment();

    // no further notifications after unsubscribe
    expect(observed).toEqual([0, 1, 0]);
});
