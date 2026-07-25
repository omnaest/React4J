
import React from "react";
import { RenderingSupport } from "../Renderer";
import { RenderingSupportContext } from "../support/RenderingSupportContext";
import { Handler, HandlerFactory } from "../handler/Handler";
import { I18nRenderer, I18nTextValue } from "./I18nText";
import { Dropdown as BSDropdown } from "react-bootstrap";

export interface DropdownItemNode {
    kind: string;
    text: I18nTextValue;
    link?: string;
    active: boolean;
    disabled: boolean;
    onClick?: Handler;
}

// renderDropdownItem is a plain function invoked directly from Dropdown.tsx/SplitButton.tsx's
// render (not mounted via JSX as its own component), so it cannot use `static contextType` or hooks.
// It reads the ambient RenderingSupportContext via a Consumer render-prop instead, which works
// regardless of how the returned element is invoked, because Context resolution is positional
// (nearest Provider ancestor in the mounted tree), not based on which function created the element.
export function renderDropdownItem(item: DropdownItemNode, index: number): JSX.Element {
    switch (item.kind) {
        case "DIVIDER":
            return <BSDropdown.Divider key={index} />;
        case "HEADER":
            return <BSDropdown.Header key={index}>{I18nRenderer.render(item.text)}</BSDropdown.Header>;
        default:
            return (
                <RenderingSupportContext.Consumer key={index}>
                    {(renderingSupport: RenderingSupport | undefined) => (
                        <BSDropdown.Item
                            href={item.link || undefined}
                            active={item.active}
                            disabled={item.disabled}
                            onClick={item.onClick ? HandlerFactory.onClick(item.onClick as Handler, renderingSupport?.uiContextAccessor, renderingSupport?.nodeContextAccessor) : undefined}
                        >
                            {I18nRenderer.render(item.text)}
                        </BSDropdown.Item>
                    )}
                </RenderingSupportContext.Consumer>
            );
    }
}
