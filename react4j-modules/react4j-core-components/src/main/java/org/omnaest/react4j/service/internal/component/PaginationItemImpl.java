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
import org.omnaest.react4j.domain.Pagination.PaginationItem;
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
import org.omnaest.react4j.service.internal.nodes.PaginationItemNode;
import org.omnaest.react4j.service.internal.nodes.handler.ServerHandler;

public class PaginationItemImpl extends AbstractUIComponent<PaginationItem> implements PaginationItem
{
    private final int    index;

    private I18nText     label;
    private boolean      active;
    private boolean      disabled;
    private EventHandler eventHandler;

    public PaginationItemImpl(ComponentContext context, int index)
    {
        super(context);
        this.index = index;
        this.withId("paginationitem-" + index);
    }

    public PaginationItemImpl(ComponentContext context, int index, I18nText label, boolean active, boolean disabled, EventHandler eventHandler)
    {
        super(context);
        this.index = index;
        this.withId("paginationitem-" + index);
        this.label = label;
        this.active = active;
        this.disabled = disabled;
        this.eventHandler = eventHandler;
    }

    @Override
    public PaginationItem withLabel(String label)
    {
        this.label = this.toI18nText(label);
        return this;
    }

    @Override
    public PaginationItem withActiveState(boolean active)
    {
        this.active = active;
        return this;
    }

    @Override
    public PaginationItem withDisabledState(boolean disabled)
    {
        this.disabled = disabled;
        return this;
    }

    @Override
    public PaginationItem onClick(EventHandler eventHandler)
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
                return locationSupport.createLocation(PaginationItemImpl.this.getId());
            }

            @Override
            public Node render(RenderingProcessor renderingProcessor, Location location, Optional<Data> data)
            {
                PaginationItemNode node = new PaginationItemNode().setLabel(PaginationItemImpl.this.getTextResolver()
                                                                                                   .apply(PaginationItemImpl.this.label, location))
                                                                  .setActive(PaginationItemImpl.this.active)
                                                                  .setDisabled(PaginationItemImpl.this.disabled);
                if (PaginationItemImpl.this.eventHandler != null)
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
                if (PaginationItemImpl.this.eventHandler != null)
                {
                    eventHandlerRegistrationSupport.register(PaginationItemImpl.this.eventHandler);
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
    public UIComponentProvider<PaginationItem> asTemplateProvider()
    {
        return () -> new PaginationItemImpl(this.context, this.index, this.label, this.active, this.disabled, this.eventHandler);
    }
}
