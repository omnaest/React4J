import React from "react";

export interface BusyScope {
    /**
     * Reports that a control in this scope has started or finished a round trip.
     *
     * Counted rather than a boolean, because a form can hold several buttons and two of them may be in flight at
     * once. A boolean would let whichever finished first declare the whole scope idle while the other was still
     * working.
     */
    reportBusy(busy: boolean): void;
}

/**
 * Lets a control tell its enclosing form that it is waiting on the server, so the form can put ITS OWN fields out
 * of action for the duration.
 *
 * WHY NOT THE GLOBAL IN-FLIGHT COUNT. `InFlightTracker` knows how many requests the application has outstanding,
 * which is exactly right for a page-level indicator and exactly wrong here: a table fetching its next page two
 * panels away would disable an unrelated form's Send button. Busy-ness that disables things has to be scoped to
 * the thing that caused it.
 *
 * WHY A CONTEXT RATHER THAN PROPS. A form does not render its buttons directly - they arrive as arbitrary nested
 * nodes through the generic renderer, and there is no prop path from one to the other. A context crosses that gap
 * without every intermediate component having to know it exists.
 *
 * The default is a no-op, so a button rendered outside any form still works and simply reports into nothing.
 */
export const BusyScopeContext = React.createContext<BusyScope>({
    reportBusy: () => {
        // No enclosing form. A standalone button still manages its own spinner; there is just nothing else to
        // disable, and that is a legitimate arrangement rather than a misconfiguration.
    }
});
