import React from "react";
import { Node, Renderer } from "../Renderer";
import { I18nRenderer, I18nTextValue } from "./I18nText";

export interface BlockQuoteNode extends Node
{
    texts: I18nTextValue[];
    elements?: Node[] | null;
    footer?: I18nTextValue | null;
}

export interface Props
{
    node: BlockQuoteNode;
}

export class BlockQuote extends React.Component<Props, {}>
{
    public static TYPE: string = "BLOCKQUOTE";

    public render(): JSX.Element
    {
        const footer = I18nRenderer.render(this.props.node.footer as I18nTextValue);
        return (
            <blockquote className="blockquote">
                {
                    this.props.node.texts.map((text, index) => (
                        <p key={index} className={"mb-0" + index}>{I18nRenderer.render(text)}</p>
                    ))
                }
                {
                    (this.props.node.elements || []).map((element, index) => (
                        <React.Fragment key={index}>{Renderer.render(element)}</React.Fragment>
                    ))
                }
                {
                    // Present only when it renders to text: Bootstrap's .blockquote-footer::before prints an em dash even for an empty footer.
                    footer ? (
                        <footer className="blockquote-footer">
                            <cite>{footer}</cite>
                        </footer>
                    ) : null
                }
            </blockquote>
        );
    }
}
