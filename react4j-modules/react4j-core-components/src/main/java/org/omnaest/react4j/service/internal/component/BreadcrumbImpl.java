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
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.omnaest.react4j.domain.Breadcrumb;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.context.data.Data;
import org.omnaest.react4j.domain.i18n.I18nText;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer;
import org.omnaest.react4j.domain.rendering.components.LocationSupport;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.domain.rendering.node.NodeRenderType;
import org.omnaest.react4j.domain.rendering.node.NodeRendererRegistry;
import org.omnaest.react4j.domain.rendering.node.NodeRenderingProcessor;
import org.omnaest.react4j.domain.support.UIComponentProvider;
import org.omnaest.react4j.service.internal.nodes.BreadcrumbNode;

public class BreadcrumbImpl extends AbstractUIComponent<Breadcrumb> implements Breadcrumb
{
    private List<BreadcrumbEntryImpl> entries = new ArrayList<>();

    public BreadcrumbImpl(ComponentContext context)
    {
        super(context);
    }

    public BreadcrumbImpl(ComponentContext context, List<BreadcrumbEntryImpl> entries)
    {
        super(context);
        this.entries = entries;
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
                                                                                  .map(entry -> new BreadcrumbNode.Entry().setActive(entry.isActive())
                                                                                                                          .setLink(entry.getLink())
                                                                                                                          .setLinkedId(entry.getLinkedId())
                                                                                                                          .setText(BreadcrumbImpl.this.getTextResolver()
                                                                                                                                                      .apply(entry.getText(),
                                                                                                                                                             location)))
                                                                                  .collect(Collectors.toList()));
            }

            @Override
            public void manageNodeRenderers(NodeRendererRegistry registry)
            {
                registry.register(BreadcrumbNode.class, NodeRenderType.HTML,
                                  (node, nodeRenderingProcessor) -> "<nav aria-label=\"breadcrumb\"><ol class=\"breadcrumb\">" + node.getEntries()
                                                                                                                                     .stream()
                                                                                                                                     .map(entry -> this.renderEntry(entry,
                                                                                                                                                                    nodeRenderingProcessor))
                                                                                                                                     .collect(Collectors.joining())
                                                                    + "</ol></nav>");
            }

            private String renderEntry(BreadcrumbNode.Entry entry, NodeRenderingProcessor nodeRenderingProcessor)
            {
                String text = nodeRenderingProcessor.render(entry.getText());
                String link = Optional.ofNullable(entry.getLinkedId())
                                      .map(linkedId -> "#" + linkedId)
                                      .orElse(entry.getLink());
                String body = entry.isActive() ? text : "<a href=\"" + link + "\">" + text + "</a>";
                return "<li class=\"breadcrumb-item" + (entry.isActive() ? " active" : "") + "\">" + body + "</li>";
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
    public Breadcrumb addEntry(Consumer<BreadcrumbEntry> breadcrumbEntryConsumer)
    {
        BreadcrumbEntryImpl entry = new BreadcrumbEntryImpl(text -> this.toI18nText(text));
        breadcrumbEntryConsumer.accept(entry);
        this.entries.add(entry);
        return this;
    }

    private static class BreadcrumbEntryImpl implements BreadcrumbEntry
    {
        private Function<String, I18nText> i18nTextResolver;

        private I18nText                   text;
        private String                     link;
        private String                     linkedId;
        private boolean                    active;

        public BreadcrumbEntryImpl(Function<String, I18nText> i18nTextResolver)
        {
            super();
            this.i18nTextResolver = i18nTextResolver;
        }

        @Override
        public BreadcrumbEntry withText(String text)
        {
            this.text = this.i18nTextResolver.apply(text);
            return this;
        }

        @Override
        public BreadcrumbEntry withLink(String link)
        {
            this.link = link;
            return this;
        }

        @Override
        public BreadcrumbEntry withLinkedLocator(String id)
        {
            this.linkedId = id;
            return this;
        }

        @Override
        public BreadcrumbEntry withLinked(UIComponent component)
        {
            return this.withLinkedLocator(component.getId());
        }

        @Override
        public BreadcrumbEntry withActiveState(boolean active)
        {
            this.active = active;
            return this;
        }

        public I18nText getText()
        {
            return this.text;
        }

        public String getLink()
        {
            return this.link;
        }

        public String getLinkedId()
        {
            return this.linkedId;
        }

        public boolean isActive()
        {
            return this.active;
        }

        @Override
        public String toString()
        {
            return "BreadcrumbEntryImpl [i18nTextResolver=" + this.i18nTextResolver + ", text=" + this.text + ", link=" + this.link + ", linkedId="
                   + this.linkedId + ", active=" + this.active + "]";
        }

    }

    @Override
    public UIComponentProvider<Breadcrumb> asTemplateProvider()
    {
        return () -> new BreadcrumbImpl(this.context, this.entries.stream()
                                                                  .collect(Collectors.toList()));
    }
}
