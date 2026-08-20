import { Backend, Target } from "../../backend/Backend";
import { InFlightTracker } from "../../backend/InFlightTracker";
import { UIContextAccessor } from "../data/DataContextManager";
import { NodeContextAccessor } from "../Renderer";

export interface Handler {
    type: "SERVER" | "CLIENT";
}

export interface ServerHandler extends Handler {
    type: "SERVER",
    target: Target,
    contextId: string;
}

export class HandlerFactory {
    /**
     * Returns a click handler that RESOLVES when the round trip it starts has settled.
     *
     * The promise used to be discarded here. That made per-control feedback impossible: a component could start a
     * server round trip but had no way to learn when its own one finished, so the only signal available was the
     * global in-flight count -- which is true of the whole application and would, for instance, put a chat's Send
     * button into a busy state because a table two panels away was fetching a page.
     */
    public static onClick(handler: Handler, uiContextAccessor?: UIContextAccessor, nodeContextAccessor?: NodeContextAccessor) {
        return (event: React.MouseEvent): Promise<void> => {
            event.preventDefault();
            return HandlerFactory.handleEvent(handler, uiContextAccessor, nodeContextAccessor);
        };
    }

    /**
     * Dispatches the handler and resolves once it has settled - including when it FAILED.
     *
     * Resolving rather than rejecting on failure is deliberate: every caller of this uses the settle signal to
     * take a control back out of a busy state, and a rejection would leave that control disabled forever on the
     * one occasion a user most needs to retry. Errors are already surfaced by the backend layer; this method's
     * contract is "the round trip is over", not "the round trip worked".
     */
    /**
     * Dispatches as {@link #handleEvent} but marks the round trip as framework housekeeping, so indicators do not
     * report it as work the user is waiting on.
     *
     * For the form's per-keystroke field sync: a real request, but not one anybody asked for, and reporting it
     * makes every busy indicator fire continuously while somebody types.
     */
    public static handleEventInBackground(handler: Handler, uiContextAccessor?: UIContextAccessor, nodeContextAccessor?: NodeContextAccessor): Promise<void> {
        if (!handler) {
            return Promise.resolve();
        }
        InFlightTracker.incrementBackground();
        return HandlerFactory.handleEvent(handler, uiContextAccessor, nodeContextAccessor)
            .finally(() => InFlightTracker.decrementBackground());
    }

    public static handleEvent(handler: Handler, uiContextAccessor?: UIContextAccessor, nodeContextAccessor?: NodeContextAccessor): Promise<void> {
        if (!handler) {
            return Promise.resolve();
        }
        if (handler && handler.type === "SERVER") {
            const serverHandler = (handler as ServerHandler);
            // Promise.resolve normalises the result rather than assuming one. Backend.sendEvent's declared type
            // always is a promise, so this is not defending against production - it is this method's boundary
            // doing what a boundary should, and it also means a partial test double of Backend does not have to
            // model a return value the test under it never looks at.
            return Promise.resolve(Backend.sendEvent(serverHandler.target, serverHandler.contextId, uiContextAccessor, nodeContextAccessor))
                .then(() => undefined)
                .catch(() => undefined);
        }
        else {
            throw new Error("Handler type " + handler.type + " not supported");
        }
    }
}
