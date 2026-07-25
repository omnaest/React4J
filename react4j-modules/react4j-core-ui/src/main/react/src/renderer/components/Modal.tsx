
import React from "react";
import { Node, Renderer, RenderingSupport } from "../Renderer";
import { RenderingSupportContext } from "../support/RenderingSupportContext";
import { Handler, HandlerFactory } from "../handler/Handler";
import { I18nRenderer, I18nTextValue } from "./I18nText";
import { Modal as BSModal } from "react-bootstrap";

export interface ModalNode extends Node {
    title: I18nTextValue;
    content: Node;
    footer?: Node;
    visible: boolean;
    size?: string;
    centered: boolean;
    onClose?: Handler;
}

export interface Props {
    node: ModalNode;
}

export class Modal extends React.Component<Props, {}> {
    public static TYPE: string = "MODAL";
    public static contextType = RenderingSupportContext;

    public render(): JSX.Element {
        const node = this.props.node;
        const renderingSupport = this.context as RenderingSupport | undefined;

        return (
            <BSModal
                show={node.visible}
                onHide={node.onClose ? () => HandlerFactory.handleEvent(node.onClose as Handler, renderingSupport?.uiContextAccessor, renderingSupport?.nodeContextAccessor) : () => { }}
                size={node.size ? (node.size as "sm" | "lg" | "xl") : undefined}
                centered={node.centered}
            >
                <BSModal.Header closeButton>
                    <BSModal.Title>{I18nRenderer.render(node.title)}</BSModal.Title>
                </BSModal.Header>
                <BSModal.Body>
                    {Renderer.render(node.content)}
                </BSModal.Body>
                {node.footer ? (
                    <BSModal.Footer>
                        {Renderer.render(node.footer)}
                    </BSModal.Footer>
                ) : <></>}
            </BSModal>
        );
    }
}
