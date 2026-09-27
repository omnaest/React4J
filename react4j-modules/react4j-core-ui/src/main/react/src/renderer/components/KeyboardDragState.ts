/**
 * Minimal client-side coordination for the ONE keyboard drag-and-drop path (AC-S1-7, plan-235 S1,
 * react4j-235-dnd-s1-authoring-notes): `Draggable`'s Space/Enter "picks up" an item, storing its
 * `dragId` here; a focused `DropTarget`'s own Space/Enter then "drops" whatever is currently picked up,
 * always as relation `INTO` - a keyboard activation carries no pointer position to derive BEFORE/AFTER
 * from (see `DropTarget.tsx`). Deliberately NOT a second parallel interaction model: the keyboard path
 * reuses the EXACT SAME write-then-dispatch mechanism a pointer drop uses (`DropTarget.commitDrop`),
 * differing only in how `dragId`/`relation` are obtained.
 *
 * Module-level singleton, mirroring `DataContextManager`'s own static-store shape - there is at most one
 * "picked up" item on the page at a time, which is exactly what a single global slot models.
 */
export class KeyboardDragState {
    private static grabbedDragId: string | null = null;

    public static setGrabbedDragId(dragId: string | null): void {
        KeyboardDragState.grabbedDragId = dragId;
    }

    public static getGrabbedDragId(): string | null {
        return KeyboardDragState.grabbedDragId;
    }

    public static clear(): void {
        KeyboardDragState.grabbedDragId = null;
    }
}
