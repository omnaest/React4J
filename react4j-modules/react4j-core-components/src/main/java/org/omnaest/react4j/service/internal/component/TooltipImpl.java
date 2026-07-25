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
import org.omnaest.react4j.domain.Tooltip;
import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.context.data.Data;
import org.omnaest.react4j.domain.i18n.I18nText;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer;
import org.omnaest.react4j.domain.rendering.components.LocationSupport;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.domain.rendering.node.NodeRendererRegistry;
import org.omnaest.react4j.domain.support.UIComponentProvider;
import org.omnaest.react4j.service.internal.nodes.TooltipNode;
import org.omnaest.react4j.service.internal.service.LocalizedTextResolverService;

public class TooltipImpl extends AbstractUIComponentAndContentHolder<Tooltip> implements Tooltip
{
    private UIComponent<?> content;
    private I18nText       text;
    private Placement      placement;

    public TooltipImpl(ComponentContext context)
    {
        super(context);
    }

    public TooltipImpl(ComponentContext context, UIComponent<?> content, I18nText text, Placement placement)
    {
        super(context);
        this.content = content;
        this.text = text;
        this.placement = placement;
    }

    @Override
    public Tooltip withContent(UIComponent<?> component)
    {
        this.content = component;
        return this;
    }

    @Override
    public Tooltip withText(String text)
    {
        this.text = this.toI18nText(text);
        return this;
    }

    @Override
    public Tooltip withPlacement(Placement placement)
    {
        this.placement = placement;
        return this;
    }

    @Override
    public UIComponentRenderer asRenderer()
    {
        return new UIComponentRenderer() {
            @Override
            public Location getLocation(LocationSupport locationSupport)
            {
                return locationSupport.createLocation(TooltipImpl.this.getId());
            }

            @Override
            public Node render(RenderingProcessor renderingProcessor, Location location, Optional<Data> data)
            {
                LocalizedTextResolverService textResolver = TooltipImpl.this.getTextResolver();
                return new TooltipNode().setContent(renderingProcessor.process(TooltipImpl.this.content, location))
                                        .setText(TooltipImpl.this.text != null ? textResolver.apply(TooltipImpl.this.text, location) : null)
                                        .setPlacement(TooltipImpl.this.placement != null ? TooltipImpl.this.placement.name()
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
                return Stream.of(ParentLocationAndComponent.of(parentLocation, TooltipImpl.this.content));
            }

        };
    }

    @Override
    public UIComponentProvider<Tooltip> asTemplateProvider()
    {
        return () -> new TooltipImpl(this.context, this.content, this.text, this.placement);
    }
}
