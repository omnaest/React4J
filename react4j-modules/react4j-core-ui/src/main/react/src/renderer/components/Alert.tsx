
import React from "react";
import { Node, Renderer } from "../Renderer";
import { Alert as BSAlert } from "react-bootstrap";

export interface AlertNode extends Node {
    content: Node;
    style: string;
    dismissible: boolean;
}

export interface Props {
    node: AlertNode;
}

export interface State {
    visible: boolean;
}

export class Alert extends React.Component<Props, State> {
    public static TYPE: string = "ALERT";

    constructor(props: Props) {
        super(props);

        this.state = {
            visible: true
        };
    }

    public render(): JSX.Element {
        if (this.state.visible) {
            return (
                <BSAlert
                    variant={this.props.node.style}
                    dismissible={this.props.node.dismissible}
                    show={this.state.visible}
                    onClose={() => this.setState({ visible: false })}
                >
                    {Renderer.render(this.props.node.content)}
                </BSAlert>
            );
        }
        else {
            return <></>;
        }
    }
}
