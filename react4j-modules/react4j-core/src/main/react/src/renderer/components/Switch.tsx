import React from "react";
import { Node } from "../Renderer";
import { I18nRenderer, I18nTextValue } from "./I18nText";

export interface SwitchNode extends Node {
    label: I18nTextValue;
    disabled: boolean;
}

export interface Props {
    node: SwitchNode;
}

export class Switch extends React.Component<Props, {}> {
    public static TYPE: string = "SWITCH";

    public render(): JSX.Element {
        return (
            <>
                <div className="form-check form-switch">
                    <input
                        id="switch"
                        className="form-check-input"
                        type="checkbox"
                        role="switch"
                        disabled={this.props.node.disabled}
                    />
                    <label
                        className="form-check-label"
                        htmlFor="switch"
                    >{I18nRenderer.render(this.props.node.label)}</label>
                </div>
            </>
        );
    }
}
