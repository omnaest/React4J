
import React from "react";
import { Node } from "../Renderer";
import { Handler, HandlerFactory } from "../handler/Handler";
import { I18nRenderer, I18nTextValue } from "./I18nText";
import { Button as BSButton } from "react-bootstrap";

export interface ButtonNode extends Node {
    name: I18nTextValue;
    style: string;
    onClick?: Handler;
}

export interface Props {
    node: ButtonNode;
}

export class Button extends React.Component<Props, {}> {
    public static TYPE: string = "BUTTON";

    private onClick = (event: React.MouseEvent<HTMLButtonElement, MouseEvent>) => {
        event.preventDefault();

    };

    public render(): JSX.Element {
        return (
            <BSButton
                variant={(this.props.node.style ? " btn-" + this.props.node.style : "")}
                //  className={"btn btn-md donate-button" + (this.props.node.style ? " btn-" + this.props.node.style : "")}
                onClick={HandlerFactory.onClick(this.props.node.onClick as Handler)}
            >{I18nRenderer.render(this.props.node.name)}</BSButton>
        );
    }
}
