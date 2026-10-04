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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.NavigationBar;
import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.context.data.Data;
import org.omnaest.react4j.domain.i18n.I18nText;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer;
import org.omnaest.react4j.domain.rendering.components.LocationSupport;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.domain.rendering.node.NodeRenderType;
import org.omnaest.react4j.domain.rendering.node.NodeRenderer;
import org.omnaest.react4j.domain.rendering.node.NodeRendererRegistry;
import org.omnaest.react4j.domain.rendering.node.NodeRenderingProcessor;
import org.omnaest.react4j.domain.support.UIComponentProvider;
import org.omnaest.react4j.service.internal.nodes.NavigationBarNode;
import org.omnaest.utils.template.TemplateUtils;

public class NavigationBarImpl extends AbstractUIComponent<NavigationBar> implements NavigationBar
{
    private List<NavigationEntryImpl> entries = new ArrayList<>();

    public NavigationBarImpl(ComponentContext context)
    {
        super(context);
    }

    public NavigationBarImpl(ComponentContext context, List<NavigationEntryImpl> entries)
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
                return locationSupport.createLocation(NavigationBarImpl.this.getId());
            }

            @Override
            public Node render(RenderingProcessor renderingProcessor, Location location, Optional<Data> data)
            {
                return new NavigationBarNode().setEntries(NavigationBarImpl.this.entries.stream()
                                                                                        .map(entry -> NavigationBarImpl.this.toNodeEntry(entry, location))
                                                                                        .collect(Collectors.toList()));
            }

            @Override
            public void manageNodeRenderers(NodeRendererRegistry registry)
            {
                registry.register(NavigationBarNode.class, NodeRenderType.HTML, new NodeRenderer<NavigationBarNode>() {
                    @Override
                    public String render(NavigationBarNode node, NodeRenderingProcessor nodeRenderingProcessor)
                    {
                        return TemplateUtils.builder()
                                            .useTemplateClassResource(this.getClass(), "/render/templates/html/navigationbar.html")
                                            .add("entries", node.getEntries()
                                                                .stream()
                                                                .map(entry -> this.toTemplateModel(entry, nodeRenderingProcessor))
                                                                .collect(Collectors.toList()))
                                            .build()
                                            .get();
                    }

                    private Map<String, Object> toTemplateModel(NavigationBarNode.Entry entry, NodeRenderingProcessor nodeRenderingProcessor)
                    {
                        Map<String, Object> model = new HashMap<>();
                        model.put("text", nodeRenderingProcessor.render(entry.getText()));
                        if (entry.getDropdownEntries() != null)
                        {
                            // the toggle of a dropdown links nowhere, only its items carry a href
                            model.put("dropdown", true);
                            model.put("items", entry.getDropdownEntries()
                                                    .stream()
                                                    .map(item -> this.toTemplateModel(item, nodeRenderingProcessor))
                                                    .collect(Collectors.toList()));
                        }
                        else
                        {
                            model.put("dropdown", false);
                            model.put("link", Optional.ofNullable(entry.getLinkedId())
                                                      .map(linkedId -> "#" + linkedId)
                                                      .orElse(entry.getLink()));
                        }
                        return model;
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
                return Stream.empty();
            }

        };
    }

    private NavigationBarNode.Entry toNodeEntry(NavigationEntryImpl entry, Location location)
    {
        return new NavigationBarNode.Entry().setActive(entry.isActive())
                                            .setDisabled(entry.isDisabled())
                                            .setLink(entry.getLink())
                                            .setLinkedId(entry.getLinkedId())
                                            .setText(this.getTextResolver()
                                                         .apply(entry.getText(), location))
                                            .setDropdownEntries(entry.getDropdownEntries() != null ? entry.getDropdownEntries()
                                                                                                          .stream()
                                                                                                          .map(item -> this.toNodeEntry(item, location))
                                                                                                          .collect(Collectors.toList())
                                                    : null);
    }

    @Override
    public NavigationBar addEntry(Consumer<NavigationBarEntry> navigationBarEntryConsumer)
    {
        NavigationEntryImpl entry = new NavigationEntryImpl(text -> this.toI18nText(text));
        navigationBarEntryConsumer.accept(entry);
        this.entries.add(entry);
        return this;
    }

    @Override
    public NavigationBar addDropdown(Consumer<NavigationBarDropdown> dropdownConsumer)
    {
        NavigationEntryImpl toggle = new NavigationEntryImpl(text -> this.toI18nText(text)).asDropdown();
        dropdownConsumer.accept(new NavigationDropdownImpl(toggle));
        this.entries.add(toggle);
        return this;
    }

    /**
     * The API view of one dropdown. The dropdown itself is a {@link NavigationEntryImpl} with a list of items, so that entries and dropdowns share one list
     * in insertion order and the items are the very same implementation as the entries of the bar.
     */
    private static class NavigationDropdownImpl implements NavigationBarDropdown
    {
        private final NavigationEntryImpl toggle;

        public NavigationDropdownImpl(NavigationEntryImpl toggle)
        {
            super();
            this.toggle = toggle;
        }

        @Override
        public NavigationBarDropdown withText(String text)
        {
            this.toggle.withText(text);
            return this;
        }

        @Override
        public NavigationBarDropdown withActiveState(boolean active)
        {
            this.toggle.withActiveState(active);
            return this;
        }

        @Override
        public NavigationBarDropdown withDisabledState(boolean disabled)
        {
            this.toggle.withDisabledState(disabled);
            return this;
        }

        @Override
        public NavigationBarDropdown addEntry(Consumer<NavigationBarEntry> navigationEntryConsumer)
        {
            NavigationEntryImpl item = this.toggle.newItem();
            navigationEntryConsumer.accept(item);
            this.toggle.getDropdownEntries()
                       .add(item);
            return this;
        }
    }

    private static class NavigationEntryImpl implements NavigationBarEntry
    {
        private Function<String, I18nText> i18nTextResolver;

        private I18nText                   text;
        private String                     link;
        private String                     linkedId;
        private boolean                    active;

        private boolean                    disabled;

        /** {@code null} for a plain entry, the items of the menu for a dropdown */
        private List<NavigationEntryImpl>  dropdownEntries;

        public NavigationEntryImpl(Function<String, I18nText> i18nTextResolver)
        {
            super();
            this.i18nTextResolver = i18nTextResolver;
        }

        public NavigationEntryImpl asDropdown()
        {
            this.dropdownEntries = new ArrayList<>();
            return this;
        }

        public NavigationEntryImpl newItem()
        {
            return new NavigationEntryImpl(this.i18nTextResolver);
        }

        public List<NavigationEntryImpl> getDropdownEntries()
        {
            return this.dropdownEntries;
        }

        @Override
        public NavigationBarEntry withText(String text)
        {
            this.text = this.i18nTextResolver.apply(text);
            return this;
        }

        @Override
        public NavigationBarEntry withLink(String link)
        {
            this.link = link;
            return this;
        }

        @Override
        public NavigationBarEntry withLinkedLocator(String id)
        {
            this.linkedId = id;
            return this;
        }

        @Override
        public NavigationBarEntry withLinked(UIComponent component)
        {
            return this.withLinkedLocator(component.getId());
        }

        @Override
        public NavigationBarEntry withActiveState(boolean active)
        {
            this.active = active;
            return this;
        }

        @Override
        public NavigationBarEntry withDisabledState(boolean disabled)
        {
            this.disabled = disabled;
            return this;
        }

        public boolean isActive()
        {
            return this.active;
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

        public boolean isDisabled()
        {
            return this.disabled;
        }

        @Override
        public String toString()
        {
            return "NavigationEntryImpl [i18nTextResolver=" + this.i18nTextResolver + ", text=" + this.text + ", link=" + this.link + ", linkedId="
                   + this.linkedId + ", active=" + this.active + ", disabled=" + this.disabled + "]";
        }

    }

    @Override
    public UIComponentProvider<NavigationBar> asTemplateProvider()
    {
        return () -> new NavigationBarImpl(this.context, this.entries.stream()
                                                                     .collect(Collectors.toList()));
    }
}
