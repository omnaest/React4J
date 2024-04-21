import React from "react";
import { Node } from "../Renderer";
import { Icon2, Icon2Node } from "./Icon2";

export interface TreeTableNode extends Node
{
    configuration: TreeTableConfiguration;
}

export interface TreeTableConfiguration
{
    columns: TreeTableColumn[];
}

export interface TreeTableColumn
{
    label: string;
}

export interface Props
{
    node: TreeTableNode;
}

interface InnerNode
{
    label: string;
    isOpen: boolean;
    cells: InnerCell[];
}

interface InnerCell
{
    content: string;
}



export class TreeTable extends React.Component<Props, {}>
{
    public static TYPE: string = "TREETABLE";

    private renderIcon(isOpen: boolean): JSX.Element
    {
        const icon = {
            icon: isOpen ? "caret-down" : "caret-right"
        } as Icon2Node;
        return (
            <Icon2 node={icon} />
        );
    }

    public render(): JSX.Element
    {
        const innerNodes: InnerNode[] = [
            {
                isOpen: true,
                label: "Node1",
                cells: []
            }, {
                isOpen: false,
                label: "Node1.1",
                cells: []
            }
        ];

        return (
            <table className="table" id="table1" >
                <thead>
                    <tr>
                        {
                            this.props.node.configuration?.columns?.map((column) => (
                                <th>{column.label}</th>
                            ))
                        }
                    </tr>
                </thead>
                <tbody>
                    {
                        innerNodes.map((innerNode) => (
                            <tr >
                                <td>{this.renderIcon(innerNode.isOpen)}{innerNode.label}</td>
                                {
                                    innerNode.cells?.map((cell) => (
                                        <td>{cell.content}</td>
                                    ))
                                }
                            </tr>
                        ))
                    }
                </tbody>
            </table >
        );
    }
}
