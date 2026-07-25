
import React from "react";
import { Node, RenderingSupport } from "../Renderer";
import { RenderingSupportContext } from "../support/RenderingSupportContext";
import { Handler, HandlerFactory } from "../handler/Handler";
import { I18nRenderer, I18nTextValue } from "./I18nText";
import { ToggleButton as BSToggleButton } from "react-bootstrap";

export interface ToggleButtonNode extends Node {
    text: I18nTextValue;
    style: string;
    pressed: boolean;
    onChange?: Handler;
}

export interface Props {
    node: ToggleButtonNode;
}

export class ToggleButton extends React.Component<Props, {}> {
    public static TYPE: string = "TOGGLEBUTTON";
    public static contextType = RenderingSupportContext;

    public render(): JSX.Element {
        const node = this.props.node;
        const renderingSupport = this.context as RenderingSupport | undefined;

        return (
            <BSToggleButton
                id={"togglebutton-" + (node.target?.join("-") ?? "0")}
                type="checkbox"
                variant={node.style || "primary"}
                value="1"
                checked={node.pressed}
                onChange={node.onChange ? () => HandlerFactory.handleEvent(node.onChange as Handler, renderingSupport?.uiContextAccessor, renderingSupport?.nodeContextAccessor) : () => { }}
            >
                {I18nRenderer.render(node.text)}
            </BSToggleButton>
        );
    }
}
