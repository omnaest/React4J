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
package org.omnaest.react4j.service.internal.handler;

import java.util.Optional;

import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.context.data.Data;
import org.omnaest.react4j.service.internal.handler.domain.DataEventHandler;
import org.omnaest.react4j.service.internal.handler.domain.Target;

/**
 * plan-78 Slice 1: resolves the {@link DataEventHandler} for a {@link Target} by descending the (cached)
 * component tree to the {@link Location} the {@link Target} denotes, instead of looking it up in
 * {@link EventHandlerRegistry}'s side-effect-populated map.
 * <p>
 * Additive: this slice wires {@link HandlerResolver} in as a FALLBACK only, consulted by
 * {@code EventHandlerServiceImpl.handleEvent} when the active handler map misses. The map stays authoritative;
 * nothing about {@link EventHandlerRegistry#registerDataEventHandler(Target, DataEventHandler)} or the plan-12
 * two-pass ordering changes.
 * </p>
 *
 * @author omnaest
 */
public interface HandlerResolver
{
    /**
     * Resolves the {@link DataEventHandler} registered at the {@link Location} the given {@link Target}
     * denotes, by descending the current (single-root, {@code DEFAULT_CONTEXT_PATH} - see plan-78 Cliff C2)
     * component tree.
     *
     * @param target
     * @param data
     * @return
     */
    Optional<DataEventHandler> resolve(Target target, Optional<Data> data);
}
