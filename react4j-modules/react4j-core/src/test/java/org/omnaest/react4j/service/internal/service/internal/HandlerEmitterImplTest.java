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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.omnaest.react4j.domain.rendering.components.HandlerEmitter;
import org.omnaest.react4j.service.internal.handler.EventHandlerRegistry;
import org.omnaest.react4j.service.internal.handler.domain.DataEventHandler;
import org.omnaest.react4j.service.internal.handler.domain.DataEventHandler.MappedData;
import org.omnaest.react4j.service.internal.handler.domain.EventHandler;
import org.omnaest.react4j.service.internal.handler.domain.Target;
import org.omnaest.react4j.service.internal.nodes.handler.Handler;
import org.omnaest.react4j.service.internal.nodes.handler.ServerHandler;

/**
 * plan-78 Cliff C1-A: unit test for the production {@link HandlerEmitter} at the algorithmic-node boundary
 * (design-test-coverage classification: zero mocks, a plain in-memory fake {@link EventHandlerRegistry}
 * instead - mirrors the {@code FakeEventHandlerRegistry} precedent already used in
 * {@code InputFormElementImplTest} / {@code FileUploadFormElementImplTest}).
 */
public class HandlerEmitterImplTest
{
    private final FakeEventHandlerRegistry registry = new FakeEventHandlerRegistry();
    private final HandlerEmitterImpl       emitter  = new HandlerEmitterImpl(this.registry);

    @Test
    public void testEmitDataEventHandlerRegistersAndReturnsServerHandlerCarryingTarget()
    {
        Target target = Target.from(() -> java.util.List.of("root", "form", "button[1]"));
        DataEventHandler dataEventHandler = (data, internalData) -> MappedData.builder()
                                                                              .data(data)
                                                                              .internalData(internalData)
                                                                              .build();

        Handler handler = this.emitter.emitDataEventHandler(target, dataEventHandler);

        assertTrue(handler instanceof ServerHandler);
        assertEquals(target, ((ServerHandler) handler).getTarget());
        assertSame(dataEventHandler, this.registry.dataHandlers.get(target));
    }

    @Test
    public void testEmitDataEventHandlerWithNullHandlerReturnsNullAndDoesNotRegister()
    {
        Target target = Target.from(() -> java.util.List.of("root", "form", "button[2]"));

        Handler handler = this.emitter.emitDataEventHandler(target, null);

        assertNull(handler);
        assertFalse(this.registry.dataHandlers.containsKey(target));
    }

    @Test
    public void testEmitEventHandlerRegistersAndReturnsServerHandlerCarryingTarget()
    {
        Target target = Target.from(() -> java.util.List.of("root", "button"));
        EventHandler eventHandler = () ->
        {
        };

        Handler handler = this.emitter.emitEventHandler(target, eventHandler);

        assertTrue(handler instanceof ServerHandler);
        assertEquals(target, ((ServerHandler) handler).getTarget());
        assertSame(eventHandler, this.registry.eventHandlers.get(target));
    }

    @Test
    public void testEmitEventHandlerWithNullHandlerReturnsNullAndDoesNotRegister()
    {
        Target target = Target.from(() -> java.util.List.of("root", "button"));

        Handler handler = this.emitter.emitEventHandler(target, null);

        assertNull(handler);
        assertFalse(this.registry.eventHandlers.containsKey(target));
    }

    private static class FakeEventHandlerRegistry implements EventHandlerRegistry
    {
        private final Map<Target, DataEventHandler> dataHandlers  = new HashMap<>();
        private final Map<Target, EventHandler>     eventHandlers = new HashMap<>();

        @Override
        public void registerEventHandler(Target target, EventHandler eventHandler)
        {
            this.eventHandlers.put(target, eventHandler);
        }

        @Override
        public void registerDataEventHandler(Target target, DataEventHandler eventHandler)
        {
            this.dataHandlers.put(target, eventHandler);
        }
    }
}
