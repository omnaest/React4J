import React from "react";
import { Node, Renderer } from "../Renderer";
import { I18nRenderer, I18nTextValue } from "./I18nText";
import { Toast, ToastContainer } from "react-bootstrap";
import { ToastPosition } from "react-bootstrap/esm/ToastContainer";

export interface ToasterNode extends Node
{
    title: I18nTextValue;
    content: Node;
    style?: string;
    placement?: string;
}

export interface Props
{
    node: ToasterNode;
}

export interface State
{
    visible: boolean;
}

export class Toaster extends React.Component<Props, State>
{
    public static TYPE: string = "TOASTER";

    constructor(props: Props)
    {
        super(props);

        this.state = {
            visible: true
        }
    }

    private onClose(): void
    {
        this.setState({
            visible: false
        });
    }

    public render(): JSX.Element
    {
        if (this.state.visible)
        {
            const node = this.props.node;

            return (
                <ToastContainer
                    position={node.placement ? (node.placement as ToastPosition) : "top-end"}
                    className="p-3"
                >
                    <Toast
                        show={this.state.visible}
                        onClose={() => this.onClose()}
                        bg={node.style || undefined}
                    >
                        <Toast.Header>
                            <strong className="me-auto">{I18nRenderer.render(node.title)}</strong>
                        </Toast.Header>
                        <Toast.Body>
                            {Renderer.render(node.content)}
                        </Toast.Body>
                    </Toast>
                </ToastContainer>
            );
        }
        else
        {
            return <></>;
        }
    }
}
