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

import org.omnaest.react4j.domain.support.UIComponentWithContent;

/**
 * A client-side drag source wrapping arbitrary content, carrying an app-chosen drag identity.
 * <p>
 * Purely client-side: {@link Draggable} has no server handler and emits no routable {@code Target} of its
 * own - it exists only so the client can mark its rendered content as draggable and attach the chosen
 * {@code dragId} to the browser's native drag payload. A {@link DropTarget} elsewhere on the page is what
 * receives the drop and reaches the server.
 * <p>
 * Domain-free by design (plan-235 Cliff 4): the framework knows nothing about what is being dragged - a
 * card, a row, a tile - only that some content carries an opaque identity an app chooses.
 *
 * @see DropTarget
 * @author omnaest
 */
public interface Draggable extends UIComponentWithContent<Draggable>
{
    /**
     * The app-chosen identity carried by a drag started from this component - opaque to the framework, echoed
     * back verbatim to whichever {@link DropTarget} receives the drop.
     *
     * @param dragId
     * @return this
     */
    public Draggable withDragId(String dragId);
}
