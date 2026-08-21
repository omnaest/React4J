import { Action } from "typesafe-actions";
import { Actions } from "./Actions";
import { ReducerConstants } from "./ReducerConstants";
import { RootReducerState, UIContextStates } from "./StoreStates";

const init: RootReducerState = {
    uiContexts: {},
    nodes: {}
};

export function rootReducer(state: RootReducerState = init,
    action: Actions): RootReducerState
{
    switch (action.type)
    {
        case ReducerConstants.UPDATE_UICONTEXT:
            {
                const contextId = action.payload.contextId;
                const uiContext = action.payload.uiContext;
                const newState = { ...state };
                newState.uiContexts[contextId] = uiContext;
                return newState;
            }
        case ReducerConstants.UPDATE_NODE:
            {
                const target = action.payload.target;
                const node = action.payload.node;
                const key = target?.join(".");
                const newState = { ...state };
                newState.nodes = { ...state.nodes };

                // A node stored for an ancestor supersedes every node stored BENEATH it.
                //
                // WHY. Each connected component prefers `nodes[itsOwnTarget]` over the node its parent hands
                // down in props (see RenderingSupportHelper.connect). That is what lets a targeted round trip
                // update one region without re-rendering the page. But it also means an entry written by an
                // EARLIER targeted event outlives the region it described: when a later event resolves higher
                // up the tree, the ancestor's fresh subtree arrives in props and the stale descendant entry
                // silently wins.
                //
                // Measured live: switching a table to flat stored a node for the table (100 unfiltered rows);
                // the next chat message then re-rendered the enclosing region and its response carried the
                // correct table -- 64 rows, filtered -- while the screen kept showing the old 100. The badge
                // beside the table updated in the same response, so the page showed two different answers to
                // the same question at once.
                //
                // Deleting the descendants makes them fall back to `defaultNodeProvider(props)`, which is
                // exactly the fresh node the ancestor just delivered.
                if (key)
                {
                    Object.keys(newState.nodes)
                          .filter((storedKey) => storedKey.startsWith(key + "."))
                          .forEach((staleDescendantKey) => delete newState.nodes[staleDescendantKey]);
                }

                newState.nodes[key] = node;
                return newState;
            }
        default:
            return state;
    }
}