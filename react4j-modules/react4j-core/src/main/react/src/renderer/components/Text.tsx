import React from "react";
import { Node } from "../Renderer";
import { I18nRenderer, I18nTextValue } from "./I18nText";

export interface TextNode extends Node
{
    texts: I18nTextValue[];
}

export interface Props
{
    node: TextNode;
}

export class Text extends React.Component<Props, {}>
{
    public static TYPE: string = "TEXT";

    public render(): JSX.Element
    {
        return (
            <>
                {this.props.node.texts.map((text, index) => I18nRenderer.render(text))}
            </>
        );
    }
}
