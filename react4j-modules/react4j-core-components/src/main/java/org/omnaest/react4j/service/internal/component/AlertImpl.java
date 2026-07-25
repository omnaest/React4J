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

import org.omnaest.react4j.domain.Alert;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.context.data.Data;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer;
import org.omnaest.react4j.domain.rendering.components.LocationSupport;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.domain.rendering.node.NodeRenderType;
import org.omnaest.react4j.domain.rendering.node.NodeRendererRegistry;
import org.omnaest.react4j.domain.support.UIComponentProvider;
import org.omnaest.react4j.service.internal.nodes.AlertNode;

public class AlertImpl extends AbstractUIComponentAndContentHolder<Alert> implements Alert
{
    private UIComponent<?> content;
    private Style          style = Style.PRIMARY;
    private boolean        dismissible;

    public AlertImpl(ComponentContext context)
    {
        super(context);
    }

    public AlertImpl(ComponentContext context, UIComponent<?> content, Style style, boolean dismissible)
    {
        super(context);
        this.content = content;
        this.style = style;
        this.dismissible = dismissible;
    }

    @Override
    public Alert withContent(UIComponent<?> component)
    {
        this.content = component;
        return this;
    }

    @Override
    public Alert withStyle(Style style)
    {
        this.style = style;
        return this;
    }

    @Override
    public Alert withDismissible(boolean dismissible)
    {
        this.dismissible = dismissible;
        return this;
    }

    @Override
    public UIComponentRenderer asRenderer()
    {
        return new UIComponentRenderer() {
            @Override
            public Location getLocation(LocationSupport locationSupport)
            {
                return locationSupport.createLocation(AlertImpl.this.getId());
            }

            @Override
            public Node render(RenderingProcessor renderingProcessor, Location location, Optional<Data> data)
            {
                return new AlertNode().setContent(renderingProcessor.process(AlertImpl.this.content, location))
                                      .setStyle(AlertImpl.this.style.name()
                                                                    .toLowerCase())
                                      .setDismissible(AlertImpl.this.dismissible);
            }

            @Override
            public void manageNodeRenderers(NodeRendererRegistry registry)
            {
                registry.register(AlertNode.class, NodeRenderType.HTML, (node, nodeRenderingProcessor) ->
                {
                    String dismissibleClass = node.isDismissible() ? " alert-dismissible" : "";
                    String dismissButton = node.isDismissible() ? "<button type=\"button\" class=\"btn-close\" aria-label=\"Close\"></button>" : "";
                    return "<div class=\"alert alert-" + node.getStyle() + dismissibleClass + "\" role=\"alert\">"
                           + nodeRenderingProcessor.render(node.getContent()) + dismissButton + "</div>";
                });
            }

            @Override
            public void manageEventHandler(EventHandlerRegistrationSupport eventHandlerRegistrationSupport)
            {
            }

            @Override
            public Stream<ParentLocationAndComponent> getSubComponents(Location parentLocation)
            {
                return Stream.of(ParentLocationAndComponent.of(parentLocation, AlertImpl.this.content));
            }

        };
    }

    @Override
    public UIComponentProvider<Alert> asTemplateProvider()
    {
        return () -> new AlertImpl(this.context, this.content, this.style, this.dismissible);
    }
}
