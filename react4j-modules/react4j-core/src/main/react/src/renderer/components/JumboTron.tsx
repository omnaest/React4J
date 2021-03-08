
import React from "react";
import { Node, Renderer } from "../Renderer";
import { I18nTextValue, I18nRenderer } from "./I18nText";

export interface JumbotronNode extends Node
{
    title: I18nTextValue;
    subTitle: I18nTextValue;
    content: Node;
    fullWidth: boolean;
}

export interface Props
{
    node: JumbotronNode;
}

export class JumboTron extends React.Component<Props, {}>
{

    public render(): JSX.Element
    {
        const title = I18nRenderer.render(this.props.node.title);
        const subTitle = I18nRenderer.render(this.props.node.subTitle);
        const hasTitle = title && title.length > 0;
        const hasSubTitle = subTitle && subTitle.length > 0;
        return (
            <div className={"jumbotron" + (this.props.node.fullWidth ? " jumbotron-fluid" : "")}>
                { hasTitle && (<h2 className="display-4">{title}</h2>)}
                { hasSubTitle && (<p className="lead">{subTitle}</p>)}
                { (hasTitle || hasSubTitle) && (<br></br>)}
                {Renderer.render(this.props.node.content)}
            </div>
        );
    }
}
