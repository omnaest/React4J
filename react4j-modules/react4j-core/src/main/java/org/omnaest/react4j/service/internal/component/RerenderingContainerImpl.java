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

import java.util.Optional;
import java.util.stream.Stream;

import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.RerenderingContainer;
import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.context.data.Data;
import org.omnaest.react4j.domain.context.ui.UIContext;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer;
import org.omnaest.react4j.domain.rendering.components.LocationSupport;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.domain.rendering.node.NodeRendererRegistry;
import org.omnaest.react4j.domain.support.UIComponentProvider;
import org.omnaest.react4j.domain.support.UIComponentProviderWithData;
import org.omnaest.react4j.service.internal.handler.domain.Target;
import org.omnaest.react4j.service.internal.nodes.RerenderingContainerNode;
import org.omnaest.react4j.service.internal.nodes.context.UIContextNode;

public class RerenderingContainerImpl extends AbstractUIComponentAndContentHolder<RerenderingContainer> implements RerenderingContainer
{
    private UIComponentProviderWithData<?> content;
    private UIContext                      uiContext;
    private boolean                        enableStaticNodeRerendering = false;

    public RerenderingContainerImpl(ComponentContext context)
    {
        super(context);
    }

    public RerenderingContainerImpl(ComponentContext context, UIComponentProviderWithData<?> content, UIContext uiContext, boolean enableStaticNodeRerendering)
    {
        super(context);
        this.content = content;
        this.uiContext = uiContext;
        this.enableStaticNodeRerendering = enableStaticNodeRerendering;
    }

    @Override
    public UIComponentRenderer asRenderer()
    {
        return new UIComponentRenderer()
        {
            @Override
            public Location getLocation(LocationSupport locationSupport)
            {
                return locationSupport.createLocation(RerenderingContainerImpl.this.getId());
            }

            @Override
            public Node render(RenderingProcessor renderingProcessor, Location location, Optional<Data> data)
            {
                return new RerenderingContainerNode().setContent(Optional.ofNullable(RerenderingContainerImpl.this.content)
                                                                         .map(contentProvider -> contentProvider.apply(data.orElse(Data.empty())))
                                                                         .map(content -> renderingProcessor.process(content, location))
                                                                         .orElse(null))
                                                     .setUiContext(Optional.ofNullable(RerenderingContainerImpl.this.uiContext)
                                                                           .map(uiContext -> new UIContextNode().setContextId(uiContext.getId(location)))
                                                                           .orElse((UIContextNode) null))
                                                     .setEnableNodeReload(RerenderingContainerImpl.this.enableStaticNodeRerendering)
                                                     .setTarget(Target.from(location));
            }

            @Override
            public void manageEventHandler(EventHandlerRegistrationSupport eventHandlerRegistrationSupport)
            {
                eventHandlerRegistrationSupport.registerAsRerenderingNode();
            }

            @Override
            public void manageNodeRenderers(NodeRendererRegistry registry)
            {
            }

            @Override
            public Stream<ParentLocationAndComponent> getSubComponents(Location parentLocation)
            {
                return Stream.of(ParentLocationAndComponent.of(parentLocation, RerenderingContainerImpl.this.content.apply(Data.empty())));
            }

        };
    }

    @Override
    public RerenderingContainer withContent(UIComponent<?> component)
    {
        return this.withContent(() -> component);
    }

    @Override
    public RerenderingContainer withContent(UIComponentProvider<?> componentProvider)
    {
        this.content = data -> componentProvider.get();
        return this;
    }

    @Override
    public RerenderingContainer withDataDrivenContent(UIComponentProviderWithData<?> componentProvider)
    {
        this.content = data -> componentProvider.apply(data);
        return this;
    }

    @Override
    public RerenderingContainer withUIContext(UIContext uiContext)
    {
        this.uiContext = uiContext;
        return this;
    }

    @Override
    public RerenderingContainer disableStaticNodeRerendering()
    {
        return this.disableStaticNodeRerendering(true);
    }

    @Override
    public RerenderingContainer disableStaticNodeRerendering(boolean disableNodeRerendering)
    {
        this.enableStaticNodeRerendering = !disableNodeRerendering;
        return this;
    }

    @Override
    public RerenderingContainer enableStaticNodeRerendering()
    {
        return this.enableStaticNodeRerendering(true);
    }

    @Override
    public RerenderingContainer enableStaticNodeRerendering(boolean enableNodeRerendering)
    {
        this.enableStaticNodeRerendering = enableNodeRerendering;
        return this;
    }

    @Override
    public UIComponentProvider<RerenderingContainer> asTemplateProvider()
    {
        return () -> new RerenderingContainerImpl(this.context, this.content, this.uiContext, this.enableStaticNodeRerendering);
    }

}
