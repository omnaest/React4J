import React from "react";
import { Node } from "../Renderer";
import { I18nRenderer, I18nTextValue } from "./I18nText";
import { Button as BSButton } from "react-bootstrap";

export interface ClipboardCopyButtonNode extends Node {
    label: I18nTextValue;
    text: string;
    clearAfterDurationMillis: number;
}

export interface Props {
    node: ClipboardCopyButtonNode;
}

/**
 * A button that copies its node's `text` to the clipboard on click, then unconditionally overwrites
 * the clipboard with the empty string again after `clearAfterDurationMillis`.
 *
 * PURELY CLIENT-SIDE, like `Toaster`'s dismiss: the server neither owns nor reacts to the copy or the
 * clear, so this node carries no `Handler` field and there is no `/ui/event` round trip involved.
 *
 * RECORDED LIMITATION (see `ClipboardCopyButton` javadoc, react4j-core-components): the auto-clear
 * overwrites the clipboard unconditionally. If the user copies something else into the clipboard
 * during the delay window, that value is overwritten too when the timer fires. A read-compare-clear
 * (`navigator.clipboard.readText()` first) would avoid that, at the cost of a second, separate
 * permission prompt; this component favors the simpler, lower-permission behavior instead.
 *
 * REQUIRES A SECURE CONTEXT - `navigator.clipboard` is only exposed by the browser on `https:`, or on
 * `localhost`/`127.0.0.1`. No non-secure-context fallback is provided: on an insecure origin
 * `navigator.clipboard` is `undefined` and the click is a no-op, guarded below rather than throwing.
 *
 * Renders a button only - it never displays `text` itself. Whether the copied value is visible
 * elsewhere on the page is entirely the caller's decision (see the Java-side node javadoc).
 */
export class ClipboardCopyButton extends React.Component<Props> {
    public static TYPE: string = "CLIPBOARDCOPYBUTTON";

    public render(): JSX.Element {
        return (
            <BSButton onClick={() => this.handleClick()}>
                {I18nRenderer.render(this.props.node.label)}
            </BSButton>
        );
    }

    private handleClick(): void {
        const clipboard = navigator.clipboard;
        if (!clipboard) {
            // No secure-context fallback (see class doc) - a click on an insecure origin is a silent no-op
            // rather than a thrown error, matching how the rest of React4J degrades an unavailable browser API.
            return;
        }

        const text = this.props.node.text;
        const clearAfterDurationMillis = this.props.node.clearAfterDurationMillis;

        clipboard.writeText(text)
            .then(() => {
                setTimeout(() => {
                    clipboard.writeText("");
                }, clearAfterDurationMillis);
            });
    }
}
