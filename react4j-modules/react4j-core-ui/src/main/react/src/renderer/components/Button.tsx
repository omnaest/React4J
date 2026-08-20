import React from "react";
import { Node, RenderingSupport } from "../Renderer";
import { RenderingSupportContext } from "../support/RenderingSupportContext";
import { BusyScopeContext } from "../support/BusyScopeContext";
import { Handler, HandlerFactory } from "../handler/Handler";
import { I18nRenderer, I18nTextValue } from "./I18nText";
import { Button as BSButton, Spinner as BSSpinner } from "react-bootstrap";

export interface ButtonNode extends Node {
    name: I18nTextValue;
    style: string;
    ariaLabel?: string;
    onClick?: Handler;
}

export interface Props {
    node: ButtonNode;
}

interface State {
    busy: boolean;
}

/**
 * A button that shows, and enforces, that it is waiting on the server.
 *
 * WHY IT TRACKS ITS OWN ROUND TRIP RATHER THAN THE GLOBAL ONE. `InFlightTracker` counts every request the
 * application has outstanding, which is the right input for the page-level indicator and the wrong one here: a
 * table fetching its next page in another panel would spin an unrelated button. This waits on the promise its own
 * click returned.
 *
 * WHY DISABLING MATTERS MORE THAN THE SPINNER. A server handler is usually a mutation. Without this, a slow
 * handler invites a second click - and a second click means doing the thing twice, silently. That is not a
 * hypothetical: it was hit during development, clicking Send while an Enter-triggered submission of the same form
 * was still in flight. The spinner explains the disabling; the disabling is the part that prevents a bug.
 *
 * NO APPEARANCE DELAY, unlike the page-level indicator. That one waits before appearing because a bar sweeping
 * across the top of the window on every fast interaction is visual noise. A button briefly disabling is
 * imperceptible at the same durations, and delaying it would leave exactly the window in which a double click
 * lands.
 */
export class Button extends React.Component<Props, State> {
    public static TYPE: string = "BUTTON";
    public static contextType = RenderingSupportContext;

    /** Guards against a state update after the button has gone, which React warns about and which leaks. */
    private mounted: boolean = false;

    public constructor(props: Props) {
        super(props);
        this.state = { busy: false };
    }

    public componentDidMount(): void {
        this.mounted = true;
    }

    public componentWillUnmount(): void {
        this.mounted = false;
    }

    public render(): JSX.Element {
        const renderingSupport = this.context as RenderingSupport | undefined;
        return (
            <BusyScopeContext.Consumer>
                {(busyScope) => (
                    <BSButton
                        variant={(this.props.node.style ? " btn-" + this.props.node.style : "")}
                        aria-label={this.props.node.ariaLabel || undefined}
                        disabled={this.state.busy}
                        // Tells assistive technology that this control is working, which a visual spinner
                        // conveys to everyone else. Without it the button simply goes quiet and unresponsive.
                        aria-busy={this.state.busy || undefined}
                        onClick={this.props.node.onClick
                            ? (event: React.MouseEvent<HTMLButtonElement>) => this.handleClick(event, busyScope, renderingSupport)
                            : undefined}
                    >
                        {this.state.busy && (
                            <>
                                <BSSpinner as="span" animation="border" size="sm" role="status" aria-hidden="true" />
                                &nbsp;
                            </>
                        )}
                        {I18nRenderer.render(this.props.node.name)}
                    </BSButton>
                )}
            </BusyScopeContext.Consumer>
        );
    }

    private handleClick(event: React.MouseEvent<HTMLButtonElement>, busyScope: { reportBusy(busy: boolean): void }, renderingSupport?: RenderingSupport): void {
        this.setState({ busy: true });
        busyScope.reportBusy(true);

        HandlerFactory.onClick(this.props.node.onClick as Handler, renderingSupport?.uiContextAccessor, renderingSupport?.nodeContextAccessor)(event)
            .finally(() => {
                // Always cleared, including on failure. A control left disabled after an error is a dead end on
                // the one occasion the user most needs to try again - which is why handleEvent resolves rather
                // than rejects when a round trip fails.
                busyScope.reportBusy(false);
                if (this.mounted) {
                    this.setState({ busy: false });
                }
            });
    }
}
