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
package org.omnaest.react4j.service.internal.service.internal;

import org.omnaest.react4j.domain.rendering.components.HandlerEmitter;
import org.omnaest.react4j.service.internal.handler.EventHandlerRegistry;
import org.omnaest.react4j.service.internal.handler.domain.DataEventHandler;
import org.omnaest.react4j.service.internal.handler.domain.EventHandler;
import org.omnaest.react4j.service.internal.handler.domain.Target;
import org.omnaest.react4j.service.internal.nodes.handler.Handler;
import org.omnaest.react4j.service.internal.nodes.handler.ServerHandler;

/**
 * plan-78 Cliff C1-A production {@link HandlerEmitter}: delegates registration to the real
 * {@link EventHandlerRegistry} and returns a fresh {@link ServerHandler} for the node DTO.
 *
 * @author omnaest
 */
public class HandlerEmitterImpl implements HandlerEmitter
{
    private final EventHandlerRegistry eventHandlerRegistry;

    public HandlerEmitterImpl(EventHandlerRegistry eventHandlerRegistry)
    {
        this.eventHandlerRegistry = eventHandlerRegistry;
    }

    @Override
    public Handler emitDataEventHandler(Target target, DataEventHandler dataEventHandler)
    {
        if (dataEventHandler == null)
        {
            return null;
        }
        this.eventHandlerRegistry.registerDataEventHandler(target, dataEventHandler);
        return new ServerHandler(target);
    }

    @Override
    public Handler emitEventHandler(Target target, EventHandler eventHandler)
    {
        if (eventHandler == null)
        {
            return null;
        }
        this.eventHandlerRegistry.registerEventHandler(target, eventHandler);
        return new ServerHandler(target);
    }
}
