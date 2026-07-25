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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer.EventHandlerRegistrationSupport;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.service.internal.handler.domain.EventHandler;
import org.omnaest.react4j.service.internal.nodes.ButtonNode;
import org.omnaest.react4j.service.internal.nodes.handler.ServerHandler;
import org.omnaest.react4j.service.internal.service.LocalizedTextResolverService;

/**
 * Regression test for plan-29 Bug 2a: a {@link ButtonImpl} with no {@code onClick} handler must render with a null
 * {@code onClick} node property and must not register any handler - it must not trigger a server click round-trip at
 * all.
 *
 * @see ButtonImpl
 */
public class ButtonImplTest
{
    private ComponentContext newContext()
    {
        ComponentContext context = mock(ComponentContext.class);
        when(context.getTextResolver()).thenReturn(mock(LocalizedTextResolverService.class));
        return context;
    }

    @Test
    public void testButtonWithOnClickRendersNonNullServerHandlerAndWithoutIsNull()
    {
        ComponentContext context = this.newContext();

        ButtonImpl buttonWithHandler = new ButtonImpl(context);
        buttonWithHandler.onClick(mock(EventHandler.class));

        Location location0 = mock(Location.class);
        when(location0.get()).thenReturn(Arrays.asList("root", buttonWithHandler.getId()));

        ButtonNode node0 = (ButtonNode) buttonWithHandler.asRenderer()
                                                         .render(mock(RenderingProcessor.class), location0, Optional.empty());
        assertNotNull(node0.getOnClick());
        assertTrue(node0.getOnClick() instanceof ServerHandler);

        ButtonImpl buttonWithoutHandler = new ButtonImpl(context);
        Location location1 = mock(Location.class);
        when(location1.get()).thenReturn(Arrays.asList("root", buttonWithoutHandler.getId()));

        ButtonNode node1 = (ButtonNode) buttonWithoutHandler.asRenderer()
                                                            .render(mock(RenderingProcessor.class), location1, Optional.empty());
        assertNull(node1.getOnClick());
    }

    @Test
    public void testManageEventHandlerRegistersOnlyWhenOnClickSet()
    {
        ComponentContext context = this.newContext();

        ButtonImpl buttonWithHandler = new ButtonImpl(context);
        EventHandler handler = mock(EventHandler.class);
        buttonWithHandler.onClick(handler);

        EventHandlerRegistrationSupport supportWithHandler = mock(EventHandlerRegistrationSupport.class);
        UIComponentRenderer rendererWithHandler = buttonWithHandler.asRenderer();
        rendererWithHandler.manageEventHandler(supportWithHandler);
        verify(supportWithHandler, times(1)).register(handler);

        ButtonImpl buttonWithoutHandler = new ButtonImpl(context);
        EventHandlerRegistrationSupport supportWithoutHandler = mock(EventHandlerRegistrationSupport.class);
        UIComponentRenderer rendererWithoutHandler = buttonWithoutHandler.asRenderer();
        rendererWithoutHandler.manageEventHandler(supportWithoutHandler);
        verify(supportWithoutHandler, never()).register(any(EventHandler.class));
    }
}
