
import React from "react";
import { Node, RenderingSupport } from "../Renderer";
import { RenderingSupportContext } from "../support/RenderingSupportContext";
import { I18nRenderer, I18nTextValue } from "./I18nText";
import { Handler, HandlerFactory } from "../handler/Handler";
import { Pagination as BSPagination } from "react-bootstrap";

export interface PaginationItemNode {
    label: I18nTextValue;
    active: boolean;
    disabled: boolean;
    onClick?: Handler;
}

export interface PaginationNode extends Node {
    entries: PaginationItemNode[];
}

export interface Props {
    node: PaginationNode;
}

export class Pagination extends React.Component<Props, {}> {
    public static TYPE: string = "PAGINATION";
    public static contextType = RenderingSupportContext;

    public render(): JSX.Element {
        const renderingSupport = this.context as RenderingSupport | undefined;
        return (
            <BSPagination>
                {this.props.node.entries.map((entry, index) => (
                    <BSPagination.Item
                        key={index}
                        active={entry.active}
                        disabled={entry.disabled}
                        onClick={entry.onClick ? HandlerFactory.onClick(entry.onClick as Handler, renderingSupport?.uiContextAccessor, renderingSupport?.nodeContextAccessor) : undefined}
                    >
                        {I18nRenderer.render(entry.label)}
                    </BSPagination.Item>
                ))}
            </BSPagination>
        );
    }
}
