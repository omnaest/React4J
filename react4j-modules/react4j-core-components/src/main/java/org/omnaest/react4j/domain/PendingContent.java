package org.omnaest.react4j.domain;

import org.omnaest.react4j.domain.support.UIComponentWithContent;

/**
 * Shows its content only while the application is waiting for the server, and nothing at all otherwise.
 *
 * <h2>Why a component and not a flag on something else</h2>
 * React4J is server-driven: a click posts an event and the page re-renders from the response. By the time the
 * server renders anything, the request it would be reporting on has already finished - so the server can never
 * emit "a response is pending". The pending state exists only on the client, which is why this is a component
 * whose visibility the CLIENT decides, rather than a property the server sets.
 *
 * <h2>What it is for</h2>
 * A transcript, a result panel, anything where the answer appears somewhere specific. Placing one of these at the
 * end of a conversation puts the waiting indicator where the reply itself will land, which tells a reader where
 * to look as well as that something is happening. A page-level indicator can only say the second thing.
 *
 * <h2>Scope</h2>
 * Waiting means "this application has an unsettled round trip". That is deliberately not narrowed to the control
 * that started it: the component's whole purpose is to appear somewhere OTHER than where the user clicked, so
 * tying it to the clicked control would defeat it. An application that wants strictly local feedback should use
 * the busy state a form and its buttons already carry.
 *
 * <h2>Content is the caller's</h2>
 * A spinner, a line of text, a skeleton row - whatever suits the surface it appears on. React4J does not impose
 * one, because "what a pending answer looks like" is a question about the application's own vocabulary.
 *
 * @author omnaest
 */
public interface PendingContent extends UIComponentWithContent<PendingContent>
{
    /**
     * How long the application must have been waiting before this appears, in milliseconds.
     * <p>
     * Most interactions settle in well under 100ms, and a block that flashed into the middle of a transcript on
     * every one of those would be worse than nothing - it would shift the content a reader is in the middle of
     * looking at. The default waits, so this only ever appears when there is genuinely something to wait for.
     */
    public PendingContent withAppearAfterMillis(int appearAfterMillis);
}
