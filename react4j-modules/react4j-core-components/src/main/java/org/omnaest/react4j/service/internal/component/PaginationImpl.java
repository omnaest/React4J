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
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.Pagination;
import org.omnaest.react4j.domain.Pagination.PaginationItem;
import org.omnaest.react4j.domain.context.data.Data;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer;
import org.omnaest.react4j.domain.rendering.components.LocationSupport;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.domain.rendering.node.NodeRenderType;
import org.omnaest.react4j.domain.rendering.node.NodeRendererRegistry;
import org.omnaest.react4j.domain.support.UIComponentProvider;
import org.omnaest.react4j.service.internal.nodes.PaginationItemNode;
import org.omnaest.react4j.service.internal.nodes.PaginationNode;

public class PaginationImpl extends AbstractUIComponentWithSubComponents<Pagination> implements Pagination
{
    private List<PaginationItemImpl> items = new ArrayList<>();

    public PaginationImpl(ComponentContext context)
    {
        super(context);
    }

    public PaginationImpl(ComponentContext context, List<PaginationItemImpl> items)
    {
        super(context);
        this.items = items;
    }

    @Override
    public Pagination addItem(Consumer<PaginationItem> paginationItemConsumer)
    {
        PaginationItemImpl item = new PaginationItemImpl(this.context, this.items.size());
        paginationItemConsumer.accept(item);
        this.items.add(item);
        return this;
    }

    @Override
    public UIComponentRenderer asRenderer()
    {
        return new UIComponentRenderer() {
            @Override
            public Location getLocation(LocationSupport locationSupport)
            {
                return locationSupport.createLocation(PaginationImpl.this.getId());
            }

            @Override
            public Node render(RenderingProcessor renderingProcessor, Location location, Optional<Data> data)
            {
                return new PaginationNode().setEntries(PaginationImpl.this.items.stream()
                                                                                .map(item -> (PaginationItemNode) renderingProcessor.process(item,
                                                                                                                                             location))
                                                                                .collect(Collectors.toList()));
            }

            @Override
            public void manageNodeRenderers(NodeRendererRegistry registry)
            {
                registry.register(PaginationNode.class, NodeRenderType.HTML,
                                  (node, nodeRenderingProcessor) -> "<nav aria-label=\"pagination\"><ul class=\"pagination\">" + node.getEntries()
                                                                                                                                     .stream()
                                                                                                                                     .map(nodeRenderingProcessor::render)
                                                                                                                                     .collect(Collectors.joining())
                                                                    + "</ul></nav>");
                registry.register(PaginationItemNode.class, NodeRenderType.HTML, (node, nodeRenderingProcessor) ->
                {
                    String stateClass = (node.isActive() ? " active" : "") + (node.isDisabled() ? " disabled" : "");
                    return "<li class=\"page-item" + stateClass + "\"><span class=\"page-link\">" + nodeRenderingProcessor.render(node.getLabel())
                           + "</span></li>";
                });
            }

            @Override
            public void manageEventHandler(EventHandlerRegistrationSupport eventHandlerRegistrationSupport)
            {
            }

            @Override
            public Stream<ParentLocationAndComponent> getSubComponents(Location parentLocation)
            {
                return PaginationImpl.this.items.stream()
                                                .map(item -> ParentLocationAndComponent.of(parentLocation, item));
            }

        };
    }

    @Override
    public UIComponentProvider<Pagination> asTemplateProvider()
    {
        return () -> new PaginationImpl(this.context, this.items.stream()
                                                                .collect(Collectors.toList()));
    }
}
