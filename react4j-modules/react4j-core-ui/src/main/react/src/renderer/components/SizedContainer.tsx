
import React from "react";
import { Node, Renderer } from "../Renderer";

export interface SizedContainerNode extends Node
{
    height: string;
    width: string;
    content: Node;
}

export interface Props
{
    node: SizedContainerNode;
}

export class SizedContainer extends React.Component<Props, {}>
{
    public static TYPE: string = "SIZEDCONTAINER";

    public render(): JSX.Element
    {
        return (
            <div
                style={{
                    height: this.props.node.height,
                    width: this.props.node.width
                }}
            >{Renderer.render(this.props.node.content)}</div>
        );
    }
}
