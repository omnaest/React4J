import React from "react";
import { Node, Renderer } from "../Renderer";
import { I18nTextValue, I18nRenderer } from "./I18nText";

export interface ToasterNode extends Node
{
    title: I18nTextValue;
    content: Node;
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
            return (
                <div
                    className="toast show toast-location"
                    role="alert"
                    aria-live="assertive"
                    aria-atomic="true"
                >
                    <div className="toast-header">
                        <img style={{ display: "none" }} src="..." className="rounded mr-2" alt="..." />
                        <strong className="mr-auto"> {I18nRenderer.render(this.props.node.title)}</strong>
                        <small className="text-muted"></small>
                        <button
                            type="button"
                            className="ml-2 mb-1 close"
                            data-dismiss="toast"
                            aria-label="Close"
                            onClick={() => this.onClose()}
                        >
                            <span aria-hidden="true">&times;</span>
                        </button>
                    </div>
                    <div className="toast-body">
                        {Renderer.render(this.props.node.content)}
                    </div>
                </div>
            );
        }
        else
        {
            return <></>;
        }
    }
}
