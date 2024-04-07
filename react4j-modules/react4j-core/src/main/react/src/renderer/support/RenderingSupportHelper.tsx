import React, { Dispatch } from "react";
import { ConnectedComponent, connect } from 'react-redux';
import { RootReducerState, UIContextStates } from '../../reducer/StoreStates';
import { Node, RenderingSupport } from '../Renderer';
import { UIContext, UIContextAccessor } from '../data/DataContextManager';
import { Actions } from "../../reducer/Actions";
import * as actions from '../../reducer/Action';
import LocalRerenderingContainer from "../components/LocalRerenderingContainer";

export interface UIContextsState {
    uiContextRenderingCounter: number;
    uiContexts: UIContextStates;
}

export interface UpdateUIContextAction {
    (uiContext: UIContext): void;
}

export interface UpdateNodeAction {
    (node: Node): void;
}

export interface UpdateUIContextActions {
    updateUIContext: UpdateUIContextAction;
}

export interface UpdateActions extends UpdateUIContextActions {
    updateNodeAction: UpdateNodeAction;
}

export class RenderingSupportHelper {
    public static connect<C extends typeof React.Component>(container: C, contextIdExtractor: (props: any) => string | undefined, defaultNodeProvider: (props: any) => any): ConnectedComponent<C, any> {
        const mapStateToProps = ({ uiContexts, nodes }: RootReducerState, props: any) => {
            const contextId = contextIdExtractor(props);
            const uiContext: UIContext = uiContexts[contextId || ""];
            const uiContextRenderingCounter = uiContext?.updateCounter || 0;
            const node = (nodes[props.node?.target?.join(".")] || defaultNodeProvider(props));
            return { uiContextRenderingCounter, uiContexts, node } as UIContextsState;
        }
        const mapDispatcherToProps = (dispatch: Dispatch<Actions>) => {
            const updateUIContextAction: UpdateUIContextAction = (uiContext: UIContext) => dispatch(actions.updateUIContext(uiContext.contextId, uiContext));
            const updateNodeAction: UpdateNodeAction = (node: Node) => dispatch(actions.updateNode(node.target, node));
            return {
                updateUIContext: updateUIContextAction,
                updateNodeAction: updateNodeAction
            } as UpdateActions;
        }
        return connect(mapStateToProps, mapDispatcherToProps)(container);
    }

    public static newRenderingSupport(uiContextsState: UIContextsState, updateActions: UpdateActions): RenderingSupport {
        return {
            uiContextAccessor: RenderingSupportHelper.newUIContextAccessor(uiContextsState, updateActions),
            nodeContextAccessor: {
                updateNode: (node) => {
                    updateActions.updateNodeAction(node);
                }
            }
        };
    }

    private static newUIContextAccessor(uiContextsState: UIContextsState, updateUIContextAction: UpdateUIContextActions): UIContextAccessor {
        return {
            updateUIContext: (uiContext) => {
                if (uiContext) {
                    uiContext.updateCounter = 1 + Math.max(uiContextsState.uiContexts[uiContext.contextId]?.updateCounter, uiContext.updateCounter);
                    uiContextsState.uiContexts[uiContext.contextId] = uiContext;
                    updateUIContextAction.updateUIContext(uiContext);
                }
            },
            getUIContextById: (contextId) => {
                if (!uiContextsState.uiContexts[contextId]) {
                    uiContextsState.uiContexts[contextId] = {
                        contextId: contextId,
                        data: {},
                        updateCounter: 0
                    };
                }
                return uiContextsState.uiContexts[contextId];
            }
        };
    }
}

