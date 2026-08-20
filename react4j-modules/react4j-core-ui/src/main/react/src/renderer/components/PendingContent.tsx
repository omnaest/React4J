import React from "react";
import { Node, Renderer } from "../Renderer";
import { InFlightTracker } from "../../backend/InFlightTracker";
import { ScrollSupport } from "../support/ScrollSupport";

export interface PendingContentNode extends Node {
    content: Node;
    appearAfterMillis: number;
}

export interface Props {
    node: PendingContentNode;
}

interface State {
    visible: boolean;
}

/**
 * Renders its content only while the application has a round trip outstanding.
 *
 * WHY THE CLIENT DECIDES. React4J is server-driven, and by the time the server renders anything the request it
 * would be reporting on has already settled - so "a response is pending" is a fact only this side ever holds. The
 * server sends the content unconditionally; this component decides whether it is on screen.
 *
 * WHY IT CLAIMS THE ROUND TRIP. While this is showing, it IS the indicator for that request, so it tells
 * InFlightTracker not to also raise the page-level one. Every round trip should produce exactly one indicator -
 * the most local one available - and a bar across the top of the window saying the same thing as a block in the
 * middle of the page teaches a user to read neither.
 *
 * WHY THE DELAY. Most interactions settle in well under 100ms. A block appearing and vanishing inside a
 * transcript on every one of those would shift the very content the reader is looking at, which is worse than no
 * feedback at all.
 */
export class PendingContent extends React.Component<Props, State> {
    public static TYPE: string = "PENDINGCONTENT";

    private unsubscribe?: () => void;
    private appearTimeout?: ReturnType<typeof setTimeout>;
    /** Whether this component currently holds a claim, so it releases exactly the claims it made. */
    private claimed: boolean = false;
    private contentRef = React.createRef<HTMLDivElement>();

    public constructor(props: Props) {
        super(props);
        this.state = { visible: false };
    }

    public componentDidMount(): void {
        this.unsubscribe = InFlightTracker.subscribe((count) => this.synchronise(count));
    }

    /**
     * Brings the block into view once it appears.
     *
     * Necessary because nothing else will. The enclosing scroll container follows the transcript on every SERVER
     * update, and this block appears without one - it is a client-side reaction to a request that has not come
     * back yet. So a user with any scroll history at all got a "working on it" indicator below the fold,
     * indistinguishable from nothing happening, which is the exact problem it exists to solve.
     *
     * Scrolls to the container's bottom rather than scrolling this element into view: the block sits at the end of
     * the transcript, and the reply that replaces it will put the container in the same place. Matching them means
     * the arrival does not move the page a second time.
     *
     * Same trade-off the enclosing container already makes: a user who deliberately scrolled up to read history
     * gets pulled back down. Consistency with the existing behaviour wins over inventing a second rule.
     */
    public componentDidUpdate(previousProps: Props, previousState: State): void {
        if (this.state.visible && !previousState.visible) {
            ScrollSupport.scrollToBottom(this.contentRef.current);
        }
    }

    public componentWillUnmount(): void {
        this.unsubscribe?.();
        this.clearAppearTimeout();
        this.releaseClaim();
    }

    private synchronise(inFlightCount: number): void {
        this.clearAppearTimeout();

        if (inFlightCount <= 0) {
            this.releaseClaim();
            if (this.state.visible) {
                this.setState({ visible: false });
            }
            return;
        }
        if (this.state.visible) {
            return;
        }

        const appearAfterMillis = this.props.node.appearAfterMillis ?? 0;
        this.appearTimeout = setTimeout(() => {
            // Claimed at the moment it becomes visible, not when the request started: until then the page-level
            // indicator is the only feedback there is, and suppressing it early would leave a slow-to-appear
            // block as the sole signal during the delay.
            this.takeClaim();
            this.setState({ visible: true });
        }, appearAfterMillis);
    }

    private takeClaim(): void {
        if (!this.claimed) {
            this.claimed = true;
            InFlightTracker.claim();
        }
    }

    private releaseClaim(): void {
        if (this.claimed) {
            this.claimed = false;
            InFlightTracker.release();
        }
    }

    private clearAppearTimeout(): void {
        if (this.appearTimeout !== undefined) {
            clearTimeout(this.appearTimeout);
            this.appearTimeout = undefined;
        }
    }

    public render(): JSX.Element | null {
        if (!this.state.visible) {
            return null;
        }
        return (
            // role="status" with aria-live="polite": a pending answer is information, not an alert, and must not
            // interrupt whatever a screen reader is currently reading out.
            <div ref={this.contentRef} role="status" aria-live="polite" data-testid="react4j-pending-content">
                {Renderer.render(this.props.node.content)}
            </div>
        );
    }
}
