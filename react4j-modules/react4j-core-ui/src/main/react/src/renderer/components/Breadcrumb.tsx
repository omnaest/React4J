
import React from "react";
import { Node } from "../Renderer";
import { I18nRenderer, I18nTextValue } from "./I18nText";
import { Breadcrumb as BSBreadcrumb } from "react-bootstrap";

export interface BreadcrumbEntryNode {
    text: I18nTextValue;
    link: string;
    linkedId: string;
    active: boolean;
}

export interface BreadcrumbNode extends Node {
    entries: BreadcrumbEntryNode[];
}

export interface Props {
    node: BreadcrumbNode;
}

export class Breadcrumb extends React.Component<Props, {}> {
    public static TYPE: string = "BREADCRUMB";

    public render(): JSX.Element {
        return (
            <BSBreadcrumb>
                {this.props.node.entries.map((entry, index) => (
                    <BSBreadcrumb.Item key={index} active={entry.active} href={entry.link}>
                        {I18nRenderer.render(entry.text)}
                    </BSBreadcrumb.Item>
                ))}
            </BSBreadcrumb>
        );
    }
}
