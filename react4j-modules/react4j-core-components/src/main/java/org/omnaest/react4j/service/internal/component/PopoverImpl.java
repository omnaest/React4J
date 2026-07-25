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

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.Popover;
import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.context.data.Data;
import org.omnaest.react4j.domain.i18n.I18nText;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer;
import org.omnaest.react4j.domain.rendering.components.LocationSupport;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.domain.rendering.node.NodeRendererRegistry;
import org.omnaest.react4j.domain.support.UIComponentProvider;
import org.omnaest.react4j.service.internal.nodes.PopoverNode;
import org.omnaest.react4j.service.internal.service.LocalizedTextResolverService;

public class PopoverImpl extends AbstractUIComponentAndContentHolder<Popover> implements Popover
{
    private UIComponent<?> content;
    private UIComponent<?> body;
    private I18nText       title;
    private Placement      placement;
    private Trigger        trigger;

    public PopoverImpl(ComponentContext context)
    {
        super(context);
    }

    public PopoverImpl(ComponentContext context, UIComponent<?> content, UIComponent<?> body, I18nText title, Placement placement, Trigger trigger)
    {
        super(context);
        this.content = content;
        this.body = body;
        this.title = title;
        this.placement = placement;
        this.trigger = trigger;
    }

    @Override
    public Popover withContent(UIComponent<?> component)
    {
        this.content = component;
        return this;
    }

    @Override
    public Popover withTitle(String title)
    {
        this.title = this.toI18nText(title);
        return this;
    }

    @Override
    public Popover withBody(UIComponent<?> body)
    {
        this.body = body;
        return this;
    }

    @Override
    public Popover withPlacement(Placement placement)
    {
        this.placement = placement;
        return this;
    }

    @Override
    public Popover withTrigger(Trigger trigger)
    {
        this.trigger = trigger;
        return this;
    }

    @Override
    public UIComponentRenderer asRenderer()
    {
        return new UIComponentRenderer() {
            @Override
            public Location getLocation(LocationSupport locationSupport)
            {
                return locationSupport.createLocation(PopoverImpl.this.getId());
            }

            @Override
            public Node render(RenderingProcessor renderingProcessor, Location location, Optional<Data> data)
            {
                LocalizedTextResolverService textResolver = PopoverImpl.this.getTextResolver();
                return new PopoverNode().setContent(renderingProcessor.process(PopoverImpl.this.content, location))
                                        .setBody(Optional.ofNullable(PopoverImpl.this.body)
                                                         .map(body -> renderingProcessor.process(body, location))
                                                         .orElse(null))
                                        .setTitle(PopoverImpl.this.title != null ? textResolver.apply(PopoverImpl.this.title, location) : null)
                                        .setPlacement(PopoverImpl.this.placement != null ? PopoverImpl.this.placement.name()
                                                                                                                     .toLowerCase()
                                                : null)
                                        .setTrigger(PopoverImpl.this.trigger != null ? PopoverImpl.this.trigger.name()
                                                                                                               .toLowerCase()
                                                : null);
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
                return Stream.of(PopoverImpl.this.content, PopoverImpl.this.body)
                             .filter(Objects::nonNull)
                             .map(component -> ParentLocationAndComponent.of(parentLocation, component));
            }

        };
    }

    @Override
    public UIComponentProvider<Popover> asTemplateProvider()
    {
        return () -> new PopoverImpl(this.context, this.content, this.body, this.title, this.placement, this.trigger);
    }
}
