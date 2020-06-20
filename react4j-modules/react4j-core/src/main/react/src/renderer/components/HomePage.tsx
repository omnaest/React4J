import React from "react";
import { Node, Renderer } from "../Renderer";

export interface HomePageNode extends Node
{
    navigation: Node;
    body: Node;
}

export interface Props
{
    node: HomePageNode;
}

export class HomePage extends React.Component<Props, {}>
{
    public static TYPE: string = "HOMEPAGE";

    public render(): JSX.Element
    {
        if (this.props.node.navigation)
        {
            return (
                <>
                    <div className="body-top">
                        {Renderer.render(this.props.node.navigation)}
                    </div>
                    <div className="body-bottom">
                        {Renderer.render(this.props.node.body)}
                    </div>
                </>
            );
        } else
        {
            return (
                <div className="body-full">
                    {Renderer.render(this.props.node.body)}
                </div>
            );
        }
    }
}
