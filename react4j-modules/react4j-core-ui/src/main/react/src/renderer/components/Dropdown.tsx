
import React from "react";
import { Node } from "../Renderer";
import { I18nRenderer, I18nTextValue } from "./I18nText";
import { DropdownItemNode, renderDropdownItem } from "./DropdownItem";
import { DropdownButton as BSDropdownButton, NavDropdown as BSNavDropdown } from "react-bootstrap";

export interface DropdownNode extends Node {
    presentation: string;
    title: I18nTextValue;
    style: string;
    drop?: string;
    items: DropdownItemNode[];
}

export interface Props {
    node: DropdownNode;
}

export class Dropdown extends React.Component<Props, {}> {
    public static TYPE: string = "DROPDOWN";

    public render(): JSX.Element {
        const node = this.props.node;

        if (node.presentation === "NAV") {
            return (
                <BSNavDropdown
                    title={I18nRenderer.render(node.title)}
                    drop={node.drop ? (node.drop as "up" | "down" | "start" | "end") : undefined}
                >
                    {node.items.map(renderDropdownItem)}
                </BSNavDropdown>
            );
        }

        return (
            <BSDropdownButton
                title={I18nRenderer.render(node.title)}
                variant={node.style || "primary"}
                drop={node.drop ? (node.drop as "up" | "down" | "start" | "end") : undefined}
            >
                {node.items.map(renderDropdownItem)}
            </BSDropdownButton>
        );
    }
}
