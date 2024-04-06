import React, { Dispatch } from "react";
import { connect } from "react-redux";
import * as actions from '../../reducer/Action';
import { Actions } from "../../reducer/Actions";
import { RootReducerState, UIContextStates } from "../../reducer/StoreStates";
import { Node, Renderer } from "../Renderer";
import { UIContext } from "../data/DataContextManager";
import { RerenderingHelper, UIContextsState, UpdateActions, UpdateNodeAction, UpdateUIContextAction } from "../support/RerenderingHelper";

export interface RerenderingContainerNode extends Node {
    content: Node;
    enableNodeReload: boolean;
}

export interface Props {
    node: RerenderingContainerNode;
}

interface State {
}

type PropsWithReduxStore = Props & UIContextsState & UpdateActions;

class RerenderingContainer extends React.Component<PropsWithReduxStore, State> {
    public static TYPE: string = "RERENDERINGCONTAINER";

    public render(): JSX.Element {
        return (
            <>
                {
                    Renderer.render(this.props.node?.content, RerenderingHelper.newRenderingSupport(this.props, this.props))
                }
            </>
        );
    }
}

export default RerenderingHelper.connect<typeof RerenderingContainer>(RerenderingContainer, (props: Props) => props.node?.content?.uiContext?.contextId, (props: Props) => props.node);
