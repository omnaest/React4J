
import React from "react";
import { Node, Renderer } from "../Renderer";
import { I18nRenderer, I18nTextValue } from "./I18nText";
import { Figure as BSFigure } from "react-bootstrap";

export interface FigureNode extends Node {
    image: Node;
    caption: I18nTextValue;
}

export interface Props {
    node: FigureNode;
}

export class Figure extends React.Component<Props, {}> {
    public static TYPE: string = "FIGURE";

    public render(): JSX.Element {
        return (
            <BSFigure>
                {Renderer.render(this.props.node.image)}
                <BSFigure.Caption>{I18nRenderer.render(this.props.node.caption)}</BSFigure.Caption>
            </BSFigure>
        );
    }
}
