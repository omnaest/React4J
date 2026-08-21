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
package org.omnaest.react4j.service.internal.component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.omnaest.react4j.domain.Location;
import org.springframework.stereotype.Service;

/**
 * Where a named component currently sits, so a handler can address it without knowing.
 *
 * <h2>Why a registry at all</h2>
 * Every field a component keeps in the submitted data is keyed by its {@link Location}, which is positional and
 * only known while rendering. A handler runs between two render passes and has no location of its own to work
 * from, so something has to carry the mapping across. This is that something: the render publishes where each
 * named component landed, and the handler looks it up.
 *
 * <h2>Why it is safe to write during a render and read in a handler</h2>
 * {@code handleEvent} renders BEFORE invoking handlers - that first pass exists to re-register handlers, and it
 * re-publishes locations as a side effect. So a handler always reads a mapping produced by a render of the same
 * component tree it is about to affect, not a stale one from an earlier request.
 *
 * <h2>What this deliberately is not</h2>
 * Not component state. It holds only WHERE a component is, never what it contains - the state itself stays in the
 * submitted data, client-owned and carried per round trip. That distinction is what keeps two browser tabs
 * independent: they render the same page to the same locations while holding entirely separate state.
 * <p>
 * The consequence is that this map is safely shared. A location is a property of the page's structure, which is
 * the same for everyone looking at it.
 *
 * @author omnaest
 */
@Service
public class NamedComponentRegistry
{
    /**
     * The context a component dispatches under when it declares none of its own - which is what the client uses
     * as the key for the root ui context.
     */
    public static final String                    ROOT_CONTEXT_ID    = "";

    private final Map<String, Registration>       nameToRegistration = new ConcurrentHashMap<>();

    /**
     * Where a named component is, and which submitted-data context its fields belong to.
     */
    public static class Registration
    {
        private final Location location;

        private final String   contextId;

        Registration(Location location, String contextId)
        {
            this.location = location;
            this.contextId = contextId;
        }

        public Location getLocation()
        {
            return this.location;
        }

        public String getContextId()
        {
            return this.contextId;
        }
    }

    /**
     * Publishes where a named component landed in this render.
     * <p>
     * Last writer wins, which is what makes a re-render self-correcting: a component that moved republishes its
     * new location, and the old one is simply overwritten. It also means two components sharing a name silently
     * collide - a programming error the framework has no way to distinguish from a legitimate re-registration.
     */
    public void register(String name, Location location, String contextId)
    {
        if (name != null && location != null)
        {
            this.nameToRegistration.put(name, new Registration(location, contextId));
        }
    }

    /**
     * Empty when nothing has rendered under this name - which is a legitimate state, not an error: a page can
     * render without a given component depending on view mode or permissions.
     */
    public Optional<Location> resolve(String name)
    {
        return Optional.ofNullable(name)
                       .map(this.nameToRegistration::get)
                       .map(Registration::getLocation);
    }

    public Optional<String> contextIdOf(String name)
    {
        return Optional.ofNullable(name)
                       .map(this.nameToRegistration::get)
                       .map(Registration::getContextId);
    }

    /**
     * Which context a submitted-data field belongs to, judged by the component whose {@link Location} the key was
     * derived from.
     *
     * <h2>Why this is needed at all</h2>
     * A round trip carries every context and the render pass reads them as one flat lookup, so by the time a
     * handler has written something the key's home is no longer obvious. Echoing the flattened result back under
     * the originating context looks harmless and is not: the key then lives in TWO contexts, and the next request
     * carries both. When they disagree - because the user changed the real one in between - whichever the merge
     * prefers wins, and the stale copy is as likely as not.
     * <p>
     * That is not hypothetical. A chat turn set a table to flat; the merged echo left a copy of the mode in the
     * chat form's context; the user then switched the table back; and the next chat message flipped it to flat
     * again from the stale copy, with no tool call involved.
     *
     * <h2>How ownership is decided</h2>
     * By {@link Location}. Components key their submitted fields by their own position in the tree, so a key that
     * contains a registered component's joined location was derived from that component and belongs where that
     * component's own controls write. Nothing here knows what a TreeTable is.
     */
    public Optional<String> contextIdOwning(String fieldKey)
    {
        if (fieldKey == null)
        {
            return Optional.empty();
        }
        return this.nameToRegistration.values()
                                      .stream()
                                      .filter(registration -> fieldKey.contains(String.join(".", registration.getLocation()
                                                                                                             .get())))
                                      .map(Registration::getContextId)
                                      .findFirst();
    }
}
