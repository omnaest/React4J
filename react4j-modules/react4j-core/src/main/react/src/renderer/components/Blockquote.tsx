import React from "react";
import { Node, Renderer } from "../Renderer";
import { I18nTextValue, I18nRenderer } from "./I18nText";

export interface BlockQuoteNode extends Node
{
    texts: I18nTextValue[];
    footer: I18nTextValue;
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
        return (
            <blockquote className="blockquote">
                {
                    this.props.node.texts.map((text, index) => (
                        <p className={"mb-0" + index}>{I18nRenderer.render(text)}</p>
                    ))
                }
                <footer className="blockquote-footer">
                    <cite>{I18nRenderer.render(this.props.node.footer)}</cite>
                </footer>
            </blockquote>
        );
    }
}
