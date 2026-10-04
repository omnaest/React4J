import React from "react";
import { Dropdown } from "react-bootstrap";
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
    /**
     * Absent/null on a plain entry. An array (possibly empty) on a dropdown: text/active/disabled then describe the toggle, link and
     * linkedId are null and the items are plain entries (one level).
     */
    dropdownEntries?: NavigationBarEntry[] | null;
}

export interface Props
{
    node: NavigationBarNode;
}

/**
 * The menu is positioned with popper's fixed strategy: the expanded bar sits in `.body-top` (overflow-x: auto, overflow-y: hidden), which clips an
 * absolutely positioned menu, while a fixed one is not clipped by an overflow ancestor (react4j-core-ui/CLAUDE.md, defect class "overlay clipped").
 */
const FIXED_POPPER_CONFIG = { strategy: "fixed" as const };

export class NavigationBar extends React.Component<Props, {}>
{
    public static TYPE: string = "NAVIGATIONBAR";

    private static href(entry: NavigationBarEntry): string | undefined
    {
        return entry.linkedId ? "#" + entry.linkedId : entry.link;
    }

    private static renderPlainEntry(entry: NavigationBarEntry, index: number): JSX.Element
    {
        return (
            <li className="nav-item" key={index}>
                <a
                    className={"nav-link" + (entry.active ? " active" : "") + (entry.disabled ? " disabled" : "")}
                    href={NavigationBar.href(entry)}
                    target={entry.link ? "_blank" : "_self"}
                >{I18nRenderer.render(entry.text)}</a>
            </li>
        );
    }

    private static renderDropdown(entry: NavigationBarEntry, index: number): JSX.Element
    {
        const items = entry.dropdownEntries || [];
        return (
            <Dropdown as="li" className="nav-item" key={index}>
                <Dropdown.Toggle
                    as="button"
                    type="button"
                    className={"nav-link" + (entry.active ? " active" : "") + (entry.disabled ? " disabled" : "")}
                    disabled={entry.disabled}
                >{I18nRenderer.render(entry.text)}</Dropdown.Toggle>
                <Dropdown.Menu popperConfig={FIXED_POPPER_CONFIG}>
                    {
                        items.map((item, itemIndex) =>
                        (
                            <Dropdown.Item
                                key={itemIndex}
                                href={NavigationBar.href(item)}
                                target={item.link ? "_blank" : "_self"}
                                active={item.active}
                                disabled={item.disabled}
                            >{I18nRenderer.render(item.text)}</Dropdown.Item>
                        )
                        )
                    }
                </Dropdown.Menu>
            </Dropdown>
        );
    }

    public render(): JSX.Element
    {
        return (
            <nav id="navbarContent" className="navbar navbar-content">
                <ul className="nav nav-pills page-navigation flex-nowrap flex-row">
                    {
                        this.props.node.entries.map((entry, index) =>
                            entry.dropdownEntries
                                ? NavigationBar.renderDropdown(entry, index)
                                : NavigationBar.renderPlainEntry(entry, index)
                        )
                    }
                </ul>
            </nav >
        );
    }
}
