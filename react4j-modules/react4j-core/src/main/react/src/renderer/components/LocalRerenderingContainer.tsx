import React, { ReactElement } from "react";
import { RenderingSupport } from "../Renderer";
import { RenderingSupportHelper, UIContextsState, UpdateActions } from "../support/RenderingSupportHelper";

export interface Props {
    contextId: string | undefined;
    children?: (renderingSupport: RenderingSupport) => ReactElement;
}

interface State {
}

type PropsWithReduxStore = Props & UIContextsState & UpdateActions;

class LocalRerenderingContainer extends React.Component<PropsWithReduxStore, State> {
    public render(): JSX.Element {
        if (this.props.children) {
            return this.props.children(RenderingSupportHelper.newRenderingSupport(this.props, this.props));
        }
        else {
            return (<></>);
        }
    }
}

export default RenderingSupportHelper.connect(LocalRerenderingContainer, (props: Props) => props.contextId, (props: Props) => undefined);
