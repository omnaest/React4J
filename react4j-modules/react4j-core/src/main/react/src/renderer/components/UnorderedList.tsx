import React from "react";
import { Node, Renderer } from "../Renderer";
import { I18nTextValue, I18nRenderer } from "./I18nText";

export interface UnorderedListNode extends Node
{
    elements: Node[];
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
            <ul className="list-unstyled">
                {
                    this.props.node.elements.map(element =>
                        <li className="list-item">
                            {Renderer.render(element)}
                        </li>
                    )
                }
            </ul>
        );
    }
}