import React from "react";
import { Node } from "../Renderer";

export interface NativeHtmlNode extends Node
{
    source: string;
}

export interface Props
{
    node: NativeHtmlNode;
}

export class NativeHtml extends React.Component<Props, {}>
{
    public static TYPE: string = "NATIVEHTML";

    public render(): JSX.Element
    {
        return this.props.node?.source ? (
            <div dangerouslySetInnerHTML={{ __html: this.props.node?.source }} />
        ) : <></>;
    }
}
