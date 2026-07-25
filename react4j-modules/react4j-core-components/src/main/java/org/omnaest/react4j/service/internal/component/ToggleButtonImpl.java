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
import org.omnaest.react4j.domain.ToggleButton;
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
import org.omnaest.react4j.service.internal.nodes.ToggleButtonNode;
import org.omnaest.react4j.service.internal.nodes.handler.ServerHandler;
import org.omnaest.react4j.service.internal.service.LocalizedTextResolverService;

public class ToggleButtonImpl extends AbstractUIComponent<ToggleButton> implements ToggleButton
{
    private I18nText     text;
    private Style        style;
    private boolean      pressed;
    private EventHandler eventHandler;

    public ToggleButtonImpl(ComponentContext context)
    {
        super(context);
    }

    public ToggleButtonImpl(ComponentContext context, I18nText text, Style style, boolean pressed, EventHandler eventHandler)
    {
        super(context);
        this.text = text;
        this.style = style;
        this.pressed = pressed;
        this.eventHandler = eventHandler;
    }

    @Override
    public ToggleButton withText(String text)
    {
        this.text = this.toI18nText(text);
        return this;
    }

    @Override
    public ToggleButton withStyle(Style style)
    {
        this.style = style;
        return this;
    }

    @Override
    public ToggleButton withPressed(boolean pressed)
    {
        this.pressed = pressed;
        return this;
    }

    @Override
    public ToggleButton onChange(EventHandler eventHandler)
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
                return locationSupport.createLocation(ToggleButtonImpl.this.getId());
            }

            @Override
            public Node render(RenderingProcessor renderingProcessor, Location location, Optional<Data> data)
            {
                LocalizedTextResolverService textResolver = ToggleButtonImpl.this.getTextResolver();
                ToggleButtonNode node = new ToggleButtonNode().setText(ToggleButtonImpl.this.text != null ? textResolver.apply(ToggleButtonImpl.this.text,
                                                                                                                               location)
                        : null)
                                                              .setStyle(ToggleButtonImpl.this.style != null ? ToggleButtonImpl.this.style.name()
                                                                                                                                         .toLowerCase()
                                                                      : null)
                                                              .setPressed(ToggleButtonImpl.this.pressed);
                if (ToggleButtonImpl.this.eventHandler != null)
                {
                    node.setOnChange(new ServerHandler(Target.from(location)));
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
                if (ToggleButtonImpl.this.eventHandler != null)
                {
                    eventHandlerRegistrationSupport.register(ToggleButtonImpl.this.eventHandler);
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
    public UIComponentProvider<ToggleButton> asTemplateProvider()
    {
        return () -> new ToggleButtonImpl(this.context, this.text, this.style, this.pressed, this.eventHandler);
    }
}
