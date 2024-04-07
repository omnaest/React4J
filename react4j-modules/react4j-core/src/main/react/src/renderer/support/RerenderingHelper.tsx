import React from "react";
import { RenderingSupport } from '../Renderer';
import LocalRerenderingContainer from "../components/LocalRerenderingContainer";

export class RerenderingHelper {
    public static wrapIntoRerenderingContainer(uiContextIds: string[] | undefined, component: (renderingSupport?: RenderingSupport) => JSX.Element): JSX.Element {
        if (uiContextIds) {
            return (
                <LocalRerenderingContainer contextId={uiContextIds}>
                    {component}
                </LocalRerenderingContainer>
            );
        }
        else {
            return component(undefined);
        }
    }
}

