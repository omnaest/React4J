import React from "react";
import { Node, Renderer, RenderingSupport } from "../Renderer";
import { I18nRenderer, I18nTextValue } from "./I18nText";
import { Image, ImageNode } from "./Image";
import { Card as BSCard } from "react-bootstrap";
import { ValueNode, ValueRenderer } from "../support/ValueRenderer";
import "./Card.css";

export interface CardNode extends Node {
    featuredTitle?: ValueNode;
    title?: ValueNode;
    subTitle?: I18nTextValue;
    locator?: string;
    header?: Node;
    content?: Node;
    footer?: Node;
    image?: ImageNode;
    adjust?: boolean;
    fullHeight?: boolean;
}

export interface Props {
    node: CardNode;
    renderingSupport?: RenderingSupport;
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
        const featuredTitle = this.props.node?.featuredTitle && ValueRenderer.render(this.props.node.featuredTitle, this.props.renderingSupport);
        // h4/h6 and the content div are emitted as siblings of BSCard.Text rather than inside it. BSCard.Text
        // renders a <p>, and a heading, div or table nested in a <p> is invalid HTML: the browser closes the
        // paragraph early and re-parents the rest, so the DOM silently stops matching the tree rendered here.
        const title = this.props.node?.title && (<h4 className="card-title">{ValueRenderer.render(this.props.node.title, this.props.renderingSupport)}</h4>);
        const subtitle = this.props.node?.subTitle && (<h6 className="card-subtitle mb-2 text-muted">{I18nRenderer.render(this.props.node.subTitle)}</h6>);
        return (
            /* h-100 is Bootstrap's own utility; a .card is already a vertical flex container whose .card-body grows,
               so filling the parent height is what puts a footer at the BOTTOM of the panel rather than directly
               under the content. */
            <BSCard id={this.props.node.locator} className={this.props.node.fullHeight ? "h-100" : undefined}>
                {this.props.node.header ? <BSCard.Header>{Renderer.render(this.props.node.header)}</BSCard.Header> : <></>}
                <BSCard.Body>
                    {this.renderImage(this.props.node?.image)}
                    {featuredTitle ? <BSCard.Title>{featuredTitle}</BSCard.Title> : <></>}
                    {title}
                    {subtitle}
                    <div className={"card-inner-body" + (this.props.node.adjust ? " width-max-content" : "")}>
                        {this.props.node.content ? Renderer.render(this.props.node.content) : <></>}
                    </div>
                </BSCard.Body>
                {this.props.node.footer ? <BSCard.Footer>{Renderer.render(this.props.node.footer)}</BSCard.Footer> : <></>}
            </BSCard>
        );
    }
}
