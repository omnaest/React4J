import React from "react";
import { Node, Renderer } from "../Renderer";

export interface CompositeNode extends Node
{
    elements: Node[];
}

export interface Props
{
    node: CompositeNode;
}

export class Composite extends React.Component<Props, {}>
{
    public static TYPE: string = "COMPOSITE";

    public render(): JSX.Element
    {
        return (
            <>
                {this.props.node.elements.map((node) => Renderer.render(node))}
            </>
        );
    }
}
