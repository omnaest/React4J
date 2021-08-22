
import React from "react";
import { Node } from "../Renderer";
import { I18nTextValue, I18nRenderer } from "./I18nText";

export interface IFrameContainerNode extends Node
{
    sourceLink: string;
    title: I18nTextValue;
    allowFullScreen: boolean;
}

export interface Props
{
    node: IFrameContainerNode;
}

export class IFrameContainer extends React.Component<Props, {}>
{
    public static TYPE: string = "IFRAMECONTAINER";

    public render(): JSX.Element
    {
        return (
            <iframe
                className="iframe-container"
                src={this.props.node.sourceLink}
                title={I18nRenderer.render(this.props.node.title)}
                allowFullScreen={this.props.node.allowFullScreen}
                frameBorder="0"
            />
        );
    }
}
