import React from "react";
import { Node, Renderer } from "../Renderer";
import { I18nTextValue, I18nRenderer } from "./I18nText";

export interface TableNode extends Node {
    columnTitles: I18nTextValue[];
    rows: TableRow[];
    options?: TableOptions;
}

export interface TableOptions {
    size: "sm" | "md" | "lg" | "xl" | "xxl";
    responsive: boolean;
}

export interface TableRow {
    cells: TableCell[];
}

export interface TableCell {
    content: Node;
}

export interface Props {
    node: TableNode;
}

export class Table extends React.Component<Props, {}> {
    public static TYPE: string = "TABLE";

    public render(): JSX.Element {
        const tableSizeClassNameSuffix = this.props.node?.options?.size ? "-" + this.props.node?.options?.size : "";
        return this.props.node?.options?.responsive ? (
            <div className={"table-responsive" + tableSizeClassNameSuffix}>
                {this.renderTable()}
            </div>
        ) : this.renderTable();
    }

    public renderTable(): JSX.Element {
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
