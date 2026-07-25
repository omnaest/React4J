import React from "react";
import { Node, Renderer } from "../Renderer";
import { Tabs as BSTabs, Tab as BSTab } from "react-bootstrap";
import { I18nRenderer, I18nTextValue } from "./I18nText";

export interface TabsNode extends Node
{
    elements: TabElement[]
}

export interface TabElement
{
    title: I18nTextValue;
    disabled?: boolean;
    active?: boolean;
    content: Node;
}

export interface Props
{
    node: TabsNode;
}

export class Tabs extends React.Component<Props>
{
    public static TYPE: string = "TABS";

    public render(): JSX.Element
    {
        const activeIndex = Math.max(0, this.props.node.elements.findIndex((element) => element.active === true));
        const activeKey = String(activeIndex);
        const id = "tabs-" + ((this.props.node.target ?? []).join("-") || "0");

        return (
            <BSTabs defaultActiveKey={activeKey} id={id}>
                {
                    this.props.node.elements.map((element, index) => (
                        <BSTab
                            key={index}
                            eventKey={String(index)}
                            title={I18nRenderer.render(element.title)}
                            disabled={element.disabled}
                        >
                            {Renderer.render(element.content)}
                        </BSTab>
                    ))
                }
            </BSTabs>
        );
    }
}
