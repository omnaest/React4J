import React from "react";
import { Node, Renderer } from "../Renderer";
import { Accordion as BSAccordion } from "react-bootstrap";
import { I18nRenderer, I18nTextValue } from "./I18nText";

export interface AccordionNode extends Node
{
    alwaysOpen: boolean;
    panels: AccordionPanel[]
}

export interface AccordionPanel
{
    title: I18nTextValue;
    expanded?: boolean;
    content: Node;
}

export interface Props
{
    node: AccordionNode;
}

export class Accordion extends React.Component<Props>
{
    public static TYPE: string = "ACCORDION";

    public render(): JSX.Element
    {
        const node = this.props.node;
        const defaultActiveKey: string[] | string | undefined = node.alwaysOpen
            ? node.panels
                .map((panel, index) => (panel.expanded === true ? String(index) : undefined))
                .filter((key): key is string => key !== undefined)
            : (() =>
            {
                const index = node.panels.findIndex((panel) => panel.expanded === true);
                return index >= 0 ? String(index) : undefined;
            })();

        return (
            <BSAccordion alwaysOpen={node.alwaysOpen} defaultActiveKey={defaultActiveKey}>
                {
                    node.panels.map((panel, index) => (
                        <BSAccordion.Item key={index} eventKey={String(index)}>
                            <BSAccordion.Header>{I18nRenderer.render(panel.title)}</BSAccordion.Header>
                            <BSAccordion.Body>{Renderer.render(panel.content)}</BSAccordion.Body>
                        </BSAccordion.Item>
                    ))
                }
            </BSAccordion>
        );
    }
}
