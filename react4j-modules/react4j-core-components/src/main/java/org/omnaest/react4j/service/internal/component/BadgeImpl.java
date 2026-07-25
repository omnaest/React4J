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

import org.omnaest.react4j.domain.Badge;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.context.data.Data;
import org.omnaest.react4j.domain.i18n.I18nText;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer;
import org.omnaest.react4j.domain.rendering.components.LocationSupport;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.domain.rendering.node.NodeRenderType;
import org.omnaest.react4j.domain.rendering.node.NodeRendererRegistry;
import org.omnaest.react4j.domain.support.UIComponentProvider;
import org.omnaest.react4j.service.internal.nodes.BadgeNode;

public class BadgeImpl extends AbstractUIComponent<Badge> implements Badge
{
    private I18nText text;
    private Style    style = Style.PRIMARY;

    public BadgeImpl(ComponentContext context)
    {
        super(context);
    }

    public BadgeImpl(ComponentContext context, I18nText text, Style style)
    {
        super(context);
        this.text = text;
        this.style = style;
    }

    @Override
    public Badge withText(String text)
    {
        this.text = this.toI18nText(text);
        return this;
    }

    @Override
    public Badge withStyle(Style style)
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
                return locationSupport.createLocation(BadgeImpl.this.getId());
            }

            @Override
            public Node render(RenderingProcessor renderingProcessor, Location location, Optional<Data> data)
            {
                return new BadgeNode().setText(BadgeImpl.this.getTextResolver()
                                                             .apply(BadgeImpl.this.text, location))
                                      .setStyle(BadgeImpl.this.style.name()
                                                                    .toLowerCase());
            }

            @Override
            public void manageNodeRenderers(NodeRendererRegistry registry)
            {
                registry.register(BadgeNode.class, NodeRenderType.HTML,
                                  (node, nodeRenderingProcessor) -> "<span class=\"badge bg-" + node.getStyle() + "\">"
                                                                    + nodeRenderingProcessor.render(node.getText()) + "</span>");
            }

            @Override
            public void manageEventHandler(EventHandlerRegistrationSupport eventHandlerRegistrationSupport)
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
    public UIComponentProvider<Badge> asTemplateProvider()
    {
        return () -> new BadgeImpl(this.context, this.text, this.style);
    }
}
