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
import org.omnaest.react4j.domain.ScrollbarContainer;
import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer;
import org.omnaest.react4j.domain.rendering.components.LocationSupport;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.domain.rendering.node.NodeRendererRegistry;
import org.omnaest.react4j.service.internal.nodes.ScrollbarContainerNode;

public class ScrollbarContainerImpl extends AbstractUIComponentAndContentHolder<ScrollbarContainer> implements ScrollbarContainer
{
    private UIComponent<?>    content;
    private VerticalBoxMode   verticalBoxMode   = VerticalBoxMode.DEFAULT_HEIGHT;
    private HorizontalBoxMode horizontalBoxMode = HorizontalBoxMode.DEFAULT_WIDTH;

    public ScrollbarContainerImpl(ComponentContext context)
    {
        super(context);
    }

    @Override
    public UIComponentRenderer asRenderer()
    {
        return new UIComponentRenderer()
        {

            @Override
            public Location getLocation(LocationSupport locationSupport)
            {
                return locationSupport.createLocation(ScrollbarContainerImpl.this.getId());
            }

            @Override
            public Node render(RenderingProcessor renderingProcessor, Location location)
            {
                return new ScrollbarContainerNode().setContent(Optional.ofNullable(ScrollbarContainerImpl.this.content)
                                                                       .map(content -> renderingProcessor.process(content, location))
                                                                       .orElse(null))
                                                   .setVerticalBoxMode(ScrollbarContainerImpl.this.verticalBoxMode.name()
                                                                                                                  .replaceAll("_", "-")
                                                                                                                  .toLowerCase())
                                                   .setHorizontalBoxMode(ScrollbarContainerImpl.this.horizontalBoxMode.name()
                                                                                                                      .replaceAll("_", "-")
                                                                                                                      .toLowerCase());
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
            public Stream<UIComponent<?>> getSubComponents()
            {
                return Stream.of(ScrollbarContainerImpl.this.content);
            }

        };
    }

    @Override
    public ScrollbarContainer withContent(UIComponent<?> component)
    {
        this.content = component;
        return this;
    }

    @Override
    public ScrollbarContainer withVerticalBox(VerticalBoxMode verticalBoxMode)
    {
        this.verticalBoxMode = verticalBoxMode;
        return this;
    }

    @Override
    public ScrollbarContainer withHorizontalBox(HorizontalBoxMode horizontalBoxMode)
    {
        this.horizontalBoxMode = horizontalBoxMode;
        return this;
    }

}
