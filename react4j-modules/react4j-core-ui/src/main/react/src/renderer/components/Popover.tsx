
import React from "react";
import { Node, Renderer } from "../Renderer";
import { I18nRenderer, I18nTextValue } from "./I18nText";
import { OverlayTrigger, Popover as BSPopover } from "react-bootstrap";
import { Placement } from "react-bootstrap/esm/types";
import { OverlayTriggerType } from "react-bootstrap/esm/OverlayTrigger";

export interface PopoverNode extends Node {
    content: Node;
    body: Node;
    title?: I18nTextValue;
    placement?: string;
    trigger?: string;
}

export interface Props {
    node: PopoverNode;
}

export class Popover extends React.Component<Props, {}> {
    public static TYPE: string = "POPOVER";

    public render(): JSX.Element {
        const node = this.props.node;

        return (
            <OverlayTrigger
                trigger={node.trigger ? (node.trigger as OverlayTriggerType) : "click"}
                placement={node.placement ? (node.placement as Placement) : "top"}
                overlay={
                    <BSPopover>
                        {node.title ? <BSPopover.Header>{I18nRenderer.render(node.title)}</BSPopover.Header> : <></>}
                        <BSPopover.Body>{Renderer.render(node.body)}</BSPopover.Body>
                    </BSPopover>
                }
            >
                <span>{Renderer.render(node.content)}</span>
            </OverlayTrigger>
        );
    }
}
