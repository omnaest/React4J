import React from "react";
import { Node, Renderer } from "../Renderer";
import { I18nRenderer, I18nTextValue } from "./I18nText";
import { Image, ImageNode } from "./Image";

export interface CardNode extends Node
{
    featuredTitle?: I18nTextValue;
    title?: I18nTextValue;
    subTitle?: I18nTextValue;
    locator?: string;
    content?: Node;
    image?: ImageNode;
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
        const image = this.props.node?.image && (<Image className="card-img-top" node={this.props.node.image}></Image>);
        const featuredTitle = this.props.node?.featuredTitle && (<h5 className="card-header">{I18nRenderer.render(this.props.node.featuredTitle)}</h5>);
        const title = this.props.node?.title && (<p><h4 className="card-title">{I18nRenderer.render(this.props.node.title)}</h4></p>);
        const subtitle = this.props.node?.subTitle && (<p><h6 className="card-subtitle mb-2 text-muted">{I18nRenderer.render(this.props.node.subTitle)}</h6></p>);
        return (
            <div id={this.props.node.locator} className="card">
                {featuredTitle}
                {image}
                <div className="card-body">
                    {title}
                    {subtitle}
                    <div className={"card-inner-body" + (this.props.node.adjust ? " width-max-content" : "")}>
                        {this.props.node.content ? Renderer.render(this.props.node.content) : <></>}
                    </div>
                </div>
            </div >
        );
    }
}
