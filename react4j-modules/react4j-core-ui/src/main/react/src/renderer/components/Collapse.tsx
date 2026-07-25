
import React from "react";
import { Node, Renderer } from "../Renderer";
import { I18nRenderer, I18nTextValue } from "./I18nText";
import { Button as BSButton, Collapse as BSCollapse } from "react-bootstrap";

export interface CollapseNode extends Node {
    content: Node;
    toggleLabel: I18nTextValue;
    initiallyOpen: boolean;
}

export interface Props {
    node: CollapseNode;
}

export interface State {
    open: boolean;
}

export class Collapse extends React.Component<Props, State> {
    public static TYPE: string = "COLLAPSE";

    constructor(props: Props) {
        super(props);

        this.state = {
            open: props.node.initiallyOpen
        };
    }

    public render(): JSX.Element {
        const node = this.props.node;

        return (
            <>
                <BSButton
                    onClick={() => this.setState({ open: !this.state.open })}
                    aria-controls="collapse-content"
                    aria-expanded={this.state.open}
                >
                    {I18nRenderer.render(node.toggleLabel)}
                </BSButton>
                <BSCollapse in={this.state.open}>
                    <div>{Renderer.render(node.content)}</div>
                </BSCollapse>
            </>
        );
    }
}
