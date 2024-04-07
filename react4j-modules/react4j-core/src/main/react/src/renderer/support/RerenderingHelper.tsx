import React from "react";
import { RenderingSupport } from '../Renderer';
import LocalRerenderingContainer from "../components/LocalRerenderingContainer";

export class RerenderingHelper {
    public static wrapInRerenderingContainer(uiContextId: string, component: (renderingSupport?: RenderingSupport) => React.JSX.Element | undefined): React.ReactNode {
        if (uiContextId) {
            return (
                <LocalRerenderingContainer contextId={uiContextId}>
                    {component}
                </LocalRerenderingContainer>
            );
        }
        else {
            return component(undefined);
        }
    }
}

