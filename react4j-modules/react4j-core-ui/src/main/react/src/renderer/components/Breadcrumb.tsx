
import React from "react";
import { Node, RenderingSupport } from "../Renderer";
import { RenderingSupportContext } from "../support/RenderingSupportContext";
import { I18nRenderer, I18nTextValue } from "./I18nText";
import { Handler, HandlerFactory } from "../handler/Handler";
import { Breadcrumb as BSBreadcrumb } from "react-bootstrap";

export interface BreadcrumbEntryNode {
    text: I18nTextValue;
    link: string;
    linkedId: string;
    active: boolean;
    onClick?: Handler;
}

export interface BreadcrumbNode extends Node {
    entries: BreadcrumbEntryNode[];
    locator?: string;
}

export interface Props {
    node: BreadcrumbNode;
}

export class Breadcrumb extends React.Component<Props, {}> {
    public static TYPE: string = "BREADCRUMB";
    public static contextType = RenderingSupportContext;

    public render(): JSX.Element {
        const renderingSupport = this.context as RenderingSupport | undefined;
        return (
            <BSBreadcrumb id={this.props.node.locator}>
                {this.props.node.entries.map((entry, index) => {
                    // Precedence (frozen at plan-262 Cliff C4): active beats everything; then onClick > link > linkedId.
                    // The handler is wired via linkProps so it lands on the <a>, not the <li> - the "/" separator is a
                    // ::before pseudo-element on the FOLLOWING item, so an <li>-level handler would misroute clicks.
                    const href = entry.link ? entry.link : (entry.linkedId ? "#" + entry.linkedId : undefined);
                    const onClick = entry.onClick
                        ? HandlerFactory.onClick(entry.onClick as Handler, renderingSupport?.uiContextAccessor, renderingSupport?.nodeContextAccessor)
                        : undefined;
                    return (
                        <BSBreadcrumb.Item key={index} active={entry.active} href={href} linkProps={{ onClick: onClick }}>
                            {I18nRenderer.render(entry.text)}
                        </BSBreadcrumb.Item>
                    );
                })}
            </BSBreadcrumb>
        );
    }
}
