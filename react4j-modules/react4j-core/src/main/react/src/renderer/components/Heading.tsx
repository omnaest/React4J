import React from "react";
import { Node, Renderer } from "../Renderer";
import { I18nTextValue, I18nRenderer } from "./I18nText";

export interface HeadingNode extends Node
{
    level: number;
    text: I18nTextValue;
}

export interface Props
{
    node: HeadingNode;
}

export class Heading extends React.Component<Props, {}>
{
    public static TYPE: string = "HEADING";

    public render(): JSX.Element
    {
        if (this.props.node.level === 1)
        {
            return (
                <h1>{I18nRenderer.render(this.props.node.text)}</h1>
            );
        }
        else
        {
            return (<></>);
        }
    }
}
