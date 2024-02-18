import React, { Dispatch } from "react";
import { connect } from "react-redux";
import * as actions from '../../reducer/Action';
import { Actions } from "../../reducer/Actions";
import { RootReducerState } from "../../reducer/StoreStates";
import { Node, Renderer } from "../Renderer";
import { UIContext } from "../data/DataContextManager";

export interface RerenderingContainerNode extends Node
{
    content: Node;
    enableNodeReload: boolean;
}

const mapStateToProps = ({ uiContexts, nodes }: RootReducerState, props: Props) =>
{
    const contextId = props.node?.content?.uiContext?.contextId;
    const uiContext: UIContext = uiContexts[contextId || ""];
    const uiContextRenderingCounter = uiContext?.updateCounter || 0;
    const node = (nodes[props.node.target?.join(".")] || props.node) as unknown as RerenderingContainerNode;
    return { uiContextRenderingCounter, uiContexts, node };
}

export interface UpdateUIContextAction
{
    (uiContext: UIContext): void;
}

export interface UpdateNodeAction
{
    (node: Node): void;
}

const mapDispatcherToProps = (dispatch: Dispatch<Actions>) =>
{
    const updateUIContextAction: UpdateUIContextAction = (uiContext: UIContext) => dispatch(actions.updateUIContext(uiContext.contextId, uiContext));
    const updateNodeAction: UpdateNodeAction = (node: Node) => dispatch(actions.updateNode(node.target, node));
    return {
        updateUIContext: updateUIContextAction,
        updateNodeAction: updateNodeAction
    }
}

export interface Props
{
    node: RerenderingContainerNode;
}

interface State
{
}

type PropsWithReduxStore = Props & ReturnType<typeof mapStateToProps> & ReturnType<typeof mapDispatcherToProps>;

class RerenderingContainer extends React.Component<PropsWithReduxStore, State>
{
    public static TYPE: string = "RERENDERINGCONTAINER";

    public render(): JSX.Element
    {
        return (
            <>
                {
                    Renderer.render(this.props.node?.content, {
                        uiContextAccessor: {
                            updateUIContext: (uiContext) =>
                            {
                                if (uiContext)
                                {
                                    uiContext.updateCounter = 1 + Math.max(this.props.uiContexts[uiContext.contextId]?.updateCounter, uiContext.updateCounter);
                                    this.props.uiContexts[uiContext.contextId] = uiContext;
                                    this.props.updateUIContext(uiContext);
                                }
                            },
                            getUIContextById: (contextId) =>
                            {
                                if (!this.props.uiContexts[contextId])
                                {
                                    this.props.uiContexts[contextId] = {
                                        contextId: contextId,
                                        data: {},
                                        updateCounter: 0
                                    };
                                }
                                return this.props.uiContexts[contextId];
                            }
                        },
                        nodeContextAccessor: {
                            updateNode: (node) =>
                            {
                                this.props.updateNodeAction(node);
                            }
                        }
                    })
                }
            </>
        );
    }
}

export default connect(mapStateToProps, mapDispatcherToProps)(RerenderingContainer);
