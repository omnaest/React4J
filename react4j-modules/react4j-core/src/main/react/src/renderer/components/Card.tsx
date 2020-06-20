import React from "react";
import { Node, Renderer } from "../Renderer";
import { I18nTextValue, I18nRenderer } from "./I18nText";

export interface CardNode extends Node
{
    title: I18nTextValue;
    locator?: string;
    content?: Node;
    adjust?: boolean;
}

export interface Props
{
    node: CardNode;
}

export class Card extends React.Component<Props, {}>
{
    public static TYPE: string = "CARD";

    public render(): JSX.Element
    {
        return (
            <div className="card">
                <h5 id={this.props.node.locator} className="card-header">{I18nRenderer.render(this.props.node.title)}</h5>
                <div className="card-body">
                    <div className={"card-inner-body" + (this.props.node.adjust ? " width-max-content" : "")}>
                        {this.props.node.content ? Renderer.render(this.props.node.content) : <></>}
                    </div>
                </div>
            </div >
        );
    }
}
