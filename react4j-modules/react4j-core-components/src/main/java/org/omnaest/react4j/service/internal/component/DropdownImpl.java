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

import org.omnaest.react4j.domain.Dropdown;
import org.omnaest.react4j.domain.Dropdown.Drop;
import org.omnaest.react4j.domain.Dropdown.DropdownItem;
import org.omnaest.react4j.domain.Dropdown.Presentation;
import org.omnaest.react4j.domain.Dropdown.Style;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.context.data.Data;
import org.omnaest.react4j.domain.i18n.I18nText;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer;
import org.omnaest.react4j.domain.rendering.components.LocationSupport;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.domain.rendering.node.NodeRendererRegistry;
import org.omnaest.react4j.domain.support.UIComponentProvider;
import org.omnaest.react4j.service.internal.nodes.DropdownItemNode;
import org.omnaest.react4j.service.internal.nodes.DropdownNode;
import org.omnaest.react4j.service.internal.service.LocalizedTextResolverService;

public class DropdownImpl extends AbstractUIComponentWithSubComponents<Dropdown> implements Dropdown
{
    private I18nText               title;
    private Style                  style;
    private Presentation           presentation = Presentation.BUTTON;
    private Drop                   drop;
    private List<DropdownItemImpl> items        = new ArrayList<>();

    public DropdownImpl(ComponentContext context)
    {
        super(context);
    }

    public DropdownImpl(ComponentContext context, Presentation presentation, I18nText title, Style style, Drop drop, List<DropdownItemImpl> items)
    {
        super(context);
        this.presentation = presentation;
        this.title = title;
        this.style = style;
        this.drop = drop;
        this.items = items;
    }

    @Override
    public Dropdown withTitle(String title)
    {
        this.title = this.toI18nText(title);
        return this;
    }

    @Override
    public Dropdown withStyle(Style style)
    {
        this.style = style;
        return this;
    }

    @Override
    public Dropdown withPresentation(Presentation presentation)
    {
        this.presentation = presentation;
        return this;
    }

    @Override
    public Dropdown withDrop(Drop drop)
    {
        this.drop = drop;
        return this;
    }

    @Override
    public Dropdown addItem(Consumer<DropdownItem> dropdownItemConsumer)
    {
        DropdownItemImpl item = new DropdownItemImpl(this.context, this.items.size(), DropdownItemImpl.Kind.LINK);
        dropdownItemConsumer.accept(item);
        this.items.add(item);
        return this;
    }

    @Override
    public Dropdown addDivider()
    {
        this.items.add(new DropdownItemImpl(this.context, this.items.size(), DropdownItemImpl.Kind.DIVIDER));
        return this;
    }

    @Override
    public Dropdown addHeader(String text)
    {
        DropdownItemImpl item = new DropdownItemImpl(this.context, this.items.size(), DropdownItemImpl.Kind.HEADER);
        item.withText(text);
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
                return locationSupport.createLocation(DropdownImpl.this.getId());
            }

            @Override
            public Node render(RenderingProcessor renderingProcessor, Location location, Optional<Data> data)
            {
                LocalizedTextResolverService textResolver = DropdownImpl.this.getTextResolver();
                return new DropdownNode().setPresentation(DropdownImpl.this.presentation.name())
                                         .setTitle(DropdownImpl.this.title != null ? textResolver.apply(DropdownImpl.this.title, location) : null)
                                         .setStyle(DropdownImpl.this.style != null ? DropdownImpl.this.style.name()
                                                                                                            .toLowerCase()
                                                 : null)
                                         .setDrop(DropdownImpl.this.drop != null ? DropdownImpl.this.drop.name()
                                                                                                         .toLowerCase()
                                                 : null)
                                         .setItems(DropdownImpl.this.items.stream()
                                                                          .map(item -> (DropdownItemNode) renderingProcessor.process(item, location))
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
            public Stream<ParentLocationAndComponent> getSubComponents(Location parentLocation)
            {
                return DropdownImpl.this.items.stream()
                                              .map(item -> ParentLocationAndComponent.of(parentLocation, item));
            }

        };
    }

    @Override
    public UIComponentProvider<Dropdown> asTemplateProvider()
    {
        return () -> new DropdownImpl(this.context, this.presentation, this.title, this.style, this.drop, this.items.stream()
                                                                                                                    .collect(Collectors.toList()));
    }
}
