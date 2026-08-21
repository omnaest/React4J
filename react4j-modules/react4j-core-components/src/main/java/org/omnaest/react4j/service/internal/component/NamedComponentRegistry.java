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
    private final Map<String, Location> nameToLocation = new ConcurrentHashMap<>();

    /**
     * Publishes where a named component landed in this render.
     * <p>
     * Last writer wins, which is what makes a re-render self-correcting: a component that moved republishes its
     * new location, and the old one is simply overwritten. It also means two components sharing a name silently
     * collide - a programming error the framework has no way to distinguish from a legitimate re-registration.
     */
    public void register(String name, Location location)
    {
        if (name != null && location != null)
        {
            this.nameToLocation.put(name, location);
        }
    }

    /**
     * Empty when nothing has rendered under this name - which is a legitimate state, not an error: a page can
     * render without a given component depending on view mode or permissions.
     */
    public Optional<Location> resolve(String name)
    {
        return Optional.ofNullable(name)
                       .map(this.nameToLocation::get);
    }
}
