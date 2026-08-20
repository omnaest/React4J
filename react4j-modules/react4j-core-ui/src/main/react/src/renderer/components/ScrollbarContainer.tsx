import React from "react";
import { Node, Renderer } from "../Renderer";
import { ScrollSupport } from "../support/ScrollSupport";
// Layout mechanics for this component's box modes. Bundled with the framework rather than placed in
// /css/custom.css, which is the application's customisation slot - see ScrollbarContainer.css.
import "./ScrollbarContainer.css";

export interface ScrollbarContainerNode extends Node
{
    content: Node;
    verticalBoxMode: string;
    horizontalBoxMode: string;
    scrollToBottomOnUpdate: boolean;
    /** Announce appended content to screen readers without moving focus (see withAnnouncedUpdates). */
    announcedUpdates?: boolean;
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

    /**
     * Keeps the newest content in view.
     *
     * This used to set scrollTop on this container only, which silently did nothing whenever the container was not
     * itself the scrolling element - and that is the common case, not an exotic one: put the content in a Card and
     * the card body owns the scroll, so the container grows to its content height and setting scrollTop on it has
     * no effect at all. The failure is invisible; the newest message simply never comes into view.
     *
     * So: scroll this container if it scrolls, otherwise find the nearest ancestor that does. "Scroll to bottom on
     * update" is a statement about what the user should end up seeing, not about which div happens to own the
     * scrollbar.
     */
    private scrollToBottomIfEnabled(): void
    {
        if (!this.props.node.scrollToBottomOnUpdate)
        {
            return;
        }
        ScrollSupport.scrollToBottom(this.contentRef.current);
    }


    public render(): JSX.Element
    {
        /*
         * The live-region attributes go on the INNER content div, not on the scroll container. A screen reader
         * announces what changes inside the element carrying role="log", and the scroll container's own scrollTop
         * changes on every update - marking it would risk announcing the region for a scroll rather than for new
         * content. The inner div only changes when content is actually appended.
         */
        const announced = this.props.node.announcedUpdates === true;
        return (
            <div ref={this.contentRef} className={"scrollbar-container " + this.props.node.verticalBoxMode + " " + this.props.node.horizontalBoxMode}>
                <div className={"scrollbar-container-content " + this.props.node.verticalBoxMode + " " + this.props.node.horizontalBoxMode}
                    role={announced ? "log" : undefined}
                    aria-live={announced ? "polite" : undefined}
                    aria-relevant={announced ? "additions" : undefined}>
                    {Renderer.render(this.props.node.content)}
                </div>
            </div>
        );
    }
}
