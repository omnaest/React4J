import React, { ReactElement } from "react";
import { RenderingSupport } from "../Renderer";
import { RenderingSupportContext } from "../support/RenderingSupportContext";
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
            const renderingSupport = RenderingSupportHelper.newRenderingSupport(this.props, this.props);
            return (
                <RenderingSupportContext.Provider value={renderingSupport}>
                    {this.props.children(renderingSupport)}
                </RenderingSupportContext.Provider>
            );
        }
        else {
            return (<></>);
        }
    }
}

export default RenderingSupportHelper.connect(LocalRerenderingContainer, (props: Props) => props.contextId ? [props.contextId] : undefined, (props: Props) => undefined);
