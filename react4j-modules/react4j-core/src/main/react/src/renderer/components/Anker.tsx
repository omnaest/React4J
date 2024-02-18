import React from "react";
import { Node } from "../Renderer";
import { I18nRenderer, I18nTextValue } from "./I18nText";

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
        const isSelfPage = this.props.node.page === "SELF";
        return (
            <a
                href={this.props.node.link}
                target={isSelfPage ? "_self" : "_blank"}
                title={I18nRenderer.render(this.props.node.title)}
            >{I18nRenderer.render(this.props.node.text)}</a>
        );
    }
}
