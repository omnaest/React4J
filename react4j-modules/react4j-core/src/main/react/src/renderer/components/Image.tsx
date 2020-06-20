
import React from "react";
import { Node } from "../Renderer";
import { I18nTextValue, I18nRenderer } from "./I18nText";

export interface ImageNode extends Node
{
    image: string;
    name: I18nTextValue;
}

export interface Props
{
    node: ImageNode;
}

export class Image extends React.Component<Props, {}>
{
    public static TYPE: string = "IMAGE";

    public render(): JSX.Element
    {
        return (
            <img
                alt={I18nRenderer.render(this.props.node.name)}
                src={"/images/" + this.props.node.image}
                width="100%"
            />
        );
    }
}
