import React from "react";
import { RenderingSupport } from '../Renderer';
import LocalRerenderingContainer from "../components/LocalRerenderingContainer";

export class RerenderingHelper {
    public static wrapInRerenderingContainer(uiContextId: string | undefined, component: (renderingSupport?: RenderingSupport) => JSX.Element): JSX.Element {
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

