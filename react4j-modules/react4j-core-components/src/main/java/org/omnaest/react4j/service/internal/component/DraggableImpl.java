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

import org.omnaest.react4j.domain.Draggable;
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
import org.omnaest.react4j.service.internal.nodes.DraggableNode;

/**
 * @see Draggable
 * @author omnaest
 */
public class DraggableImpl extends AbstractUIComponentAndContentHolder<Draggable> implements Draggable
{
    private UIComponent<?> content;
    private String         dragId;

    public DraggableImpl(ComponentContext context)
    {
        super(context);
    }

    public DraggableImpl(ComponentContext context, UIComponent<?> content, String dragId)
    {
        super(context);
        this.content = content;
        this.dragId = dragId;
    }

    @Override
    public Draggable withDragId(String dragId)
    {
        this.dragId = dragId;
        return this;
    }

    @Override
    public Draggable withContent(UIComponent<?> component)
    {
        this.content = component;
        return this;
    }

    @Override
    public UIComponentRenderer asRenderer()
    {
        return new UIComponentRenderer() {

            @Override
            public Location getLocation(LocationSupport locationSupport)
            {
                return locationSupport.createLocation(DraggableImpl.this.getId());
            }

            @Override
            public Node render(RenderingProcessor renderingProcessor, Location location, Optional<Data> data)
            {
                return new DraggableNode().setContent(Optional.ofNullable(DraggableImpl.this.content)
                                                              .map(content -> renderingProcessor.process(content, location))
                                                              .orElse(null))
                                          .setDragId(DraggableImpl.this.dragId);
            }

            @Override
            public void manageNodeRenderers(NodeRendererRegistry registry)
            {
                registry.register(DraggableNode.class, NodeRenderType.HTML,
                                  (node, nodeRenderingProcessor) -> "<div>" + Optional.ofNullable(node.getContent())
                                                                                      .map(nodeRenderingProcessor::render)
                                                                                      .orElse("")
                                                                    + "</div>");
            }

            @Override
            public void manageEventHandler(EventHandlerRegistrationSupport eventHandlerRegistrationSupport)
            {
                // purely client-side interaction (drag) - there is no server EventHandler to register
            }

            @Override
            public Stream<ParentLocationAndComponent> getSubComponents(Location parentLocation)
            {
                return DraggableImpl.this.content != null ? Stream.of(ParentLocationAndComponent.of(parentLocation, DraggableImpl.this.content))
                        : Stream.empty();
            }

        };
    }

    @Override
    public UIComponentProvider<Draggable> asTemplateProvider()
    {
        return () -> new DraggableImpl(this.context, this.content, this.dragId);
    }
}
