import React from "react";
import { Node, Renderer } from "../Renderer";
import { I18nTextValue, I18nRenderer } from "./I18nText";

export interface TableNode extends Node
{
    columnTitles: I18nTextValue[];
    rows: TableRow[];
}

export interface TableRow
{
    cells: TableCell[];
}

export interface TableCell
{
    content: Node;
}

export interface Props
{
    node: TableNode;
}

export class Table extends React.Component<Props, {}>
{
    public static TYPE: string = "TABLE";

    public render(): JSX.Element
    {
        return (
            <table className="table">
                <thead>
                    <tr>
                        {
                            this.props.node.columnTitles.map((title, index) => (
                                <th key={index} scope="col">{I18nRenderer.render(title)}</th>
                            ))
                        }
                    </tr>
                </thead>
                <tbody>
                    {
                        this.props.node.rows.map((row, index) => (
                            <tr key={index}>
                                {
                                    row.cells.map((cell, cellIndex) => (
                                        <td key={cellIndex}>{Renderer.render(cell.content)}</td>
                                    ))
                                }
                            </tr>
                        ))
                    }
                </tbody>
            </table>
        );
    }
}
