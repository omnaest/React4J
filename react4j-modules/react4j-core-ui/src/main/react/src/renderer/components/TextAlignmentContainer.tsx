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

    // The wire carries left|center|right; Bootstrap 5 names the same alignments text-start|text-center|text-end.
    private static horizontalAlignClass(alignment: TextAlignmentContainerNode["horizontalAlignment"]): string
    {
        switch (alignment)
        {
            case "left": return "text-start";
            case "right": return "text-end";
            case "center": return "text-center";
            default: return "";
        }
    }

    public render(): JSX.Element
    {
        const classes = [
            this.props.node.ellipsis ? "text-truncate" : "",
            this.props.node.nowrap ? "text-nowrap" : "",
            this.props.node.verticalAlignment ? "align-" + this.props.node.verticalAlignment : "",
            TextAlignmentContainer.horizontalAlignClass(this.props.node.horizontalAlignment)
        ].filter(token => token).join(" ");
        return (
            <span className={classes}>
                {Renderer.render(this.props.node.content)}
            </span>
        );
    }
}
