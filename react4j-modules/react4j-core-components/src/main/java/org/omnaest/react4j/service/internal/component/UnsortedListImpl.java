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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.omnaest.react4j.domain.Icon;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.UnsortedList;
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
import org.omnaest.react4j.service.internal.nodes.UnsortedListNode;
import org.omnaest.utils.template.TemplateUtils;

public class UnsortedListImpl extends AbstractUIComponent<UnsortedList> implements UnsortedList
{
    private List<UIComponent<?>> elements           = new ArrayList<>();
    private boolean              enableBulletPoints = false;

    public UnsortedListImpl(ComponentContext context)
    {
        super(context);
    }

    public UnsortedListImpl(ComponentContext context, List<UIComponent<?>> elements, boolean enableBulletPoints)
    {
        super(context);
        this.elements = elements;
        this.enableBulletPoints = enableBulletPoints;
    }

    @Override
    public UIComponentRenderer asRenderer()
    {
        return new UIComponentRenderer()
        {
            @Override
            public Location getLocation(LocationSupport locationSupport)
            {
                return locationSupport.createLocation(UnsortedListImpl.this.getId());
            }

            @Override
            public Node render(RenderingProcessor renderingProcessor, Location location, Optional<Data> data)
            {
                return new UnsortedListNode().setEnableBulletPoints(UnsortedListImpl.this.enableBulletPoints)
                                             .setElements(UnsortedListImpl.this.elements.stream()
                                                                                        .map(component -> renderingProcessor.process(component, location))
                                                                                        .collect(Collectors.toList()));
            }

            @Override
            public void manageNodeRenderers(NodeRendererRegistry registry)
            {
                registry.register(UnsortedListNode.class, NodeRenderType.HTML, new NodeRenderer<UnsortedListNode>()
                {
                    @Override
                    public String render(UnsortedListNode node, NodeRenderingProcessor nodeRenderingProcessor)
                    {
                        return TemplateUtils.builder()
                                            .useTemplateClassResource(this.getClass(), "/render/templates/html/unsorted_list.html")
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
                return UnsortedListImpl.this.elements.stream()
                                                     .map(component -> ParentLocationAndComponent.of(parentLocation, component));
            }

        };
    }

    @Override
    public UnsortedList addText(Icon.StandardIcon icon, String text)
    {
        this.elements.add(this.getUiComponentFactory()
                              .newComposite()
                              .addComponents(Arrays.asList(this.getUiComponentFactory()
                                                               .newIcon()
                                                               .from(icon),
                                                           this.getUiComponentFactory()
                                                               .newText()
                                                               .addText(text))));
        return this;
    }

    @Override
    public UnsortedList addText(String text)
    {
        return this.addText(null, text);
    }

    @Override
    public UnsortedList enableBulletPoints()
    {
        return this.enableBulletPoints(true);
    }

    @Override
    public UnsortedList enableBulletPoints(boolean enableBulletPoints)
    {
        this.enableBulletPoints = enableBulletPoints;
        return this;
    }

    @Override
    public UnsortedList addEntry(UIComponent<?> component)
    {
        this.elements.add(component);
        return this;
    }

    @Override
    public UnsortedList addEntries(List<UIComponent<?>> components)
    {
        Optional.ofNullable(components)
                .orElse(Collections.emptyList())
                .forEach(this::addEntry);
        return this;
    }

    @Override
    public UIComponentProvider<UnsortedList> asTemplateProvider()
    {
        return () -> new UnsortedListImpl(this.context, this.elements.stream()
                                                                     .collect(Collectors.toList()),
                                          this.enableBulletPoints);
    }

}
