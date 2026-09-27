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

import org.omnaest.react4j.domain.Breadcrumb.BreadcrumbEntry;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.UIComponent;
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
import org.omnaest.react4j.service.internal.nodes.BreadcrumbEntryNode;
import org.omnaest.react4j.service.internal.nodes.handler.Handler;
import org.omnaest.react4j.service.internal.nodes.handler.ServerHandler;

/**
 * @see BreadcrumbImpl
 * @see PaginationItemImpl
 * @author omnaest
 */
public class BreadcrumbEntryImpl extends AbstractUIComponent<BreadcrumbEntry> implements BreadcrumbEntry
{
    private final int    index;

    private I18nText     text;
    private String       link;
    private String       linkedId;
    private boolean      active;
    private EventHandler eventHandler;

    public BreadcrumbEntryImpl(ComponentContext context, int index)
    {
        super(context);
        this.index = index;
        this.withId("breadcrumbentry-" + index);
    }

    public BreadcrumbEntryImpl(ComponentContext context, int index, I18nText text, String link, String linkedId, boolean active, EventHandler eventHandler)
    {
        super(context);
        this.index = index;
        this.withId("breadcrumbentry-" + index);
        this.text = text;
        this.link = link;
        this.linkedId = linkedId;
        this.active = active;
        this.eventHandler = eventHandler;
    }

    @Override
    public BreadcrumbEntry withText(String text)
    {
        this.text = this.toI18nText(text);
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

    @Override
    public BreadcrumbEntry onClick(EventHandler eventHandler)
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
                return locationSupport.createLocation(BreadcrumbEntryImpl.this.getId());
            }

            @Override
            public Node render(RenderingProcessor renderingProcessor, Location location, Optional<Data> data)
            {
                BreadcrumbEntryNode node = new BreadcrumbEntryNode().setText(BreadcrumbEntryImpl.this.getTextResolver()
                                                                                                     .apply(BreadcrumbEntryImpl.this.text, location))
                                                                    .setLink(BreadcrumbEntryImpl.this.link)
                                                                    .setLinkedId(BreadcrumbEntryImpl.this.linkedId)
                                                                    .setActive(BreadcrumbEntryImpl.this.active);
                if (BreadcrumbEntryImpl.this.eventHandler != null)
                {
                    node.setOnClick(BreadcrumbEntryImpl.this.emitOnClickHandler(renderingProcessor, Target.from(location)));
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
                if (BreadcrumbEntryImpl.this.eventHandler != null)
                {
                    eventHandlerRegistrationSupport.register(BreadcrumbEntryImpl.this.eventHandler);
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
    public UIComponentProvider<BreadcrumbEntry> asTemplateProvider()
    {
        return () -> new BreadcrumbEntryImpl(this.context, this.index, this.text, this.link, this.linkedId, this.active, this.eventHandler);
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
     * <p>
     * Copied verbatim from {@link PaginationItemImpl#emitOnClickHandler(RenderingProcessor, Target)}
     * (plan-262 S1).
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
