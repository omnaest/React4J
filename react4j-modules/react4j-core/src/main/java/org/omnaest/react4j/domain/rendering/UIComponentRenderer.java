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
package org.omnaest.react4j.domain.rendering;

import java.util.stream.Stream;

import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.components.LocationSupport;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.domain.rendering.node.NodeRenderType;
import org.omnaest.react4j.domain.rendering.node.NodeRenderer;
import org.omnaest.react4j.domain.rendering.node.NodeRendererRegistry;
import org.omnaest.react4j.service.internal.handler.EventHandlerRegistry;
import org.omnaest.react4j.service.internal.handler.domain.DataEventHandler;
import org.omnaest.react4j.service.internal.handler.domain.EventHandler;

/**
 * Renderer for a particular {@link UIComponent} which creates a {@link Node} structure
 * 
 * @author omnaest
 */
public interface UIComponentRenderer
{
    /**
     * Provides the current {@link Location}
     * 
     * @see LocationSupport#createLocation(String)
     * @param locationSupport
     * @return
     */
    public Location getLocation(LocationSupport locationSupport);

    /**
     * Renders a {@link Node} based on the {@link UIComponent} data
     * 
     * @param renderingProcessor
     * @param location
     * @return
     */
    public Node render(RenderingProcessor renderingProcessor, Location location);

    /**
     * Allows to specify additional {@link NodeRenderer} instances which allow to process the generated {@link Node} to static content like
     * {@link NodeRenderType#HTML}
     * 
     * @param registry
     */
    public void manageNodeRenderers(NodeRendererRegistry registry);

    /**
     * Returns all contained sub {@link UIComponent}s
     * 
     * @return
     */
    public Stream<UIComponent<?>> getSubComponents();

    /**
     * Allows to register {@link EventHandler} to the {@link EventHandlerRegistry}
     * 
     * @param eventHandlerRegistrationSupport
     */
    public void manageEventHandler(EventHandlerRegistrationSupport eventHandlerRegistrationSupport);

    public static interface EventHandlerRegistrationSupport
    {
        public EventHandlerRegistrationSupport register(EventHandler eventHandler);

        public EventHandlerRegistrationSupport register(DataEventHandler eventHandler);

        public EventHandlerRegistrationSupport registerAsRerenderingNode();
    }
}
