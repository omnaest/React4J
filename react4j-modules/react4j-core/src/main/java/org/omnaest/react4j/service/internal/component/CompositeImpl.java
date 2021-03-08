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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.omnaest.react4j.domain.Composite;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer;
import org.omnaest.react4j.domain.rendering.components.LocationSupport;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.domain.rendering.node.NodeRendererRegistry;
import org.omnaest.react4j.domain.support.UIComponentFactoryFunction;
import org.omnaest.react4j.domain.support.UIComponentProvider;
import org.omnaest.react4j.service.internal.nodes.CompositeNode;

public class CompositeImpl extends AbstractUIComponentWithSubComponents<Composite> implements Composite
{
    private List<UIComponent<?>> components = new ArrayList<>();

    public CompositeImpl(ComponentContext context)
    {
        super(context);
    }

    @Override
    public UIComponentRenderer asRenderer()
    {
        return new UIComponentRenderer()
        {
            @Override
            public Location getLocation(LocationSupport locationSupport)
            {
                return locationSupport.createLocation(CompositeImpl.this.getId());
            }

            @Override
            public Node render(RenderingProcessor renderingProcessor, Location location)
            {
                return new CompositeNode().setElements(CompositeImpl.this.components.stream()
                                                                                    .map(component -> renderingProcessor.process(component, location))
                                                                                    .collect(Collectors.toList()));
            }

            @Override
            public void manageNodeRenderers(NodeRendererRegistry registry)
            {
            }

            @Override
            public void manageEventHandler(EventHandlerRegistrationSupport eventHandlerRegistrationSupport)
            {
            }

            @Override
            public Stream<UIComponent<?>> getSubComponents()
            {
                return CompositeImpl.this.components.stream();
            }

        };
    }

    @Override
    public Composite addNewComponent(UIComponentFactoryFunction factoryConsumer)
    {
        return this.addComponent(factoryConsumer.apply(this.getUiComponentFactory()));
    }

    @Override
    public Composite addComponent(UIComponent<?> component)
    {
        this.components.add(component);
        return this;
    }

    @Override
    public Composite addComponent(UIComponentProvider<?> componentProvider)
    {
        return this.addComponent(componentProvider.get());
    }

    @Override
    public Composite addComponents(List<UIComponent<?>> components)
    {
        Optional.ofNullable(components)
                .ifPresent(consumer -> consumer.forEach(this::addComponent));
        return this;
    }
}
