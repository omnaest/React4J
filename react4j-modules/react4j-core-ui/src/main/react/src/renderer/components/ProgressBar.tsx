import React from "react";
import { Node } from "../Renderer";
import { I18nRenderer, I18nTextValue } from "./I18nText";

export interface ProgressBarNode extends Node
{
    text: I18nTextValue;
    min: number;
    max: number;
    value: number;
}

export interface Props
{
    node: ProgressBarNode;
}

export class ProgressBar extends React.Component<Props, {}>
{
    public static TYPE: string = "PROGRESSBAR";

    public render(): JSX.Element
    {
        const ratio = (this.props.node.value / (this.props.node.max - this.props.node.min)) * 100;
        return (
            <div className="progress">
                <div
                    className="progress-bar"
                    role="progressbar"
                    style={{ "width": ratio + "%" }}
                    aria-valuenow={this.props.node.value}
                    aria-valuemin={this.props.node.min}
                    aria-valuemax={this.props.node.max}>{I18nRenderer.render(this.props.node.text)}</div>
            </div>
        );
    }
}
