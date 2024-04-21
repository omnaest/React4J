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
    public static connect<C extends typeof React.Component>(container: C, uiContextIdsExtractor: (props: any) => string[] | undefined, defaultNodeProvider: (props: any) => any): ConnectedComponent<C, any> {
        const mapStateToProps = ({ uiContexts, nodes }: RootReducerState, props: any) => {
            let uiContextRenderingCounter = 0;
            const contextIds = uiContextIdsExtractor(props);
            contextIds?.forEach((contextId) => {
                const uiContext: UIContext = uiContexts[contextId];
                uiContextRenderingCounter += uiContext?.updateCounter || 0;
            })
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
        const updateUIContext = (uiContext: UIContext) => {
            if (uiContext) {
                uiContext.updateCounter = 1 + Math.max(uiContextsState.uiContexts[uiContext.contextId]?.updateCounter || 0, uiContext.updateCounter || 0);
                uiContextsState.uiContexts[uiContext.contextId] = uiContext;
                updateUIContextAction.updateUIContext(uiContext);
            }
        };
        const getUIContextById = (contextId: string) => {
            if (!uiContextsState.uiContexts[contextId]) {
                uiContextsState.uiContexts[contextId] = {
                    contextId: contextId,
                    data: {},
                    internalData: {},
                    updateCounter: 0
                };
            }
            return uiContextsState.uiContexts[contextId];
        };
        return {
            updateUIContext: updateUIContext,
            initializeUIContext: (uiContextNode?) => {
                if (uiContextNode?.contextId && !uiContextsState.uiContexts[uiContextNode?.contextId]) {
                    const uiContext = {
                        ...uiContextNode,
                        updateCounter: 0
                    };
                    if (!uiContext.data) {
                        uiContext.data = {};
                    }
                    if (!uiContext.internalData) {
                        uiContext.internalData = {};
                    }
                    updateUIContext(uiContext);
                }
            },
            getUIContextById: getUIContextById
        };
    }
}

