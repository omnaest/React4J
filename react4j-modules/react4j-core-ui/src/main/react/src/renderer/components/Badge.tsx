
import React from "react";
import { Node } from "../Renderer";
import { I18nRenderer, I18nTextValue } from "./I18nText";
import { Badge as BSBadge } from "react-bootstrap";

export interface BadgeNode extends Node {
    text: I18nTextValue;
    style: string;
}

export interface Props {
    node: BadgeNode;
}

export class Badge extends React.Component<Props, {}> {
    public static TYPE: string = "BADGE";

    public render(): JSX.Element {
        return (
            <BSBadge bg={this.props.node.style}>{I18nRenderer.render(this.props.node.text)}</BSBadge>
        );
    }
}
