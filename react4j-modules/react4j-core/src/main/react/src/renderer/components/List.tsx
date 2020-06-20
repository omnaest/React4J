import React from "react";
import { Node } from "../Renderer";
import { I18nTextValue, I18nRenderer } from "./I18nText";

export interface ListNode extends Node
{
    elements: ListElement[];
}

export interface ListElement
{
    text: I18nTextValue;
    icon?: string;
}

export interface Props
{
    node: ListNode;
}

export class List extends React.Component<Props, {}>
{
    public render(): JSX.Element
    {
        return (
            <ul className="list-unstyled">
                {
                    this.props.node.elements.map(element =>
                        <li className="list-item">
                            {element.icon ? <i className={"fas fa-" + element.icon}>&nbsp;</i> : <></>}
                            {I18nRenderer.render(element.text)}
                        </li>
                    )
                }
            </ul>
        );
    }
}
