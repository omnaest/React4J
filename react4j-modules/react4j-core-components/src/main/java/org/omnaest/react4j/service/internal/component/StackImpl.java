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
import org.omnaest.react4j.domain.Stack;
import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.context.data.Data;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer;
import org.omnaest.react4j.domain.rendering.components.LocationSupport;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.domain.rendering.node.NodeRenderType;
import org.omnaest.react4j.domain.rendering.node.NodeRendererRegistry;
import org.omnaest.react4j.domain.support.UIComponentProvider;
import org.omnaest.react4j.service.internal.nodes.StackNode;

public class StackImpl extends AbstractUIComponentAndContentHolder<Stack> implements Stack
{
    private UIComponent<?> content;
    private Direction      direction = Direction.VERTICAL;
    private int            gap;

    public StackImpl(ComponentContext context)
    {
        super(context);
    }

    public StackImpl(ComponentContext context, UIComponent<?> content, Direction direction, int gap)
    {
        super(context);
        this.content = content;
        this.direction = direction;
        this.gap = gap;
    }

    @Override
    public Stack withContent(UIComponent<?> component)
    {
        this.content = component;
        return this;
    }

    @Override
    public Stack withDirection(Direction direction)
    {
        this.direction = direction;
        return this;
    }

    @Override
    public Stack withGap(int gap)
    {
        this.gap = gap;
        return this;
    }

    @Override
    public UIComponentRenderer asRenderer()
    {
        return new UIComponentRenderer() {
            @Override
            public Location getLocation(LocationSupport locationSupport)
            {
                return locationSupport.createLocation(StackImpl.this.getId());
            }

            @Override
            public Node render(RenderingProcessor renderingProcessor, Location location, Optional<Data> data)
            {
                return new StackNode().setContent(renderingProcessor.process(StackImpl.this.content, location))
                                      .setDirection(StackImpl.this.direction.name()
                                                                            .toLowerCase())
                                      .setGap(StackImpl.this.gap);
            }

            @Override
            public void manageNodeRenderers(NodeRendererRegistry registry)
            {
                registry.register(StackNode.class, NodeRenderType.HTML, (node, nodeRenderingProcessor) ->
                {
                    String flexDirection = "horizontal".equals(node.getDirection()) ? "row" : "column";
                    return "<div class=\"d-flex flex-" + flexDirection + " gap-" + node.getGap() + "\">"
                           + nodeRenderingProcessor.render(node.getContent()) + "</div>";
                });
            }

            @Override
            public void manageEventHandler(EventHandlerRegistrationSupport eventHandlerRegistrationSupport)
            {
            }

            @Override
            public Stream<ParentLocationAndComponent> getSubComponents(Location parentLocation)
            {
                return Stream.of(ParentLocationAndComponent.of(parentLocation, StackImpl.this.content));
            }

        };
    }

    @Override
    public UIComponentProvider<Stack> asTemplateProvider()
    {
        return () -> new StackImpl(this.context, this.content, this.direction, this.gap);
    }
}
