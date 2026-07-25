
import React from "react";
import { Node, Renderer } from "../Renderer";
import { I18nRenderer, I18nTextValue } from "./I18nText";
import { OverlayTrigger, Tooltip as BSTooltip } from "react-bootstrap";
import { Placement } from "react-bootstrap/esm/types";

export interface TooltipNode extends Node {
    content: Node;
    text: I18nTextValue;
    placement?: string;
}

export interface Props {
    node: TooltipNode;
}

export class Tooltip extends React.Component<Props, {}> {
    public static TYPE: string = "TOOLTIP";

    public render(): JSX.Element {
        const node = this.props.node;

        return (
            <OverlayTrigger
                placement={node.placement ? (node.placement as Placement) : "top"}
                overlay={<BSTooltip>{I18nRenderer.render(node.text)}</BSTooltip>}
            >
                <span>{Renderer.render(node.content)}</span>
            </OverlayTrigger>
        );
    }
}
