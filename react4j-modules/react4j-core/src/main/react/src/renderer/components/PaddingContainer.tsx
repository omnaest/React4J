import React from "react";
import { Node, Renderer } from "../Renderer";

export interface PaddingContainerNode extends Node
{
    content: Node;
    vertical: boolean;
    horizontal: boolean;
}

export interface Props
{
    node: PaddingContainerNode;
}

export class PaddingContainer extends React.Component<Props, {}>
{
    public static TYPE: string = "PADDINGCONTAINER";

    public render(): JSX.Element
    {
        return (
            <div
                className={(this.props.node.horizontal ? "px-3" : "") + " " + (this.props.node.vertical ? "py-3" : "")}>
                {Renderer.render(this.props.node.content)}
            </div>
        );
    }
}
