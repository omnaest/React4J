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

import org.apache.commons.text.StringEscapeUtils;
import org.omnaest.react4j.domain.Breadcrumb;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.context.data.Data;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer;
import org.omnaest.react4j.domain.rendering.components.LocationSupport;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.domain.rendering.node.NodeRenderType;
import org.omnaest.react4j.domain.rendering.node.NodeRendererRegistry;
import org.omnaest.react4j.domain.support.UIComponentProvider;
import org.omnaest.react4j.service.internal.nodes.BreadcrumbEntryNode;
import org.omnaest.react4j.service.internal.nodes.BreadcrumbNode;

public class BreadcrumbImpl extends AbstractUIComponentWithSubComponents<Breadcrumb> implements Breadcrumb
{
    private List<BreadcrumbEntryImpl> entries = new ArrayList<>();
    private String                    locator;

    public BreadcrumbImpl(ComponentContext context)
    {
        super(context);
    }

    public BreadcrumbImpl(ComponentContext context, List<BreadcrumbEntryImpl> entries)
    {
        super(context);
        this.entries = entries;
    }

    public BreadcrumbImpl(ComponentContext context, List<BreadcrumbEntryImpl> entries, String locator)
    {
        super(context);
        this.entries = entries;
        this.locator = locator;
    }

    @Override
    public Breadcrumb withLinkLocator(String locator)
    {
        this.locator = locator;
        return this;
    }

    @Override
    public UIComponentRenderer asRenderer()
    {
        return new UIComponentRenderer() {
            @Override
            public Location getLocation(LocationSupport locationSupport)
            {
                return locationSupport.createLocation(BreadcrumbImpl.this.getId());
            }

            @Override
            public Node render(RenderingProcessor renderingProcessor, Location location, Optional<Data> data)
            {
                return new BreadcrumbNode().setEntries(BreadcrumbImpl.this.entries.stream()
                                                                                  .map(entry -> (BreadcrumbEntryNode) renderingProcessor.process(entry,
                                                                                                                                                 location))
                                                                                  .collect(Collectors.toList()))
                                           .setLocator(BreadcrumbImpl.this.locator);
            }

            @Override
            public void manageNodeRenderers(NodeRendererRegistry registry)
            {
                registry.register(BreadcrumbNode.class, NodeRenderType.HTML,
                                  (node, nodeRenderingProcessor) -> "<nav aria-label=\"breadcrumb\""
                                                                    + (node.getLocator() != null
                                                                            ? " id=\"" + StringEscapeUtils.escapeHtml4(node.getLocator()) + "\""
                                                                            : "")
                                                                    + "><ol class=\"breadcrumb\">" + node.getEntries()
                                                                                                         .stream()
                                                                                                         .map(nodeRenderingProcessor::render)
                                                                                                         .collect(Collectors.joining())
                                                                    + "</ol></nav>");
                registry.register(BreadcrumbEntryNode.class, NodeRenderType.HTML, (entryNode, nodeRenderingProcessor) ->
                {
                    String text = nodeRenderingProcessor.render(entryNode.getText());
                    String link = Optional.ofNullable(entryNode.getLinkedId())
                                          .map(linkedId -> "#" + linkedId)
                                          .orElse(entryNode.getLink());
                    String body = entryNode.isActive() ? text : "<a href=\"" + link + "\">" + text + "</a>";
                    return "<li class=\"breadcrumb-item" + (entryNode.isActive() ? " active" : "") + "\">" + body + "</li>";
                });
            }

            @Override
            public void manageEventHandler(EventHandlerRegistrationSupport eventHandlerRegistrationSupport)
            {
                // each entry registers its own handler in its own manageEventHandler (see BreadcrumbEntryImpl)
            }

            @Override
            public Stream<ParentLocationAndComponent> getSubComponents(Location parentLocation)
            {
                return BreadcrumbImpl.this.entries.stream()
                                                  .map(entry -> ParentLocationAndComponent.of(parentLocation, entry));
            }

        };
    }

    @Override
    public Breadcrumb addEntry(Consumer<BreadcrumbEntry> breadcrumbEntryConsumer)
    {
        BreadcrumbEntryImpl entry = new BreadcrumbEntryImpl(this.context, this.entries.size());
        breadcrumbEntryConsumer.accept(entry);
        this.entries.add(entry);
        return this;
    }

    @Override
    public UIComponentProvider<Breadcrumb> asTemplateProvider()
    {
        return () -> new BreadcrumbImpl(this.context, this.entries.stream()
                                                                  .collect(Collectors.toList()),
                                        this.locator);
    }
}
