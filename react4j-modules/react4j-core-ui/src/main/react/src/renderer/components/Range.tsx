import React from "react";
import { Node } from "../Renderer";
import { I18nRenderer, I18nTextValue } from "./I18nText";

export interface RangeNode extends Node
{
    label: I18nTextValue;
    min: string;
    max: string;
    step: string;
    disabled: boolean;
}

export interface Props
{
    node: RangeNode;
}

export class Range extends React.Component<Props, {}>
{
    public static TYPE: string = "RANGE";

    public render(): JSX.Element
    {
        return (
            <>
                <label htmlFor="range" className="form-label">{I18nRenderer.render(this.props.node.label)}</label>
                <input type="range"
                    id="range"
                    className="form-range"
                    min={this.props.node.min}
                    max={this.props.node.max}
                    step={this.props.node.step}
                    disabled={this.props.node.disabled === true}
                ></input>
            </>
        );
    }
}
