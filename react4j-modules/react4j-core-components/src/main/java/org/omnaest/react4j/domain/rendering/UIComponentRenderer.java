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

import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.context.data.Data;
import org.omnaest.react4j.domain.context.data.source.registry.DataSourceRegistry;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.components.LocationSupport;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.domain.rendering.node.NodeRenderType;
import org.omnaest.react4j.domain.rendering.node.NodeRenderer;
import org.omnaest.react4j.domain.rendering.node.NodeRendererRegistry;
import org.omnaest.react4j.service.internal.handler.EventHandlerRegistry;
import org.omnaest.react4j.service.internal.handler.domain.DataEventHandler;
import org.omnaest.react4j.service.internal.handler.domain.EventHandler;
import org.omnaest.utils.element.bi.BiElement;

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
     * @param data
     * @return
     */
    public Node render(RenderingProcessor renderingProcessor, Location location, Optional<Data> data);

    /**
     * Allows to specify additional {@link NodeRenderer} instances which allow to process the generated {@link Node} to static content like
     * {@link NodeRenderType#HTML}
     * 
     * @param registry
     */
    public void manageNodeRenderers(NodeRendererRegistry registry);

    /**
     * Returns all contained sub {@link UIComponent}s and their additional parent location
     * 
     * @param parentLocation
     * @return
     */
    public Stream<ParentLocationAndComponent> getSubComponents(Location parentLocation);

    /**
     * Allows to register {@link EventHandler} to the {@link EventHandlerRegistry}
     * 
     * @param eventHandlerRegistrationSupport
     */
    public default void manageEventHandler(EventHandlerRegistrationSupport eventHandlerRegistrationSupport)
    {
        // do nothing by default
    }

    public default void manageDataSources(DataSourceRegistry registry, Location location)
    {
        // do nothing by default
    }

    public static interface EventHandlerRegistrationSupport
    {
        public EventHandlerRegistrationSupport register(EventHandler eventHandler);

        public EventHandlerRegistrationSupport register(DataEventHandler eventHandler);

        public EventHandlerRegistrationSupport registerAsRerenderingNode();
    }

    public static class ParentLocationAndComponent
    {
        private UIComponent<?> component;
        private Location       parentLocation;

        protected ParentLocationAndComponent(UIComponent<?> component, Location parentLocation)
        {
            super();
            this.component = component;
            this.parentLocation = parentLocation;
        }

        public UIComponent<?> getComponent()
        {
            return this.component;
        }

        public Location getParentLocation()
        {
            return this.parentLocation;
        }

        @Override
        public String toString()
        {
            return "ParentLocationAndComponent [component=" + this.component + ", parentLocation=" + this.parentLocation + "]";
        }

        public static ParentLocationAndComponent of(Location parentLocation, UIComponent<?> component)
        {
            return new ParentLocationAndComponent(component, parentLocation);
        }

        public BiElement<Location, RenderableUIComponent<?>> applyToComponent(Function<UIComponent<?>, RenderableUIComponent<?>> mapper)
        {
            return BiElement.of(this.parentLocation, this.component)
                            .<RenderableUIComponent<?>>applyToSecondArgument(mapper::apply);
        }
    }
}
