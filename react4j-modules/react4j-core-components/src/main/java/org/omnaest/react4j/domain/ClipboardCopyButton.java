/*******************************************************************************
 * Copyright 2021 Danny Kunz
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package org.omnaest.react4j.domain;

import java.util.concurrent.TimeUnit;

/**
 * A button that copies a caller-supplied text to the clipboard when clicked, and unconditionally overwrites the
 * clipboard with the empty string again after a caller-supplied delay ("auto-clear").
 * <p>
 * This is a purely client-side interaction: the server neither owns nor reacts to the copy or the clear, so there is
 * no server {@code EventHandler} and no round trip once the node has been rendered - the same kind of call
 * {@code Alert}/{@code Toaster} already make for their client-only dismiss behavior, and the opposite of the call a
 * server-owned overlay/toggle (e.g. {@code Modal}, {@code ToggleButton}) makes for state the server must open or
 * react to.
 * <p>
 * <b>Recorded limitation - deliberate, not an oversight.</b> The auto-clear overwrites the clipboard
 * <em>unconditionally</em> after the configured delay: if the user copies something else into the clipboard during
 * the delay window, that value is overwritten too when the timer fires. Comparing the clipboard's current content
 * first would require {@code navigator.clipboard.readText()}, which additionally prompts for a separate read
 * permission; this component favors the simpler, lower-permission, higher-confidentiality unconditional overwrite
 * instead. Renegotiable against evidence that a read-compare-clear is in fact unobtrusive.
 * <p>
 * Renders a button only - it never displays the supplied text itself anywhere in its own node. Whether the copied
 * value is visible on screen elsewhere on the page is entirely the caller's decision.
 * <p>
 * Relies on {@code navigator.clipboard}, which browsers only expose in a secure context ({@code https:}, or
 * {@code localhost}/{@code 127.0.0.1}). No non-secure-context fallback is provided; see the {@code .tsx} renderer for
 * the exact client-side behavior on an insecure origin.
 *
 * @author omnaest
 */
public interface ClipboardCopyButton extends UIComponent<ClipboardCopyButton>
{
    /**
     * The text copied to the clipboard when this button is clicked.
     *
     * @param text
     * @return this
     */
    public ClipboardCopyButton withText(String text);

    /**
     * This button's visible label.
     *
     * @param label
     * @return this
     */
    public ClipboardCopyButton withLabel(String label);

    /**
     * How long after a successful copy the clipboard is unconditionally overwritten with the empty string again.
     * Default is 30 seconds.
     *
     * @param duration
     * @param timeUnit
     * @return this
     */
    public ClipboardCopyButton withClearAfterDuration(int duration, TimeUnit timeUnit);
}
