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

import org.omnaest.react4j.domain.Figure;
import org.omnaest.react4j.domain.Image;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.context.data.Data;
import org.omnaest.react4j.domain.i18n.I18nText;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer;
import org.omnaest.react4j.domain.rendering.components.LocationSupport;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.domain.rendering.node.NodeRendererRegistry;
import org.omnaest.react4j.domain.support.UIComponentProvider;
import org.omnaest.react4j.service.internal.nodes.FigureNode;
import org.omnaest.utils.element.cached.CachedElement;

public class FigureImpl extends AbstractUIComponentWithSubComponents<Figure> implements Figure
{
    private CachedElement<Image> image;
    private I18nText             caption;

    public FigureImpl(ComponentContext context)
    {
        super(context);
        this.image = CachedElement.of(() -> this.getUiComponentFactory()
                                                .newImage());
    }

    public FigureImpl(ComponentContext context, Image image, I18nText caption)
    {
        super(context);
        this.image = CachedElement.of(() -> image != null ? image
                : this.getUiComponentFactory()
                      .newImage());
        this.caption = caption;
    }

    @Override
    public Figure withImage(String imageName)
    {
        this.image.get()
                  .withImage(imageName);
        return this;
    }

    @Override
    public Figure withName(String name)
    {
        this.image.get()
                  .withName(name);
        return this;
    }

    @Override
    public Figure withCaption(String caption)
    {
        this.caption = this.toI18nText(caption);
        return this;
    }

    @Override
    public UIComponentRenderer asRenderer()
    {
        return new UIComponentRenderer() {
            @Override
            public Location getLocation(LocationSupport locationSupport)
            {
                return locationSupport.createLocation(FigureImpl.this.getId());
            }

            @Override
            public Node render(RenderingProcessor renderingProcessor, Location location, Optional<Data> data)
            {
                return new FigureNode().setImage(renderingProcessor.process(FigureImpl.this.image.get(), location))
                                       .setCaption(FigureImpl.this.getTextResolver()
                                                                  .apply(FigureImpl.this.caption, location));
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
                return Stream.of(ParentLocationAndComponent.of(parentLocation, FigureImpl.this.image.get()));
            }

        };
    }

    @Override
    public UIComponentProvider<Figure> asTemplateProvider()
    {
        return () -> new FigureImpl(this.context, this.image.get(), this.caption);
    }
}
