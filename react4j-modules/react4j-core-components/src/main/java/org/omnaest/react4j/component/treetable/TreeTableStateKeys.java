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
package org.omnaest.react4j.component.treetable;

import java.util.ArrayList;
import java.util.List;

import org.omnaest.react4j.domain.Location;

/**
 * The submitted-{@code Data} field keys under which one {@code TreeTable} keeps its view state.
 *
 * <h2>Why this is its own type</h2>
 * A {@code TreeTable}'s mode, filters, sort and window live in the submitted data, and until now only the renderer
 * knew where. That was fine while the renderer was the only party involved - it both wrote and read them. It stops
 * being fine the moment anything ELSE needs to change the table, because a second derivation of the same key is a
 * second chance for the two to disagree, and they would disagree silently: a write under a key nobody reads simply
 * has no effect, with no error anywhere.
 * <p>
 * That failure mode is not hypothetical in this file's history. The sort field had exactly two derivations of its
 * own default - one seeded from column config, one hardcoding an empty list - and the first sort click on a grid
 * silently discarded the seeded initial sort. The fix then was to route both readers through one helper. This type
 * is the same fix at a wider scope, before rather than after.
 *
 * <h2>Positional, and therefore fragile in one specific way</h2>
 * Every key is derived from the grid's {@link Location}, which is positional - it encodes where the component sits
 * in the rendered tree. Move a table to a different place on the page and its keys change, so whatever state was
 * submitted under the old ones is silently abandoned and the table falls back to its defaults. That is inherent to
 * positional addressing rather than something this type introduces, but it is the reason a key must never be
 * spelled out by hand at a call site.
 *
 * @author omnaest
 */
public final class TreeTableStateKeys
{
    private static final String PREFIX = "treetable.";

    private final String        gridPrefix;

    private TreeTableStateKeys(Location gridLocation)
    {
        this.gridPrefix = PREFIX + String.join(".", gridLocation.get()) + ".";
    }

    /**
     * @param gridLocation
     *            the GRID's own location - not the table's, not a row's. Every key below hangs off it.
     */
    public static TreeTableStateKeys of(Location gridLocation)
    {
        return new TreeTableStateKeys(gridLocation);
    }

    /** Whether the table shows a flat list rather than the hierarchy. */
    public String flatMode()
    {
        return this.gridPrefix + "flatMode";
    }

    /** The text typed into one column's filter box. */
    public String filter(String columnKey)
    {
        return this.gridPrefix + "filter." + columnKey;
    }

    /** The ordered multi-column sort spec, encoded as {@code columnKey:DIRECTION} entries, primary first. */
    public String sort()
    {
        return this.gridPrefix + "sort";
    }

    /** One column's position within the ordered sort spec. */
    public String sortPriority(String columnKey)
    {
        return this.gridPrefix + "sortpriority." + columnKey;
    }

    /** Whether the filter row is shown. */
    public String filtersVisible()
    {
        return this.gridPrefix + "filtersVisible";
    }

    /** The set of expanded node ids. Ignored while the table is flat. */
    public String expandedNodes()
    {
        return this.gridPrefix + "expandedNodes";
    }

    /**
     * The current (possibly load-more-widened) window limit for one sibling group.
     *
     * @param parentNodeId
     *            the group's parent, or {@code null} for the root group - which is spelled {@code "root"} in the
     *            key rather than omitted, so a real node id can never collide with the root's own group.
     */
    public String window(String parentNodeId)
    {
        return this.gridPrefix + (parentNodeId != null ? parentNodeId : "root") + ".windowLimit";
    }

    /**
     * Encodes one entry of the ordered sort spec {@link #sort()} holds.
     * <p>
     * Kept here beside the key itself because a value written in the wrong shape fails exactly like a key written
     * in the wrong shape: the renderer skips entries it cannot decode, defensively and silently.
     */
    public static String encodeSort(String columnKey, String direction)
    {
        return columnKey + ":" + direction;
    }

    /**
     * Whether a key belongs to the grid at {@code gridLocation} - used to clear a table's whole view state
     * without enumerating every key shape it might have written.
     */
    public boolean owns(String fieldKey)
    {
        return fieldKey != null && fieldKey.startsWith(this.gridPrefix);
    }

    /**
     * Every filter key currently present, given the fields a {@code Data} holds. Derived from the live key set
     * rather than from the column list, so a filter left behind by a column that has since been removed is still
     * found and can still be cleared.
     */
    public List<String> filterKeysAmong(Iterable<String> fieldKeys)
    {
        String filterPrefix = this.gridPrefix + "filter.";
        List<String> found = new ArrayList<>();
        fieldKeys.forEach(key ->
        {
            if (key != null && key.startsWith(filterPrefix))
            {
                found.add(key);
            }
        });
        return found;
    }
}
