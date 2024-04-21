import React from "react";
import { Node, Renderer } from "../Renderer";

export interface UnorderedListNode extends Node
{
    elements: Node[];
    enableBulletPoints: boolean;
}

export interface Props
{
    node: UnorderedListNode;
}

export class UnorderedList extends React.Component<Props, {}>
{
    public static TYPE: string = "UNORDEREDLIST";

    public render(): JSX.Element
    {
        return (
            <ul className={this.props.node.enableBulletPoints ? "" : "list-unstyled"}>
                {
                    this.props.node.elements.map(element =>
                        <li className={"list-item"}>
                            {Renderer.render(element)}
                        </li>
                    )
                }
            </ul>
        );
    }
}
