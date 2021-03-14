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
import org.omnaest.react4j.domain.PaddingContainer;
import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.context.data.Data;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer;
import org.omnaest.react4j.domain.rendering.components.LocationSupport;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.domain.rendering.node.NodeRendererRegistry;
import org.omnaest.react4j.domain.support.UIComponentProvider;
import org.omnaest.react4j.service.internal.nodes.PaddingContainerNode;

public class PaddingContainerImpl extends AbstractUIComponentAndContentHolder<PaddingContainer> implements PaddingContainer
{
    private boolean        verticalPadding   = true;
    private boolean        horizontalPadding = true;
    private UIComponent<?> content;

    public PaddingContainerImpl(ComponentContext context)
    {
        super(context);
    }

    public PaddingContainerImpl(ComponentContext context, boolean verticalPadding, boolean horizontalPadding, UIComponent<?> content)
    {
        super(context);
        this.verticalPadding = verticalPadding;
        this.horizontalPadding = horizontalPadding;
        this.content = content;
    }

    @Override
    public UIComponentRenderer asRenderer()
    {
        return new UIComponentRenderer()
        {

            @Override
            public Location getLocation(LocationSupport locationSupport)
            {
                return locationSupport.createLocation(PaddingContainerImpl.this.getId());
            }

            @Override
            public Node render(RenderingProcessor renderingProcessor, Location location, Optional<Data> data)
            {
                return new PaddingContainerNode().setHorizontal(PaddingContainerImpl.this.horizontalPadding)
                                                 .setVertical(PaddingContainerImpl.this.verticalPadding)
                                                 .setContent(renderingProcessor.process(PaddingContainerImpl.this.content, location));
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
                return Stream.of(ParentLocationAndComponent.of(parentLocation, PaddingContainerImpl.this.content));
            }

        };
    }

    @Override
    public PaddingContainer withVerticalPadding(boolean verticalPadding)
    {
        this.verticalPadding = verticalPadding;
        return this;
    }

    @Override
    public PaddingContainer withHorizontalPadding(boolean horizontalPadding)
    {
        this.horizontalPadding = horizontalPadding;
        return this;
    }

    @Override
    public PaddingContainer withNoHorizontalPadding()
    {
        return this.withHorizontalPadding(false);
    }

    @Override
    public PaddingContainer withNoVerticalPadding()
    {
        return this.withVerticalPadding(false);
    }

    @Override
    public PaddingContainer withContent(UIComponent<?> component)
    {
        this.content = component;
        return this;
    }

    @Override
    public UIComponentProvider<PaddingContainer> asTemplateProvider()
    {
        return () -> new PaddingContainerImpl(this.context, this.verticalPadding, this.horizontalPadding, this.content);
    }

}
