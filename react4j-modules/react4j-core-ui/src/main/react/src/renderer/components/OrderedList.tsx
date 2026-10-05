import React from "react";
import { Node, Renderer } from "../Renderer";

export interface OrderedListNode extends Node
{
    elements: Node[];
    startNumber: number;
}

export interface Props
{
    node: OrderedListNode;
}

export class OrderedList extends React.Component<Props, {}>
{
    public static TYPE: string = "ORDEREDLIST";

    public render(): JSX.Element
    {
        const startNumber = this.props.node.startNumber;
        // start is only set when it differs from the default of 1, so the default list carries no start attribute
        const start = startNumber !== undefined && startNumber !== null && startNumber !== 1 ? startNumber : undefined;
        return (
            <ol start={start}>
                {
                    this.props.node.elements.map((element, index) =>
                        <li key={index} className={"list-item"}>
                            {Renderer.render(element)}
                        </li>
                    )
                }
            </ol>
        );
    }
}
