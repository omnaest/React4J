import React, { ReactElement } from "react";
import { RenderingSupport } from "../Renderer";
import { RerenderingHelper, UIContextsState, UpdateActions } from "../support/RerenderingHelper";

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
            return this.props.children(RerenderingHelper.newRenderingSupport(this.props, this.props));
        }
        else {
            return (<></>);
        }
    }
}

export default RerenderingHelper.connect(LocalRerenderingContainer, (props: Props) => props.contextId, (props: Props) => undefined);
