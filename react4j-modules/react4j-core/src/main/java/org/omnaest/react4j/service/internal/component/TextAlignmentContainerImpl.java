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
import org.omnaest.react4j.domain.TextAlignmentContainer;
import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer;
import org.omnaest.react4j.domain.rendering.components.LocationSupport;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.domain.rendering.node.NodeRendererRegistry;
import org.omnaest.react4j.domain.support.UIComponentProvider;
import org.omnaest.react4j.service.internal.nodes.TextAlignmentContainerNode;

public class TextAlignmentContainerImpl extends AbstractUIComponentAndContentHolder<TextAlignmentContainer> implements TextAlignmentContainer
{
    private UIComponentProvider<?> content  = UIComponentProvider.empty();
    private boolean                ellipsis = false;
    private boolean                nowrap   = false;
    private HorizontalAlignment    horizontalAlignment;
    private VerticalAlignment      verticalAlignment;

    public TextAlignmentContainerImpl(ComponentContext context)
    {
        super(context);
    }

    @Override
    public TextAlignmentContainerImpl withEllipsis(boolean ellipsis)
    {
        this.ellipsis = ellipsis;
        return this;
    }

    @Override
    public TextAlignmentContainerImpl withNowrap(boolean nowrap)
    {
        this.nowrap = nowrap;
        return this;
    }

    @Override
    public TextAlignmentContainerImpl withHorizontalAlignment(HorizontalAlignment horizontalAlignment)
    {
        this.horizontalAlignment = horizontalAlignment;
        return this;
    }

    @Override
    public TextAlignmentContainerImpl withVerticalAlignment(VerticalAlignment verticalAlignment)
    {
        this.verticalAlignment = verticalAlignment;
        return this;
    }

    @Override
    public UIComponentRenderer asRenderer()
    {
        return new UIComponentRenderer()
        {

            @Override
            public Location getLocation(LocationSupport locationSupport)
            {
                return locationSupport.createLocation(TextAlignmentContainerImpl.this.getId());
            }

            @Override
            public Node render(RenderingProcessor renderingProcessor, Location location)
            {
                return new TextAlignmentContainerNode().setEllipsis(TextAlignmentContainerImpl.this.ellipsis)
                                                       .setNowrap(TextAlignmentContainerImpl.this.nowrap)
                                                       .setHorizontalAlignment(Optional.ofNullable(TextAlignmentContainerImpl.this.horizontalAlignment)
                                                                                       .map(HorizontalAlignment::name)
                                                                                       .map(String::toLowerCase)
                                                                                       .orElse(null))
                                                       .setVerticalAlignment(Optional.ofNullable(TextAlignmentContainerImpl.this.verticalAlignment)
                                                                                     .map(VerticalAlignment::name)
                                                                                     .map(String::toLowerCase)
                                                                                     .orElse(null))
                                                       .setContent(renderingProcessor.process(TextAlignmentContainerImpl.this.content.get(), location));
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
                return Stream.of(TextAlignmentContainerImpl.this.content.get());
            }

        };
    }

    @Override
    public TextAlignmentContainer withContent(UIComponent<?> component)
    {
        return this.withContent(UIComponentProvider.of(component));
    }

    @Override
    public TextAlignmentContainer withContent(UIComponentProvider<?> componentProvider)
    {
        this.content = componentProvider;
        return this;
    }

}
