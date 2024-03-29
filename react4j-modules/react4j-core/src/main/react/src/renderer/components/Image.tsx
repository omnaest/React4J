
import React from "react";
import { Node } from "../Renderer";
import { I18nRenderer, I18nTextValue } from "./I18nText";

export interface ImageNode extends Node
{
    image: string;
    name: I18nTextValue;
}

export interface Props
{
    node: ImageNode;
    className?: string;
}

export class Image extends React.Component<Props, {}>
{
    public static TYPE: string = "IMAGE";

    public render(): JSX.Element
    {
        return (
            <img
                className={this.props.className}
                alt={I18nRenderer.render(this.props.node.name)}
                src={"/images/" + this.props.node.image}
                width="100%"
            />
        );
    }
}
