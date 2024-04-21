import { UIContext } from "../renderer/data/DataContextManager";
import { Node } from "../renderer/Renderer";


export interface RootReducerState
{
    uiContexts: UIContextStates;
    nodes: NodeStates;
}

export interface UIContextStates
{
    [contextId: string]: UIContext;
}

export interface NodeStates
{
    [target: string]: Node;
}

