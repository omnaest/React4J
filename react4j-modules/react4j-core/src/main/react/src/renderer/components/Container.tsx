import React from "react";
import { Node, Renderer } from "../Renderer";
import { Locatable } from "./Locatable";
import { RowNode } from "./Row";

export interface ContainerNode extends Node, Locatable
{
    rows: RowNode[];
    unlimitedColumns: boolean;
}

export interface Props
{
    node: ContainerNode;
}

export class Container extends React.Component<Props, {}>
{
    public static TYPE: string = "CONTAINER";

    public render(): JSX.Element
    {
        return (
            <div
                id={this.props.node.locator}
                className={"container-fluid page-content" + (this.props.node.unlimitedColumns ? " unlimited-columns" : "")}
            >
                {(this.props.node.rows.map((row) => Renderer.render(row)))}
            </div>
        );
    }
}
