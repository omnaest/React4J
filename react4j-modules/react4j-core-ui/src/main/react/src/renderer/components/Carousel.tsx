
import React from "react";
import { Node, Renderer } from "../Renderer";
import { I18nRenderer, I18nTextValue } from "./I18nText";
import { Carousel as BSCarousel } from "react-bootstrap";

export interface CarouselItemNode {
    image: Node;
    caption: I18nTextValue;
}

export interface CarouselNode extends Node {
    items: CarouselItemNode[];
    interval: number | null;
    controls: boolean;
    indicators: boolean;
    fade: boolean;
}

export interface Props {
    node: CarouselNode;
}

export class Carousel extends React.Component<Props, {}> {
    public static TYPE: string = "CAROUSEL";

    public render(): JSX.Element {
        const node = this.props.node;

        return (
            <BSCarousel interval={node.interval ?? null} controls={node.controls} indicators={node.indicators} fade={node.fade}>
                {node.items.map((item, index) => (
                    <BSCarousel.Item key={index}>
                        {Renderer.render(item.image)}
                        {item.caption ? <BSCarousel.Caption>{I18nRenderer.render(item.caption)}</BSCarousel.Caption> : null}
                    </BSCarousel.Item>
                ))}
            </BSCarousel>
        );
    }
}
