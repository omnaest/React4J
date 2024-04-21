import React from "react";
import { Node } from "../Renderer";
import { I18nRenderer, I18nTextValue } from "./I18nText";

export interface NavigationBarNode extends Node
{
    entries: NavigationBarEntry[];
}

export interface NavigationBarEntry
{
    text: I18nTextValue;
    linkedId?: string;
    link?: string;
    active?: boolean;
    disabled?: boolean;
}

export interface Props
{
    node: NavigationBarNode;
}

export class NavigationBar extends React.Component<Props, {}>
{
    public static TYPE: string = "NAVIGATIONBAR";

    public render(): JSX.Element
    {
        return (
            <nav id="navbarContent" className="navbar navbar-content">
                <ul className="nav nav-pills page-navigation flex-nowrap flex-row">
                    {
                        this.props.node.entries.map(entry =>
                        (
                            <li className="nav-item">
                                <a
                                    className={"nav-link" + (entry.active ? " active" : "") + (entry.disabled ? " disabled" : "")}
                                    href={entry.linkedId ? "#" + entry.linkedId : entry.link}
                                    target={entry.link ? "_blank" : "_self"}
                                >{I18nRenderer.render(entry.text)}</a>
                            </li>
                        )
                        )
                    }
                </ul>
            </nav >
        );
    }
}
