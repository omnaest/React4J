
import React from "react";
import { Node, Renderer } from "../Renderer";

export interface RatioContainerNode extends Node
{
    content: Node;
    ratio: "1x1" | "4x3" | "16x9" | "21x9"
}

export interface Props
{
    node: RatioContainerNode;
}

export class RatioContainer extends React.Component<Props, {}>
{
    public static TYPE: string = "RATIOCONTAINER";

    public render(): JSX.Element
    {
        return (
            <div className={"ratio ratio-" + this.props.node.ratio}>
                {Renderer.render(this.props.node.content)}
            </div>
        );
    }
}
