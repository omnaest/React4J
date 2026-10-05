import React from "react";
import { Node } from "../Renderer";
import { I18nRenderer, I18nTextValue } from "./I18nText";

export interface TextNode extends Node
{
    texts: I18nTextValue[];
    style?: string | null;
    emphasis?: string[] | null;
}

export interface Props
{
    node: TextNode;
}

/**
 * The theme class per server-side Text.Style, mirroring Text.Style#toCssClass() in react4j-core-components.
 * The one place the client mapping lives; a style missing here renders unstyled.
 */
const STYLE_CLASS: { [style: string]: string } = {
    MUTED: "text-body-secondary"
};

/**
 * The semantic element per server-side Text.Emphasis, mirroring Text.Emphasis#toElementName() in react4j-core-components.
 * The one place the client mapping lives. The key order IS the nesting order (enum order, outermost first), so the
 * rendering does not depend on the order the array arrives in; a member missing here is ignored.
 */
const EMPHASIS_ELEMENT: { [emphasis: string]: string } = {
    BOLD: "strong",
    ITALIC: "em",
    STRIKETHROUGH: "del"
};

export class Text extends React.Component<Props, {}>
{
    public static TYPE: string = "TEXT";

    public render(): JSX.Element
    {
        const style = this.props.node.style;
        const className = style && Object.prototype.hasOwnProperty.call(STYLE_CLASS, style) ? STYLE_CLASS[style] : undefined;
        const texts = this.props.node.texts.map((text, index) => <React.Fragment key={index}>{I18nRenderer.render(text)}</React.Fragment>);
        const emphasised = this.wrapIntoEmphasis(texts);
        if (className)
        {
            return <span className={className}>{emphasised}</span>;
        }
        return (
            <>
                {emphasised}
            </>
        );
    }

    private wrapIntoEmphasis(content: React.ReactNode): React.ReactNode
    {
        const emphasis = this.props.node.emphasis || [];
        return Object.keys(EMPHASIS_ELEMENT)
            .filter(member => emphasis.indexOf(member) >= 0)
            .reverse()
            .reduce((inner, member) => React.createElement(EMPHASIS_ELEMENT[member], null, inner), content);
    }
}
