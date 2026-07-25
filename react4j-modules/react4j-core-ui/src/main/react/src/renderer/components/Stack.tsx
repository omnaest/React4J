
import React from "react";
import { Node, Renderer } from "../Renderer";
import { Stack as BSStack } from "react-bootstrap";

export interface StackNode extends Node {
    content: Node;
    direction: string;
    gap: number;
}

export interface Props {
    node: StackNode;
}

export class Stack extends React.Component<Props, {}> {
    public static TYPE: string = "STACK";

    public render(): JSX.Element {
        return (
            <BSStack direction={this.props.node.direction as "horizontal" | "vertical"} gap={this.props.node.gap}>
                {Renderer.render(this.props.node.content)}
            </BSStack>
        );
    }
}
