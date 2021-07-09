import React from "react";
import { Node, Renderer } from "../Renderer";
import { I18nTextValue, I18nRenderer } from "./I18nText";

export interface AnkerNode extends Node
{
    text: I18nTextValue;
    title: I18nTextValue;
    link: string;
    page: "SELF" | "BLANK";
}

export interface Props
{
    node: AnkerNode;
}

export class Anker extends React.Component<Props, {}>
{
    public static TYPE: string = "ANKER";

    public render(): JSX.Element
    {
        return (
            <a
                href={this.props.node.link}
                target={this.props.node.page === "SELF" ? "_self" : "_blank"}
                title={I18nRenderer.render(this.props.node.title)}
            >{I18nRenderer.render(this.props.node.text)}</a>
        );
    }
}
