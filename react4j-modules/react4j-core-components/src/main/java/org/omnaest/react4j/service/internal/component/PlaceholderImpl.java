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

import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.Placeholder;
import org.omnaest.react4j.domain.context.data.Data;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer;
import org.omnaest.react4j.domain.rendering.components.LocationSupport;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.domain.rendering.node.NodeRenderType;
import org.omnaest.react4j.domain.rendering.node.NodeRendererRegistry;
import org.omnaest.react4j.domain.support.UIComponentProvider;
import org.omnaest.react4j.service.internal.nodes.PlaceholderNode;

public class PlaceholderImpl extends AbstractUIComponent<Placeholder> implements Placeholder
{
    private Style     style;
    private Size      size;
    private int       columns = 12;
    private Animation animation;

    public PlaceholderImpl(ComponentContext context)
    {
        super(context);
    }

    public PlaceholderImpl(ComponentContext context, Style style, Size size, int columns, Animation animation)
    {
        super(context);
        this.style = style;
        this.size = size;
        this.columns = columns;
        this.animation = animation;
    }

    @Override
    public Placeholder withStyle(Style style)
    {
        this.style = style;
        return this;
    }

    @Override
    public Placeholder withSize(Size size)
    {
        this.size = size;
        return this;
    }

    @Override
    public Placeholder withColumns(int columns)
    {
        this.columns = columns;
        return this;
    }

    @Override
    public Placeholder withAnimation(Animation animation)
    {
        this.animation = animation;
        return this;
    }

    @Override
    public UIComponentRenderer asRenderer()
    {
        return new UIComponentRenderer() {
            @Override
            public Location getLocation(LocationSupport locationSupport)
            {
                return locationSupport.createLocation(PlaceholderImpl.this.getId());
            }

            @Override
            public Node render(RenderingProcessor renderingProcessor, Location location, Optional<Data> data)
            {
                return new PlaceholderNode().setStyle(Optional.ofNullable(PlaceholderImpl.this.style)
                                                              .map(Style::name)
                                                              .map(String::toLowerCase)
                                                              .orElse(null))
                                            .setSize(Optional.ofNullable(PlaceholderImpl.this.size)
                                                             .map(Size::name)
                                                             .map(String::toLowerCase)
                                                             .orElse(null))
                                            .setColumns(PlaceholderImpl.this.columns)
                                            .setAnimation(Optional.ofNullable(PlaceholderImpl.this.animation)
                                                                  .map(Animation::name)
                                                                  .map(String::toLowerCase)
                                                                  .orElse(null));
            }

            @Override
            public void manageNodeRenderers(NodeRendererRegistry registry)
            {
                registry.register(PlaceholderNode.class, NodeRenderType.HTML, (node, nodeRenderingProcessor) ->
                {
                    List<String> classes = new ArrayList<>();
                    classes.add("placeholder");
                    classes.add("col-" + node.getColumns());
                    Optional.ofNullable(node.getSize())
                            .ifPresent(size -> classes.add("placeholder-" + size));
                    Optional.ofNullable(node.getStyle())
                            .ifPresent(style -> classes.add("bg-" + style));
                    Optional.ofNullable(node.getAnimation())
                            .ifPresent(animation -> classes.add("placeholder-" + animation));
                    return "<span class=\"" + classes.stream()
                                                     .collect(Collectors.joining(" "))
                           + "\"></span>";
                });
            }

            @Override
            public void manageEventHandler(EventHandlerRegistrationSupport eventHandlerRegistrationSupport)
            {
            }

            @Override
            public Stream<ParentLocationAndComponent> getSubComponents(Location parentLocation)
            {
                return Stream.empty();
            }

        };
    }

    @Override
    public UIComponentProvider<Placeholder> asTemplateProvider()
    {
        return () -> new PlaceholderImpl(this.context, this.style, this.size, this.columns, this.animation);
    }
}
