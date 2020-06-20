import React from "react";
import { Node, Renderer } from "../Renderer";

export interface ScrollbarContainerNode extends Node
{
    content: Node;
    verticalBoxMode: string;
    horizontalBoxMode: string;
}

export interface Props
{
    node: ScrollbarContainerNode;
}

export class ScrollbarContainer extends React.Component<Props, {}>
{
    public static TYPE: string = "SCROLLBARCONTAINER";

    public render(): JSX.Element
    {
        return (
            <div className={"scrollbar-container " + this.props.node.verticalBoxMode + " " + this.props.node.horizontalBoxMode}>
                <div className={"scrollbar-container-content " + this.props.node.verticalBoxMode + " " + this.props.node.horizontalBoxMode}>
                    {Renderer.render(this.props.node.content)}
                </div>
            </div>
        );
    }
}
