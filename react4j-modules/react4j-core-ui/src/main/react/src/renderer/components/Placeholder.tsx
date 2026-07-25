
import React from "react";
import { Node } from "../Renderer";
import { Placeholder as BSPlaceholder } from "react-bootstrap";

export interface PlaceholderNode extends Node {
    style: string;
    size: string;
    columns: number;
    animation?: string;
}

export interface Props {
    node: PlaceholderNode;
}

export class Placeholder extends React.Component<Props, {}> {
    public static TYPE: string = "PLACEHOLDER";

    public render(): JSX.Element {
        const content = (
            <BSPlaceholder
                xs={this.props.node.columns}
                bg={this.props.node.style}
                size={this.props.node.size as "sm" | "lg" | "xs"}
            />
        );
        if (this.props.node.animation) {
            return (
                <BSPlaceholder as="div" animation={this.props.node.animation as "glow" | "wave"}>
                    {content}
                </BSPlaceholder>
            );
        }
        return content;
    }
}
