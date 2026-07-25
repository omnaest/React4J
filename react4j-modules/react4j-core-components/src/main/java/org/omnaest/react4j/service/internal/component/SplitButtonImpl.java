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

import org.omnaest.react4j.domain.Dropdown.DropdownItem;
import org.omnaest.react4j.domain.Dropdown.Style;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.SplitButton;
import org.omnaest.react4j.domain.context.data.Data;
import org.omnaest.react4j.domain.i18n.I18nText;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer;
import org.omnaest.react4j.domain.rendering.components.LocationSupport;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.domain.rendering.node.NodeRendererRegistry;
import org.omnaest.react4j.domain.support.UIComponentProvider;
import org.omnaest.react4j.service.internal.handler.domain.EventHandler;
import org.omnaest.react4j.service.internal.handler.domain.Target;
import org.omnaest.react4j.service.internal.nodes.DropdownItemNode;
import org.omnaest.react4j.service.internal.nodes.SplitButtonNode;
import org.omnaest.react4j.service.internal.nodes.handler.ServerHandler;
import org.omnaest.react4j.service.internal.service.LocalizedTextResolverService;

public class SplitButtonImpl extends AbstractUIComponentWithSubComponents<SplitButton> implements SplitButton
{
    private I18nText               text;
    private Style                  style;
    private EventHandler           eventHandler;
    private List<DropdownItemImpl> items = new ArrayList<>();

    public SplitButtonImpl(ComponentContext context)
    {
        super(context);
    }

    public SplitButtonImpl(ComponentContext context, I18nText text, Style style, EventHandler eventHandler, List<DropdownItemImpl> items)
    {
        super(context);
        this.text = text;
        this.style = style;
        this.eventHandler = eventHandler;
        this.items = items;
    }

    @Override
    public SplitButton withText(String text)
    {
        this.text = this.toI18nText(text);
        return this;
    }

    @Override
    public SplitButton withStyle(Style style)
    {
        this.style = style;
        return this;
    }

    @Override
    public SplitButton onClick(EventHandler eventHandler)
    {
        this.eventHandler = eventHandler;
        return this;
    }

    @Override
    public SplitButton addItem(Consumer<DropdownItem> dropdownItemConsumer)
    {
        DropdownItemImpl item = new DropdownItemImpl(this.context, this.items.size(), DropdownItemImpl.Kind.LINK);
        dropdownItemConsumer.accept(item);
        this.items.add(item);
        return this;
    }

    @Override
    public SplitButton addDivider()
    {
        this.items.add(new DropdownItemImpl(this.context, this.items.size(), DropdownItemImpl.Kind.DIVIDER));
        return this;
    }

    @Override
    public SplitButton addHeader(String text)
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
                return locationSupport.createLocation(SplitButtonImpl.this.getId());
            }

            @Override
            public Node render(RenderingProcessor renderingProcessor, Location location, Optional<Data> data)
            {
                LocalizedTextResolverService textResolver = SplitButtonImpl.this.getTextResolver();
                SplitButtonNode node = new SplitButtonNode().setTitle(SplitButtonImpl.this.text != null ? textResolver.apply(SplitButtonImpl.this.text,
                                                                                                                             location)
                        : null)
                                                            .setStyle(SplitButtonImpl.this.style != null ? SplitButtonImpl.this.style.name()
                                                                                                                                     .toLowerCase()
                                                                    : null)
                                                            .setItems(SplitButtonImpl.this.items.stream()
                                                                                                .map(item -> (DropdownItemNode) renderingProcessor.process(item,
                                                                                                                                                           location))
                                                                                                .collect(Collectors.toList()));
                if (SplitButtonImpl.this.eventHandler != null)
                {
                    node.setOnClick(new ServerHandler(Target.from(location)));
                }
                return node;
            }

            @Override
            public void manageNodeRenderers(NodeRendererRegistry registry)
            {
            }

            @Override
            public void manageEventHandler(EventHandlerRegistrationSupport eventHandlerRegistrationSupport)
            {
                if (SplitButtonImpl.this.eventHandler != null)
                {
                    eventHandlerRegistrationSupport.register(SplitButtonImpl.this.eventHandler);
                }
            }

            @Override
            public Stream<ParentLocationAndComponent> getSubComponents(Location parentLocation)
            {
                return SplitButtonImpl.this.items.stream()
                                                 .map(item -> ParentLocationAndComponent.of(parentLocation, item));
            }

        };
    }

    @Override
    public UIComponentProvider<SplitButton> asTemplateProvider()
    {
        return () -> new SplitButtonImpl(this.context, this.text, this.style, this.eventHandler, this.items.stream()
                                                                                                           .collect(Collectors.toList()));
    }
}
