import React from "react";
import { Node } from "../Renderer";
import { I18nRenderer, I18nTextValue } from "./I18nText";

export interface AnkerButtonNode extends Node
{
    text: I18nTextValue;
    link: string;
    style: string;
    page: "SELF" | "BLANK";
}

export interface Props
{
    node: AnkerButtonNode;
}

export class AnkerButton extends React.Component<Props, {}>
{
    public static TYPE: string = "ANKERBUTTON";

    public render(): JSX.Element
    {
        return (
            <a
                href={this.props.node.link}
                target={this.props.node.page === "SELF" ? "_self" : "_blank"}
                className={"btn btn-md" + (this.props.node.style ? " btn-" + this.props.node.style : "")}
                role="button"
            >{I18nRenderer.render(this.props.node.text)}</a>
        );
    }
}
