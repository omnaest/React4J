import { rootReducer } from "./Reducer";
import { updateNode } from "./Action";
import { RootReducerState } from "./StoreStates";

/**
 * The node store, and the one rule that is not obvious about it.
 *
 * A connected component prefers `nodes[itsOwnTarget]` over the node its parent hands down, which is what lets a
 * targeted round trip update one region without re-rendering the page. The cost is that such an entry outlives
 * the region it described: when a later event resolves further UP the tree, the ancestor's fresh subtree arrives
 * in props and a stale descendant entry silently wins over it.
 *
 * That is not a hypothetical. Switching a table to flat stored a node for the table (100 unfiltered rows); the
 * next chat message re-rendered the enclosing region, and its response carried the correct table -- 64 rows,
 * filtered -- while the screen kept showing the old 100. The badge beside the table updated from the same
 * response, so the page displayed two different answers to the same question at once.
 */
describe("rootReducer UPDATE_NODE", () => {
    const nodeAt = (label: string) => ({ target: [], type: "COMPOSITE", label } as any);

    const stateWith = (nodes: { [key: string]: any }): RootReducerState => ({ uiContexts: {}, nodes } as RootReducerState);

    test("stores a node under its target", () => {
        const state = rootReducer(stateWith({}), updateNode(["a", "b"] as any, nodeAt("fresh")));
        expect(state.nodes["a.b"]).toEqual(nodeAt("fresh"));
    });

    test("an ancestor update drops nodes stored beneath it, so they fall back to the fresh props node", () => {
        const before = stateWith({
            "a.b": nodeAt("stale-outer-child"),
            "a.b.c.grid": nodeAt("stale-table-100-rows")
        });

        const after = rootReducer(before, updateNode(["a", "b"] as any, nodeAt("fresh-subtree-with-64-rows")));

        expect(after.nodes["a.b"]).toEqual(nodeAt("fresh-subtree-with-64-rows"));
        // THE decisive one: without this the table keeps rendering its own stored node and ignores the corrected
        // subtree its parent just received.
        expect(after.nodes["a.b.c.grid"]).toBeUndefined();
    });

    test("leaves siblings and ancestors alone -- only what is BENEATH the updated target is superseded", () => {
        const before = stateWith({
            "a": nodeAt("ancestor"),
            "a.bb": nodeAt("sibling-with-a-prefix-that-merely-starts-the-same"),
            "a.b": nodeAt("target"),
            "a.b.c": nodeAt("descendant")
        });

        const after = rootReducer(before, updateNode(["a", "b"] as any, nodeAt("fresh")));

        expect(after.nodes["a"]).toEqual(nodeAt("ancestor"));
        // "a.bb" starts with "a.b" as a STRING but is not beneath it as a target - the separator is what
        // distinguishes them, and dropping it would silently blank an unrelated region.
        expect(after.nodes["a.bb"]).toEqual(nodeAt("sibling-with-a-prefix-that-merely-starts-the-same"));
        expect(after.nodes["a.b.c"]).toBeUndefined();
    });

    test("does not mutate the previous state's node map", () => {
        const before = stateWith({ "a.b.c": nodeAt("descendant") });

        rootReducer(before, updateNode(["a", "b"] as any, nodeAt("fresh")));

        // The reducer used to write through a shallow copy into the SAME nodes object. Deleting entries that way
        // would edit history - anything holding the previous state would see the deletions too.
        expect(before.nodes["a.b.c"]).toEqual(nodeAt("descendant"));
    });
});
