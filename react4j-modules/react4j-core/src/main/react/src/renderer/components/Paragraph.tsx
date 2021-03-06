import React from "react";
import { Node, Renderer } from "../Renderer";

export interface ParagraphNode extends Node
{
    elements: Node[];
    bold: boolean;
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
                <p className={this.props.node.bold ? "font-weight-bold" : ""}>
                    {this.props.node.elements.map((element, index) => (
                        <span key={index}>{Renderer.render(element)}</span>
                    ))}
                </p>
            </>
        );
    }
}
