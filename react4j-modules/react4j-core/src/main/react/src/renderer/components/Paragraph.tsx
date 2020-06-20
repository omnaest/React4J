import React from "react";
import { Node, Renderer } from "../Renderer";
import { I18nTextValue, I18nRenderer } from "./I18nText";

export interface ParagraphNode extends Node
{
    elements: Node[];
}

export interface Props
{
    node: ParagraphNode;
}

export class Paragraph extends React.Component<Props, {}>
{
    public static TYPE: string = "PARAGRAPH";

    public render(): JSX.Element
    {
        return (
            <>
                {this.props.node.elements.map((element, index) => (
                    <p key={index}>{Renderer.render(element)}</p>
                ))}
            </>
        );
    }
}
