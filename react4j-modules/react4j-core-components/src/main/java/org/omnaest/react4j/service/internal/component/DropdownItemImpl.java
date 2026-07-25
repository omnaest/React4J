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

import java.util.Optional;
import java.util.stream.Stream;

import org.omnaest.react4j.domain.Dropdown.DropdownItem;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.context.data.Data;
import org.omnaest.react4j.domain.i18n.I18nText;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer;
import org.omnaest.react4j.domain.rendering.components.HandlerEmitter;
import org.omnaest.react4j.domain.rendering.components.LocationSupport;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.domain.rendering.node.NodeRendererRegistry;
import org.omnaest.react4j.domain.support.UIComponentProvider;
import org.omnaest.react4j.service.internal.handler.domain.EventHandler;
import org.omnaest.react4j.service.internal.handler.domain.Target;
import org.omnaest.react4j.service.internal.nodes.DropdownItemNode;
import org.omnaest.react4j.service.internal.nodes.handler.Handler;
import org.omnaest.react4j.service.internal.nodes.handler.ServerHandler;
import org.omnaest.react4j.service.internal.service.LocalizedTextResolverService;

public class DropdownItemImpl extends AbstractUIComponent<DropdownItem> implements DropdownItem
{
    private final int    index;
    private final Kind   kind;

    private I18nText     text;
    private String       link;
    private boolean      active;
    private boolean      disabled;
    private EventHandler eventHandler;

    public DropdownItemImpl(ComponentContext context, int index, Kind kind)
    {
        super(context);
        this.index = index;
        this.kind = kind;
        this.withId("dropdownitem-" + index);
    }

    public DropdownItemImpl(ComponentContext context, int index, Kind kind, I18nText text, String link, boolean active, boolean disabled, EventHandler eventHandler)
    {
        super(context);
        this.index = index;
        this.kind = kind;
        this.withId("dropdownitem-" + index);
        this.text = text;
        this.link = link;
        this.active = active;
        this.disabled = disabled;
        this.eventHandler = eventHandler;
    }

    @Override
    public DropdownItem withText(String text)
    {
        this.text = this.toI18nText(text);
        return this;
    }

    @Override
    public DropdownItem withLink(String link)
    {
        this.link = link;
        return this;
    }

    @Override
    public DropdownItem withActiveState(boolean active)
    {
        this.active = active;
        return this;
    }

    @Override
    public DropdownItem withDisabledState(boolean disabled)
    {
        this.disabled = disabled;
        return this;
    }

    @Override
    public DropdownItem onClick(EventHandler eventHandler)
    {
        this.eventHandler = eventHandler;
        return this;
    }

    @Override
    public UIComponentRenderer asRenderer()
    {
        return new UIComponentRenderer() {
            @Override
            public Location getLocation(LocationSupport locationSupport)
            {
                return locationSupport.createLocation(DropdownItemImpl.this.getId());
            }

            @Override
            public Node render(RenderingProcessor renderingProcessor, Location location, Optional<Data> data)
            {
                LocalizedTextResolverService textResolver = DropdownItemImpl.this.getTextResolver();
                DropdownItemNode node = new DropdownItemNode().setKind(DropdownItemImpl.this.kind.name())
                                                              .setText(DropdownItemImpl.this.text != null ? textResolver.apply(DropdownItemImpl.this.text,
                                                                                                                               location)
                                                                      : null)
                                                              .setLink(DropdownItemImpl.this.link)
                                                              .setActive(DropdownItemImpl.this.active)
                                                              .setDisabled(DropdownItemImpl.this.disabled);
                if (DropdownItemImpl.this.eventHandler != null)
                {
                    node.setOnClick(DropdownItemImpl.this.emitOnClickHandler(renderingProcessor, Target.from(location)));
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
                if (DropdownItemImpl.this.eventHandler != null)
                {
                    eventHandlerRegistrationSupport.register(DropdownItemImpl.this.eventHandler);
                }
            }

            @Override
            public Stream<ParentLocationAndComponent> getSubComponents(Location parentLocation)
            {
                return Stream.empty();
            }

        };
    }

    @Override
    public UIComponentProvider<DropdownItem> asTemplateProvider()
    {
        return () -> new DropdownItemImpl(this.context, this.index, this.kind, this.text, this.link, this.active, this.disabled, this.eventHandler);
    }

    public static enum Kind
    {
        LINK, DIVIDER, HEADER
    }

    /**
     * plan-78 Cliff C1-A: obtains the {@code onClick} node-DTO {@link Handler} through the
     * {@link RenderingProcessor}'s {@link HandlerEmitter} instead of constructing
     * {@code new ServerHandler(target)} directly - the emitter registers the handler AND returns the node
     * DTO in one call. Null-tolerant: a raw Mockito {@code mock(RenderingProcessor.class)} returns
     * {@code null} for {@link RenderingProcessor#handlers()} (Mockito stubs default methods too, unlike a
     * hand-rolled anonymous subclass), so this falls back to the pre-Slice-2 behavior of building the
     * {@link ServerHandler} directly - keeping the existing {@code *ImplTest} suite green without requiring
     * every test to stub a real {@link HandlerEmitter}.
     *
     * @param renderingProcessor
     * @param target
     * @return
     */
    private Handler emitOnClickHandler(RenderingProcessor renderingProcessor, Target target)
    {
        HandlerEmitter handlerEmitter = renderingProcessor != null ? renderingProcessor.handlers() : null;
        return handlerEmitter != null ? handlerEmitter.emitEventHandler(target, this.eventHandler) : new ServerHandler(target);
    }
}
