import React from "react";
import { Node, Renderer } from "../Renderer";
import { RenderingSupportContext } from "../support/RenderingSupportContext";
import { RenderingSupportHelper, UIContextsState, UpdateActions, UpdateNodeAction, UpdateUIContextAction } from "../support/RenderingSupportHelper";

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
        const renderingSupport = RenderingSupportHelper.newRenderingSupport(this.props, this.props);
        return (
            <RenderingSupportContext.Provider value={renderingSupport}>
                {
                    Renderer.render(this.props.node?.content, renderingSupport)
                }
            </RenderingSupportContext.Provider>
        );
    }
}

export default RenderingSupportHelper.connect<typeof RerenderingContainer>(RerenderingContainer, (props: Props) => props.node?.content?.uiContextIds, (props: Props) => props.node);
