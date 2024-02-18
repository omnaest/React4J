import React from "react";
import { Node } from "../Renderer";
import { I18nRenderer, I18nTextValue } from "./I18nText";

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
        else if (this.props.node.level === 2)
        {
            return (
                <h2>{I18nRenderer.render(this.props.node.text)}</h2>
            );
        }
        else if (this.props.node.level === 3)
        {
            return (
                <h3>{I18nRenderer.render(this.props.node.text)}</h3>
            );
        }
        else if (this.props.node.level === 4)
        {
            return (
                <h4>{I18nRenderer.render(this.props.node.text)}</h4>
            );
        }
        else if (this.props.node.level === 5)
        {
            return (
                <h5>{I18nRenderer.render(this.props.node.text)}</h5>
            );
        }
        else if (this.props.node.level === 6)
        {
            return (
                <h6>{I18nRenderer.render(this.props.node.text)}</h6>
            );
        }
        else
        {
            return (<></>);
        }
    }
}
