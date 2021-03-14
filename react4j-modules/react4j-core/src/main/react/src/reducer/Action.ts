import { action } from "typesafe-actions";
import { Target } from "../backend/Backend";
import { UIContext } from "../renderer/data/DataContextManager";
import { Node } from "../renderer/Renderer";
import { ReducerConstants } from "./ReducerConstants";


export function updateUIContext(contextId: string, uiContext: UIContext)
{
    return action(ReducerConstants.UPDATE_UICONTEXT, {
        contextId,
        uiContext
    });
}

export function updateNode(target: Target, node: Node)
{
    return action(ReducerConstants.UPDATE_NODE, {
        target,
        node
    });
}

