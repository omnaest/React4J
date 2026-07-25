import React from "react";
import { Node, Renderer } from "../Renderer";

export interface ScrollbarContainerNode extends Node
{
    content: Node;
    verticalBoxMode: string;
    horizontalBoxMode: string;
    scrollToBottomOnUpdate: boolean;
}

export interface Props
{
    node: ScrollbarContainerNode;
}

export class ScrollbarContainer extends React.Component<Props, {}>
{
    public static TYPE: string = "SCROLLBARCONTAINER";

    private contentRef = React.createRef<HTMLDivElement>();

    public componentDidMount(): void
    {
        this.scrollToBottomIfEnabled();
    }

    public componentDidUpdate(): void
    {
        this.scrollToBottomIfEnabled();
    }

    private scrollToBottomIfEnabled(): void
    {
        if (this.props.node.scrollToBottomOnUpdate && this.contentRef.current)
        {
            this.contentRef.current.scrollTop = this.contentRef.current.scrollHeight;
        }
    }

    public render(): JSX.Element
    {
        return (
            <div ref={this.contentRef} className={"scrollbar-container " + this.props.node.verticalBoxMode + " " + this.props.node.horizontalBoxMode}>
                <div className={"scrollbar-container-content " + this.props.node.verticalBoxMode + " " + this.props.node.horizontalBoxMode}>
                    {Renderer.render(this.props.node.content)}
                </div>
            </div>
        );
    }
}
