import React from "react";
import { Node, Renderer } from "../Renderer";
import { I18nTextValue, I18nRenderer } from "./I18nText";

export interface VerticalContentSwitcherNode extends Node
{
    elements: VerticalContentBody[]
}

export interface VerticalContentBody
{
    title: I18nTextValue;
    disabled?: boolean;
    active?: boolean;
    content: Node;
}

export interface Props
{
    node: VerticalContentSwitcherNode;
}

export interface State
{
    index: number;
}

export class VerticalContentSwitcher extends React.Component<Props, State>
{
    public static TYPE: string = "VERTICALCONTENTSWITCHER";

    constructor(props: Props)
    {
        super(props);

        this.state = {
            index: props.node.elements.findIndex((element) => element.active === true)
        };
    }

    public render(): JSX.Element
    {
        return (
            <div className="vertical-content-switcher">
                <div className="vertical-content-switcher-navigation">
                    <ul className="nav flex-column nav-pills">
                        {
                            this.props.node.elements.map((element, index) => (
                                <li className="nav-item" key={index}>
                                    <a
                                        className={"nav-link" + (index === this.state.index ? " active" : "") + (element.disabled ? " disabled" : "")}
                                        href="#"
                                        onClick={() => element.disabled ? null : this.setState({ index: index })}
                                    >{I18nRenderer.render(element.title)}</a>
                                </li>
                            ))
                        }
                    </ul>
                </div>
                <div className="vertical-content-switcher-body">
                    {
                        this.props.node.elements.map((element, index) => this.state.index !== index ? (
                            <></>
                        ) : (
                                <div className="card" key={index}>
                                    <div className="card-body">
                                        <div className="card-inner-body">
                                            {Renderer.render(element.content)}
                                        </div>
                                    </div>
                                </div>
                            ))
                    }
                </div>
            </div>
        );
    }
}
