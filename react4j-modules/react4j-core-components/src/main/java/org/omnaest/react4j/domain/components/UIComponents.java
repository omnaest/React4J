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
package org.omnaest.react4j.domain.components;

import org.omnaest.react4j.domain.context.data.Data;

/**
 * Driving a NAMED component from a handler that belongs to a different one.
 *
 * <h2>What this is for</h2>
 * A page is not a set of independent components. A chat box asks for something and a table has to show it; a
 * filter panel is cleared and a chart has to follow. Until now an application could only express that by keeping
 * a shadow copy of the other component's state and hoping the two agreed - which is how a table ends up with a
 * mode the application believes in and a mode the user actually pressed.
 * <p>
 * Here the handler talks to the component instead:
 *
 * <pre>
 * button.onClick((data, context) -&gt;
 * {
 *     uiComponents.in(data)
 *                 .treeTable("partners")
 *                 .setFlatMode(true);
 *     return data;
 * });
 * </pre>
 *
 * <h2>Where the write goes, and why that is the whole design</h2>
 * {@link #in(Data)} names the {@link Data} the writes land in - the same {@link Data} the handler returns, which
 * is what the response echoes and the next render reads. So the state stays where it already lived, client-owned
 * and carried per round trip; nothing new is held on the server, and two browser tabs remain two independent
 * pages.
 * <p>
 * <b>Only from a handler.</b> The {@link Data} a render pass sees is immutable and its setter throws, so these
 * calls belong in a {@code DataEventHandler} - which is exactly where the need arises anyway. A build method must
 * stay a pure function of state; it runs many times per round trip.
 *
 * <h2>What it deliberately does not do</h2>
 * There is no read-back of another component's state and no "refresh" call. Reading is unnecessary because the
 * request already carries the whole page's submitted data, and refreshing is unnecessary because every round trip
 * re-renders from that data - a component whose inputs changed shows the change without being told to.
 *
 * @author omnaest
 */
public interface UIComponents
{
    /**
     * Binds the writes to a {@link Data} instance - the one the handler was given and will return.
     */
    public ComponentAccess in(Data data);

    /**
     * The named components a page exposes, in their own vocabulary.
     */
    public static interface ComponentAccess
    {
        /**
         * The tree table registered under {@code name} via {@code TreeTable.withName(String)}.
         * <p>
         * Never null. A name that no rendered table claims yields a handle whose calls are no-ops rather than a
         * failure - a page can legitimately render without a given table (a different view mode, a permission),
         * and a handler should not have to know which.
         */
        public TreeTableAccess treeTable(String name);
    }

    /**
     * A tree table, addressed by what it does rather than by the fields it keeps.
     */
    public static interface TreeTableAccess
    {
        /**
         * Whether the table shows a flat list rather than the hierarchy.
         */
        public TreeTableAccess setFlatMode(boolean flat);

        /**
         * Sets one column's filter text, replacing whatever was there. An empty or null value clears that column.
         */
        public TreeTableAccess setFilter(String columnKey, String value);

        /**
         * Clears every column filter, including one left behind by a column that no longer exists.
         */
        public TreeTableAccess clearFilters();

        /**
         * Replaces the whole sort spec with a single column.
         */
        public TreeTableAccess setSort(String columnKey, boolean ascending);

        /**
         * Removes all sorting.
         */
        public TreeTableAccess clearSort();

        /**
         * Whether the filter row is shown.
         */
        public TreeTableAccess setFiltersVisible(boolean visible);

        /**
         * Whether a table is actually registered under this name in the current render.
         * <p>
         * Exposed because "the table is not on the page" and "the call did nothing" are otherwise
         * indistinguishable, and an application that genuinely needs to branch on it should not have to infer it.
         */
        public boolean isPresent();
    }
}
