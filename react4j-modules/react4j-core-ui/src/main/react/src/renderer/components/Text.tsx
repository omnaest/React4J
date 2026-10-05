import React from "react";
import { Node } from "../Renderer";
import { I18nRenderer, I18nTextValue } from "./I18nText";

export interface TextNode extends Node
{
    texts: I18nTextValue[];
    style?: string | null;
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

export class Text extends React.Component<Props, {}>
{
    public static TYPE: string = "TEXT";

    public render(): JSX.Element
    {
        const style = this.props.node.style;
        const className = style && Object.prototype.hasOwnProperty.call(STYLE_CLASS, style) ? STYLE_CLASS[style] : undefined;
        const texts = this.props.node.texts.map((text, index) => I18nRenderer.render(text));
        if (className)
        {
            return <span className={className}>{texts}</span>;
        }
        return (
            <>
                {texts}
            </>
        );
    }
}
