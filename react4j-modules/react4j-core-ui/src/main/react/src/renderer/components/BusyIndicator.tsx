import React from "react";
import "./BusyIndicator.css";

export interface Props {
    /** How many round trips are currently in flight; anything above zero means the server is working. */
    inFlightCount: number;
    /**
     * How long a round trip must be outstanding before the bar appears, in milliseconds.
     *
     * Most React4J interactions settle in well under 100ms, and a bar that flashed on every one of those would be
     * worse than no bar at all -- visual noise that carries no information, on the interactions that least need
     * it. Waiting first means the indicator only ever appears when there is genuinely something to wait for.
     *
     * Exposed as a prop so a test can drive it to zero rather than advancing timers around a hard-coded constant.
     */
    appearAfterMillis?: number;
}

interface State {
    visible: boolean;
}

/**
 * A thin animated bar across the top of the window while the server is working.
 *
 * WHY THIS EXISTS. React4J is server-driven: a click posts an event and the page re-renders from the response. For
 * a fast handler that is invisible and fine. For a slow one -- a report, an external call, a language model
 * deciding which tools to call -- the page simply sits there, and a user who has just pressed Enter cannot tell a
 * slow answer from a dropped one. The observed consequence is people pressing the button again, which for a
 * mutating handler means doing the thing twice.
 *
 * WHY IT LIVES HERE RATHER THAN IN EVERY APPLICATION. The in-flight count was already tracked, but its only
 * consumer was `data-inflight-count` on the App root -- a hook for browser tests to wait on instead of sleeping.
 * The signal was machine-readable and had no human-readable form. Rendering it in the framework means every
 * React4J application gets the feedback without asking for it, and none can forget: an application that has to opt
 * in is an application that ships without it.
 *
 * WHY A BAR AND NOT AN OVERLAY. An overlay blocks the page and steals focus, which is wrong for a request that may
 * settle in 200ms. A bar at the top edge is out of the way, is the convention most users already read as "loading",
 * and leaves the content beneath it readable and scrollable while the answer is on its way.
 */
export class BusyIndicator extends React.Component<Props, State> {
    private static readonly DEFAULT_APPEAR_AFTER_MILLIS = 200;

    private appearTimeout?: ReturnType<typeof setTimeout>;

    public constructor(props: Props) {
        super(props);
        this.state = { visible: false };
    }

    public componentDidMount(): void {
        this.synchroniseWithInFlightCount();
    }

    public componentDidUpdate(previousProps: Props): void {
        if (previousProps.inFlightCount !== this.props.inFlightCount) {
            this.synchroniseWithInFlightCount();
        }
    }

    public componentWillUnmount(): void {
        this.clearAppearTimeout();
    }

    /**
     * Busy means "show, but only if this lasts"; idle means "hide immediately".
     *
     * The asymmetry is deliberate. Delaying the appearance suppresses flicker on fast round trips; delaying the
     * disappearance would leave the bar up after the answer had already arrived, which reads as the page still
     * being stuck.
     */
    private synchroniseWithInFlightCount(): void {
        this.clearAppearTimeout();

        if (this.props.inFlightCount <= 0) {
            if (this.state.visible) {
                this.setState({ visible: false });
            }
            return;
        }
        if (this.state.visible) {
            return;
        }

        const appearAfterMillis = this.props.appearAfterMillis ?? BusyIndicator.DEFAULT_APPEAR_AFTER_MILLIS;
        this.appearTimeout = setTimeout(() => this.setState({ visible: true }), appearAfterMillis);
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
            // role="status" with aria-live="polite" rather than "alert": a request being in progress is
            // information, not a problem, and it must not interrupt whatever a screen reader is currently
            // reading. The text is visually hidden because the bar already says it to anyone who can see it.
            <div className="react4j-busy-indicator" role="status" aria-live="polite" data-testid="react4j-busy-indicator">
                <div className="react4j-busy-indicator-bar" />
                <span className="visually-hidden">Working</span>
            </div>
        );
    }
}
