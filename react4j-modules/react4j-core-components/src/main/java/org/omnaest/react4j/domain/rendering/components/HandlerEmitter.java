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
package org.omnaest.react4j.domain.rendering.components;

import org.omnaest.react4j.service.internal.handler.domain.DataEventHandler;
import org.omnaest.react4j.service.internal.handler.domain.EventHandler;
import org.omnaest.react4j.service.internal.handler.domain.Target;
import org.omnaest.react4j.service.internal.nodes.handler.Handler;

/**
 * plan-78 Cliff C1-A: the handler-emission channel exposed by {@link RenderingProcessor#handlers()} so a
 * component's {@code render(...)} method - which today either constructs {@code new ServerHandler(target)}
 * directly (the 7 {@code manageEventHandler}-based sites, plan-78 finding F3) or registers against an
 * {@link org.omnaest.react4j.service.internal.handler.EventHandlerRegistry} field baked in at construction
 * time (the form-element sites, e.g. {@code FormRendererImpl}, {@code ButtonFormElementImpl}) - can do both
 * registration AND node-DTO construction through ONE call instead.
 * <p>
 * An implementation registers the given handler in whatever scope THIS emitter represents (the real global
 * {@code EventHandlerRegistry} in production, a traversal-scoped capturing map during a
 * {@code HandlerResolver} descent that must never mutate the active handler map) and returns the node-DTO
 * {@link Handler} to embed. A {@code null} handler yields a {@code null} {@link Handler} - mirrors the
 * existing gating at {@code ButtonImpl.render(...)}.
 * </p>
 *
 * @author omnaest
 */
public interface HandlerEmitter
{
    Handler emitDataEventHandler(Target target, DataEventHandler dataEventHandler);

    Handler emitEventHandler(Target target, EventHandler eventHandler);
}
