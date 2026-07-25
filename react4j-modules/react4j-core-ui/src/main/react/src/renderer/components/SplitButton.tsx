
import React from "react";
import { Node, RenderingSupport } from "../Renderer";
import { RenderingSupportContext } from "../support/RenderingSupportContext";
import { Handler, HandlerFactory } from "../handler/Handler";
import { I18nRenderer, I18nTextValue } from "./I18nText";
import { DropdownItemNode, renderDropdownItem } from "./DropdownItem";
import { SplitButton as BSSplitButton } from "react-bootstrap";

export interface SplitButtonNode extends Node {
    title: I18nTextValue;
    style: string;
    onClick?: Handler;
    items: DropdownItemNode[];
}

export interface Props {
    node: SplitButtonNode;
}

export class SplitButton extends React.Component<Props, {}> {
    public static TYPE: string = "SPLITBUTTON";
    public static contextType = RenderingSupportContext;

    public render(): JSX.Element {
        const node = this.props.node;
        const renderingSupport = this.context as RenderingSupport | undefined;

        return (
            <BSSplitButton
                id={"splitbutton-" + (node.target?.join("-") ?? "0")}
                title={I18nRenderer.render(node.title)}
                variant={node.style || "primary"}
                onClick={node.onClick ? HandlerFactory.onClick(node.onClick as Handler, renderingSupport?.uiContextAccessor, renderingSupport?.nodeContextAccessor) : undefined}
            >
                {node.items.map(renderDropdownItem)}
            </BSSplitButton>
        );
    }
}
