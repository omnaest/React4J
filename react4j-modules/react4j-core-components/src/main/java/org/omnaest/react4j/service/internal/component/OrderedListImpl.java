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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.OrderedList;
import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.context.data.Data;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer;
import org.omnaest.react4j.domain.rendering.components.LocationSupport;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.domain.rendering.node.NodeRenderType;
import org.omnaest.react4j.domain.rendering.node.NodeRenderer;
import org.omnaest.react4j.domain.rendering.node.NodeRendererRegistry;
import org.omnaest.react4j.domain.rendering.node.NodeRenderingProcessor;
import org.omnaest.react4j.domain.support.UIComponentProvider;
import org.omnaest.react4j.service.internal.nodes.OrderedListNode;
import org.omnaest.utils.MapperUtils;
import org.omnaest.utils.template.TemplateUtils;

public class OrderedListImpl extends AbstractUIComponent<OrderedList> implements OrderedList
{
    private static final int     DEFAULT_START_NUMBER = 1;

    private List<UIComponent<?>> elements             = new ArrayList<>();
    private int                  startNumber          = DEFAULT_START_NUMBER;

    public OrderedListImpl(ComponentContext context)
    {
        super(context);
    }

    public OrderedListImpl(ComponentContext context, List<UIComponent<?>> elements, int startNumber)
    {
        super(context);
        this.elements = elements;
        this.startNumber = startNumber;
    }

    @Override
    public UIComponentRenderer asRenderer()
    {
        return new UIComponentRenderer() {
            @Override
            public Location getLocation(LocationSupport locationSupport)
            {
                return locationSupport.createLocation(OrderedListImpl.this.getId());
            }

            @Override
            public Node render(RenderingProcessor renderingProcessor, Location location, Optional<Data> data)
            {
                return new OrderedListNode().setStartNumber(OrderedListImpl.this.startNumber)
                                            .setElements(OrderedListImpl.this.elements.stream()
                                                                                      .map(MapperUtils.withIntCounter())
                                                                                      .map(componentAndIndex -> renderingProcessor.process(componentAndIndex.getFirst(),
                                                                                                                                           ChildLocationSupport.indexedChildLocation(location,
                                                                                                                                                                                     componentAndIndex.getSecond())))
                                                                                      .collect(Collectors.toList()));
            }

            @Override
            public void manageNodeRenderers(NodeRendererRegistry registry)
            {
                registry.register(OrderedListNode.class, NodeRenderType.HTML, new NodeRenderer<OrderedListNode>() {
                    @Override
                    public String render(OrderedListNode node, NodeRenderingProcessor nodeRenderingProcessor)
                    {
                        // the default start number is the only one that renders no attribute
                        String startAttribute = node.getStartNumber() == DEFAULT_START_NUMBER ? "" : " start=\"" + node.getStartNumber() + "\"";
                        return TemplateUtils.builder()
                                            .useTemplateClassResource(this.getClass(), "/render/templates/html/ordered_list.html")
                                            .add("startAttribute", startAttribute)
                                            .add("items", node.getElements()
                                                              .stream()
                                                              .map(nodeRenderingProcessor::render)
                                                              .collect(Collectors.toList()))
                                            .build()
                                            .get();
                    }
                });
            }

            @Override
            public void manageEventHandler(EventHandlerRegistrationSupport eventHandlerRegistrationSupport)
            {
            }

            @Override
            public Stream<ParentLocationAndComponent> getSubComponents(Location parentLocation)
            {
                return OrderedListImpl.this.elements.stream()
                                                    .map(MapperUtils.withIntCounter())
                                                    .map(componentAndIndex -> ParentLocationAndComponent.of(ChildLocationSupport.indexedChildLocation(parentLocation,
                                                                                                                                                      componentAndIndex.getSecond()),
                                                                                                            componentAndIndex.getFirst()));
            }

        };
    }

    @Override
    public OrderedList addText(String text)
    {
        this.elements.add(this.getUiComponentFactory()
                              .newText()
                              .addText(text));
        return this;
    }

    @Override
    public OrderedList addEntry(UIComponent<?> component)
    {
        this.elements.add(component);
        return this;
    }

    @Override
    public OrderedList addEntries(List<UIComponent<?>> components)
    {
        Optional.ofNullable(components)
                .orElse(Collections.emptyList())
                .forEach(this::addEntry);
        return this;
    }

    @Override
    public OrderedList withStartNumber(int startNumber)
    {
        this.startNumber = startNumber;
        return this;
    }

    @Override
    public UIComponentProvider<OrderedList> asTemplateProvider()
    {
        return () -> new OrderedListImpl(this.context, this.elements.stream()
                                                                    .collect(Collectors.toList()),
                                         this.startNumber);
    }

}
