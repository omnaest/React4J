import React from "react";
import { Node, Renderer } from "../Renderer";
import { I18nRenderer, I18nTextValue } from "./I18nText";
import { Image, ImageNode } from "./Image";
import { Card as BSCard } from "react-bootstrap";

export interface CardNode extends Node {
    featuredTitle?: I18nTextValue;
    title?: I18nTextValue;
    subTitle?: I18nTextValue;
    locator?: string;
    content?: Node;
    image?: ImageNode;
    adjust?: boolean;
}

export interface Props {
    node: CardNode;
}

export class Card extends React.Component<Props, {}> {
    public static TYPE: string = "CARD";

    private renderImage(imageNode?: ImageNode) {
        return imageNode ? (
            <BSCard.Img
                variant="top"
                src={"/images/" + imageNode.image}
                alt={I18nRenderer.render(imageNode.name)}
                width="100%"
            />
        ) : <></>;
    }

    public render(): JSX.Element {
        const featuredTitle = this.props.node?.featuredTitle && I18nRenderer.render(this.props.node.featuredTitle);
        const title = this.props.node?.title && (<p><h4 className="card-title">{I18nRenderer.render(this.props.node.title)}</h4></p>);
        const subtitle = this.props.node?.subTitle && (<p><h6 className="card-subtitle mb-2 text-muted">{I18nRenderer.render(this.props.node.subTitle)}</h6></p>);
        return (
            <BSCard id={this.props.node.locator}>
                <BSCard.Body>
                    {this.renderImage(this.props.node?.image)}
                    <BSCard.Title>{featuredTitle}</BSCard.Title>
                    <BSCard.Text>
                        {title}
                        {subtitle}
                        <div className={"card-inner-body" + (this.props.node.adjust ? " width-max-content" : "")}>
                            {this.props.node.content ? Renderer.render(this.props.node.content) : <></>}
                        </div>
                    </BSCard.Text>
                </BSCard.Body>
            </BSCard>
        );
    }
}
