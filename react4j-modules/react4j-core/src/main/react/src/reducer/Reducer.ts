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
                const newState = { ...state };
                newState.nodes[target?.join(".")] = node;
                return newState;
            }
        default:
            return state;
    }
}