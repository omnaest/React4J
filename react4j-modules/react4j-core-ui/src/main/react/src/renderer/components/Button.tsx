
import React from "react";
import { Node, RenderingSupport } from "../Renderer";
import { RenderingSupportContext } from "../support/RenderingSupportContext";
import { Handler, HandlerFactory } from "../handler/Handler";
import { I18nRenderer, I18nTextValue } from "./I18nText";
import { Button as BSButton } from "react-bootstrap";

export interface ButtonNode extends Node {
    name: I18nTextValue;
    style: string;
    /** Accessible name announced instead of the visible text (see Button.withAriaLabel). */
    ariaLabel?: string;
    onClick?: Handler;
}

export interface Props {
    node: ButtonNode;
}

export class Button extends React.Component<Props, {}> {
    public static TYPE: string = "BUTTON";
    public static contextType = RenderingSupportContext;

    private onClick = (event: React.MouseEvent<HTMLButtonElement, MouseEvent>) => {
        event.preventDefault();

    };

    public render(): JSX.Element {
        const renderingSupport = this.context as RenderingSupport | undefined;
        return (
            <BSButton
                variant={(this.props.node.style ? " btn-" + this.props.node.style : "")}
                aria-label={this.props.node.ariaLabel || undefined}
                //  className={"btn btn-md donate-button" + (this.props.node.style ? " btn-" + this.props.node.style : "")}
                onClick={this.props.node.onClick ? HandlerFactory.onClick(this.props.node.onClick as Handler, renderingSupport?.uiContextAccessor, renderingSupport?.nodeContextAccessor) : undefined}
            >{I18nRenderer.render(this.props.node.name)}</BSButton>
        );
    }
}
