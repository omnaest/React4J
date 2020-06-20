
import React from "react";
import { Node } from "../Renderer";
import { I18nTextValue, I18nRenderer } from "./I18nText";
import { Handler, HandlerFactory } from "../handler/Handler";

export interface ButtonNode extends Node
{
    name: I18nTextValue;
    style: string;
    onClick?: Handler;
}

export interface Props
{
    node: ButtonNode;
}

export class Button extends React.Component<Props, {}>
{
    public static TYPE: string = "BUTTON";

    private onClick = (event: React.MouseEvent<HTMLButtonElement, MouseEvent>) =>
    {
        event.preventDefault();

    };

    public render(): JSX.Element
    {
        return (
            <button
                type="button"
                className={"btn btn-md donate-button" + (this.props.node.style ? " btn-" + this.props.node.style : "")}
                onClick={HandlerFactory.onClick(this.props.node.onClick as Handler)}
            >{I18nRenderer.render(this.props.node.name)}</button>
        );
    }
}
