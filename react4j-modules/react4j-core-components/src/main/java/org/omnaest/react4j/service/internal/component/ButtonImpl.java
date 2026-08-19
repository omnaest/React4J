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

import org.omnaest.react4j.domain.Button;
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
import org.omnaest.react4j.service.internal.nodes.ButtonNode;
import org.omnaest.react4j.service.internal.nodes.handler.Handler;
import org.omnaest.react4j.service.internal.nodes.handler.ServerHandler;

public class ButtonImpl extends AbstractUIComponent<Button> implements Button
{
    private I18nText     name;
    private Style        style = Style.PRIMARY;
    private EventHandler eventHandler;
    private String       ariaLabel;

    public ButtonImpl(ComponentContext context)
    {
        super(context);
    }

    public ButtonImpl(ComponentContext context, I18nText name, Style style, EventHandler eventHandler)
    {
        super(context);
        this.name = name;
        this.style = style;
        this.eventHandler = eventHandler;
    }

    @Override
    public Button withName(String name)
    {
        this.name = this.toI18nText(name);
        return this;
    }

    @Override
    public Button withAriaLabel(String ariaLabel)
    {
        this.ariaLabel = ariaLabel;
        return this;
    }

    @Override
    public Button withStyle(Style style)
    {
        this.style = style;
        return this;
    }

    @Override
    public UIComponentRenderer asRenderer()
    {
        return new UIComponentRenderer() {
            @Override
            public Location getLocation(LocationSupport locationSupport)
            {
                return locationSupport.createLocation(ButtonImpl.this.getId());
            }

            @Override
            public Node render(RenderingProcessor renderingProcessor, Location location, Optional<Data> data)
            {
                ButtonNode node = new ButtonNode().setName(ButtonImpl.this.getTextResolver()
                                                                          .apply(ButtonImpl.this.name, location))
                                                  .setAriaLabel(ButtonImpl.this.ariaLabel)
                                                  .setStyle(ButtonImpl.this.style.name()
                                                                                 .toLowerCase());
                if (ButtonImpl.this.eventHandler != null)
                {
                    node.setOnClick(ButtonImpl.this.emitOnClickHandler(renderingProcessor, Target.from(location)));
                }
                return node;
            }

            @Override
            public void manageEventHandler(EventHandlerRegistrationSupport eventHandlerRegistrationSupport)
            {
                if (ButtonImpl.this.eventHandler != null)
                {
                    eventHandlerRegistrationSupport.register(ButtonImpl.this.eventHandler);
                }
            }

            @Override
            public void manageNodeRenderers(NodeRendererRegistry registry)
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
    public Button onClick(EventHandler eventHandler)
    {
        this.eventHandler = eventHandler;
        return this;
    }

    @Override
    public UIComponentProvider<Button> asTemplateProvider()
    {
        return () -> new ButtonImpl(this.context, this.name, this.style, this.eventHandler);
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
