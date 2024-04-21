import React from "react";
import { Node, Renderer, RenderingSupport } from "../Renderer";

export interface ListViewElementNode extends Node {
    content: Node;
}

export interface Props {
    node: ListViewElementNode;
    renderingSupport?: RenderingSupport;
}

export class ListViewElement extends React.Component<Props, {}> {
    public static TYPE: string = "LISTVIEWELEMENT";

    public render(): JSX.Element {
        return Renderer.render(this.props.node.content, this.props.renderingSupport);
    }
}
