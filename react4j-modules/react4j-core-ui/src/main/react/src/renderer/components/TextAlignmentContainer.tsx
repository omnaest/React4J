import React from "react";
import { Node, Renderer } from "../Renderer";

export interface TextAlignmentContainerNode extends Node
{
    content: Node;
    horizontalAlignment: "left" | "center" | "right";
    verticalAlignment: "baseline" | "top" | "middle" | "bottom" | "text-top" | "text-bottom";
    nowrap: boolean;
    ellipsis: boolean;
}

export interface Props
{
    node: TextAlignmentContainerNode;
}

export class TextAlignmentContainer extends React.Component<Props, {}>
{
    public static TYPE: string = "TEXTALIGNMENTCONTAINER";

    public render(): JSX.Element
    {
        const ellipsisClass = this.props.node.ellipsis ? " text-truncate" : "";
        const nowrapClass = this.props.node.nowrap ? " text-nowrap" : "";
        const verticalAlignClass = this.props.node.verticalAlignment ? "align-" + this.props.node.verticalAlignment : "";
        const horizontalAlignClass = this.props.node.horizontalAlignment ? "text-" + this.props.node.horizontalAlignment : "";
        return (
            <span className={ellipsisClass + nowrapClass + verticalAlignClass + horizontalAlignClass}>
                {Renderer.render(this.props.node.content)}
            </span>
        );
    }
}
