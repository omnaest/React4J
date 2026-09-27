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

import java.util.function.Consumer;

import org.omnaest.react4j.domain.DropTarget.DropEvent;
import org.omnaest.react4j.domain.support.UIComponentWithContent;

/**
 * A drop target wrapping arbitrary content. A drop delivers the dropped {@link Draggable}'s {@code dragId}
 * together with a {@link DropRelation}, chosen client-side by where within this target's own rendered bounds
 * the pointer was released (top third -&gt; {@link DropRelation#BEFORE}, bottom third -&gt;
 * {@link DropRelation#AFTER}, middle -&gt; {@link DropRelation#INTO}).
 * <p>
 * Composable with {@link Draggable} (plan-235 Cliff 4): a component that is both draggable and a drop target
 * (e.g. a reorderable list item that can also be reparented onto) simply wraps itself in both - reorder and
 * reparent are then the SAME mechanism at different drop positions. The framework knows nothing about cards,
 * columns, boards or any other application vocabulary; the app supplies all of that meaning through
 * {@link #onDrop(Consumer)}.
 * <p>
 * The drag identity rides the existing submitted-{@code Data} channel rather than a new protocol field
 * (plan-235 charter revision S9): the two field keys the client must write into before dispatching a drop are
 * generated server-side from THIS component's own {@code Location} and carried on the rendered node
 * ({@code dragIdFieldKey}/{@code relationFieldKey}) - mirroring the established
 * {@code TreeTableRendererImpl.filterFieldKey(Location, String)} idiom for a client-authored value on this
 * same channel (see {@code TreeTable}'s per-column filter text).
 *
 * @see Draggable
 * @author omnaest
 */
public interface DropTarget extends UIComponentWithContent<DropTarget>
{
    /**
     * Registers the drop handler - opt-in
     * ({@code react4j-gate-optin-handler-on-existing-node}). A {@link DropTarget} this is never called on
     * registers no server handler and emits an empty routable target with {@code null} field keys
     * ({@code react4j-disabled-optional-control-target-empty-not-null}).
     *
     * @param handler
     * @return this
     */
    public DropTarget onDrop(Consumer<DropEvent> handler);

    /**
     * Where, relative to the target's own bounds, a drop landed - chosen client-side by pointer position
     * within the target's rendered element. Emitted verbatim uppercase on the wire
     * ({@code react4j-enum-discriminator-emit-name-verbatim}) - never lower-cased at either end; the client
     * writes the literal {@link Enum#name()} and the server does {@link Enum#valueOf(Class, String)}.
     */
    public static enum DropRelation
    {
        BEFORE, AFTER, INTO
    }

    /**
     * What a drop delivers: the dropped {@link Draggable}'s {@code dragId} and the chosen {@link DropRelation}.
     */
    public static class DropEvent
    {
        private final String       dragId;
        private final DropRelation relation;

        private DropEvent(String dragId, DropRelation relation)
        {
            super();
            this.dragId = dragId;
            this.relation = relation;
        }

        public static DropEvent of(String dragId, DropRelation relation)
        {
            return new DropEvent(dragId, relation);
        }

        public String getDragId()
        {
            return this.dragId;
        }

        public DropRelation getRelation()
        {
            return this.relation;
        }

        @Override
        public String toString()
        {
            return "DropEvent [dragId=" + this.dragId + ", relation=" + this.relation + "]";
        }
    }
}
