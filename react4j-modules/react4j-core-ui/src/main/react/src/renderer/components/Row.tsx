import React from "react";
import { Node, Renderer } from "../Renderer";
import { CellNode } from "./Cell";

export interface RowNode extends Node
{
    cells: CellNode[];
}

export interface Props
{
    node: RowNode;
}

export class Row extends React.Component<Props, {}>
{
    public static TYPE: string = "ROW";

    public render(): JSX.Element
    {
        return (
            <div className="row">
                {this.props.node.cells.map((cell) => Renderer.render(cell))}
            </div>
        );
    }
}
