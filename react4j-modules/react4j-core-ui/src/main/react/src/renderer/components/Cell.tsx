import React from "react";
import { Node, Renderer } from "../Renderer";

export interface CellNode extends Node
{
    colspan: number;
    content: Node;
}

export interface Props
{
    node: CellNode;
}

export class Cell extends React.Component<Props, {}>
{
    public static TYPE: string = "CELL";

    public render(): JSX.Element
    {
        return (
            <div className={"column col-md-" + this.props.node.colspan}>
                {Renderer.render(this.props.node.content)}
            </div>
        );
    }
}
