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

import java.util.Optional;

import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.context.data.Data;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.service.internal.handler.domain.DataEventHandler;
import org.omnaest.react4j.service.internal.handler.domain.EventHandler;
import org.omnaest.react4j.service.internal.handler.domain.Target;
import org.omnaest.react4j.service.internal.nodes.handler.Handler;

public interface RenderingProcessor
{
    public Node process(UIComponent<?> component, Location parentLocation, Optional<Data> data);

    public default Node process(UIComponent<?> component, Location parentLocation)
    {
        return this.process(component, parentLocation, Optional.empty());
    }

    /**
     * The handler-emission channel (plan-78 Cliff C1-A) for the component currently being rendered. Defaults
     * to a no-op {@link HandlerEmitter} that emits {@code null} for every call, so an implementation that has
     * no real emitter to offer (and any hand-rolled anonymous {@link RenderingProcessor} that does not
     * override this method) stays behaviorally inert rather than throwing.
     * <p>
     * Note for callers: a raw Mockito {@code mock(RenderingProcessor.class)} does NOT invoke this default -
     * Mockito stubs every method, including default ones, to its default answer ({@code null}) unless
     * explicitly configured otherwise. Call sites that must stay compatible with such mocks (and with the
     * {@code render(Location)} compatibility overload used elsewhere) treat a {@code null} {@link #handlers()}
     * result the same as this no-op emitter.
     * </p>
     *
     * @return
     */
    public default HandlerEmitter handlers()
    {
        return new HandlerEmitter() {
            @Override
            public Handler emitDataEventHandler(Target target, DataEventHandler dataEventHandler)
            {
                return null;
            }

            @Override
            public Handler emitEventHandler(Target target, EventHandler eventHandler)
            {
                return null;
            }
        };
    }
}
