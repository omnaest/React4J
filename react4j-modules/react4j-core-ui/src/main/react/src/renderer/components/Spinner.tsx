
import React from "react";
import { Node } from "../Renderer";
import { Spinner as BSSpinner } from "react-bootstrap";

export interface SpinnerNode extends Node {
    style: string;
    spinnerType: string;
}

export interface Props {
    node: SpinnerNode;
}

export class Spinner extends React.Component<Props, {}> {
    public static TYPE: string = "SPINNER";

    public render(): JSX.Element {
        return (
            <BSSpinner
                animation={this.props.node.spinnerType as "border" | "grow"}
                variant={this.props.node.style}
            />
        );
    }
}
