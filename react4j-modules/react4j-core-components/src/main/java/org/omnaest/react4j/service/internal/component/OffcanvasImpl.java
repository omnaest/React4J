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

import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.Offcanvas;
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
import org.omnaest.react4j.service.internal.nodes.OffcanvasNode;
import org.omnaest.react4j.service.internal.nodes.handler.Handler;
import org.omnaest.react4j.service.internal.nodes.handler.ServerHandler;
import org.omnaest.react4j.service.internal.service.LocalizedTextResolverService;

public class OffcanvasImpl extends AbstractUIComponentAndContentHolder<Offcanvas> implements Offcanvas
{
    private I18nText       title;
    private UIComponent<?> content;
    private boolean        visible;
    private Placement      placement;
    private EventHandler   eventHandler;

    public OffcanvasImpl(ComponentContext context)
    {
        super(context);
    }

    public OffcanvasImpl(ComponentContext context, I18nText title, UIComponent<?> content, boolean visible, Placement placement, EventHandler eventHandler)
    {
        super(context);
        this.title = title;
        this.content = content;
        this.visible = visible;
        this.placement = placement;
        this.eventHandler = eventHandler;
    }

    @Override
    public Offcanvas withTitle(String title)
    {
        this.title = this.toI18nText(title);
        return this;
    }

    @Override
    public Offcanvas withContent(UIComponent<?> component)
    {
        this.content = component;
        return this;
    }

    @Override
    public Offcanvas withVisible(boolean visible)
    {
        this.visible = visible;
        return this;
    }

    @Override
    public Offcanvas withPlacement(Placement placement)
    {
        this.placement = placement;
        return this;
    }

    @Override
    public Offcanvas onClose(EventHandler eventHandler)
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
                return locationSupport.createLocation(OffcanvasImpl.this.getId());
            }

            @Override
            public Node render(RenderingProcessor renderingProcessor, Location location, Optional<Data> data)
            {
                LocalizedTextResolverService textResolver = OffcanvasImpl.this.getTextResolver();
                OffcanvasNode node = new OffcanvasNode().setTitle(OffcanvasImpl.this.title != null ? textResolver.apply(OffcanvasImpl.this.title, location)
                        : null)
                                                        .setContent(renderingProcessor.process(OffcanvasImpl.this.content, location))
                                                        .setVisible(OffcanvasImpl.this.visible)
                                                        .setPlacement(OffcanvasImpl.this.placement != null ? OffcanvasImpl.this.placement.name()
                                                                                                                                         .toLowerCase()
                                                                : null);
                if (OffcanvasImpl.this.eventHandler != null)
                {
                    node.setOnClose(OffcanvasImpl.this.emitOnCloseHandler(renderingProcessor, Target.from(location)));
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
                if (OffcanvasImpl.this.eventHandler != null)
                {
                    eventHandlerRegistrationSupport.register(OffcanvasImpl.this.eventHandler);
                }
            }

            @Override
            public Stream<ParentLocationAndComponent> getSubComponents(Location parentLocation)
            {
                return Stream.of(ParentLocationAndComponent.of(parentLocation, OffcanvasImpl.this.content));
            }

        };
    }

    @Override
    public UIComponentProvider<Offcanvas> asTemplateProvider()
    {
        return () -> new OffcanvasImpl(this.context, this.title, this.content, this.visible, this.placement, this.eventHandler);
    }

    /**
     * plan-78 Cliff C1-A: obtains the {@code onClose} node-DTO {@link Handler} through the
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
    private Handler emitOnCloseHandler(RenderingProcessor renderingProcessor, Target target)
    {
        HandlerEmitter handlerEmitter = renderingProcessor != null ? renderingProcessor.handlers() : null;
        return handlerEmitter != null ? handlerEmitter.emitEventHandler(target, this.eventHandler) : new ServerHandler(target);
    }
}
