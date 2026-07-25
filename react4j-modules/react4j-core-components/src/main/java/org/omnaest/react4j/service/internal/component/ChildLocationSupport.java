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

import java.util.Optional;

import org.omnaest.react4j.domain.Location;

/**
 * Shared helper generating a positional/index based intermediate {@link Location} segment for a child of a container that holds an ordered list of
 * arbitrary {@link org.omnaest.react4j.domain.UIComponent} children.
 * <p>
 * A component's {@link Location} must be BOTH stable across re-instantiation of the same structural position (so that a re-created component reproduces
 * the same {@link org.omnaest.react4j.service.internal.handler.domain.Target} the client already holds) AND unique across same-type siblings (so a
 * container holding multiple children of the same type does not have them collide onto the same {@link Location}). The container's own child index
 * satisfies both: it is a pure function of list position (deterministic + stable) and it differs for every sibling.
 * <p>
 * This mirrors the precedent already established by {@code GridContainerImpl}/{@code TableRendererImpl}'s {@code createCellLocation} helper, which is
 * called identically from both {@code render()} and {@code getSubComponents()} so the render walk and the registration walk always agree on the
 * Location of a given child. Every container using this helper MUST call it identically from both methods, enumerating the SAME list in the SAME order.
 *
 * @author omnaest
 */
final class ChildLocationSupport
{
    private ChildLocationSupport()
    {
        // helper only, no instances
    }

    /**
     * Returns {@code parentLocation.and("component" + index)}, or {@code null} if the given {@link Location} is {@code null}.
     *
     * @param parentLocation
     * @param index
     * @return
     */
    public static Location indexedChildLocation(Location parentLocation, int index)
    {
        return Optional.ofNullable(parentLocation)
                       .map(location -> location.and("component" + index))
                       .orElse(null);
    }

}
