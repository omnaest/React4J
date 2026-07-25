
import React from "react";
import { Node, Renderer, RenderingSupport } from "../Renderer";
import { RenderingSupportContext } from "../support/RenderingSupportContext";
import { Handler, HandlerFactory } from "../handler/Handler";
import { I18nRenderer, I18nTextValue } from "./I18nText";
import { Offcanvas as BSOffcanvas } from "react-bootstrap";
import { OffcanvasPlacement } from "react-bootstrap/esm/Offcanvas";

export interface OffcanvasNode extends Node {
    title: I18nTextValue;
    content: Node;
    visible: boolean;
    placement?: string;
    onClose?: Handler;
}

export interface Props {
    node: OffcanvasNode;
}

export class Offcanvas extends React.Component<Props, {}> {
    public static TYPE: string = "OFFCANVAS";
    public static contextType = RenderingSupportContext;

    public render(): JSX.Element {
        const node = this.props.node;
        const renderingSupport = this.context as RenderingSupport | undefined;

        return (
            <BSOffcanvas
                show={node.visible}
                onHide={node.onClose ? () => HandlerFactory.handleEvent(node.onClose as Handler, renderingSupport?.uiContextAccessor, renderingSupport?.nodeContextAccessor) : () => { }}
                placement={node.placement ? (node.placement as OffcanvasPlacement) : "start"}
            >
                <BSOffcanvas.Header closeButton>
                    <BSOffcanvas.Title>{I18nRenderer.render(node.title)}</BSOffcanvas.Title>
                </BSOffcanvas.Header>
                <BSOffcanvas.Body>
                    {Renderer.render(node.content)}
                </BSOffcanvas.Body>
            </BSOffcanvas>
        );
    }
}
