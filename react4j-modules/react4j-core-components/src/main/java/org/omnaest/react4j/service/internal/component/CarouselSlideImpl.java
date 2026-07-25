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

import org.omnaest.react4j.domain.Carousel.CarouselSlide;
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
import org.omnaest.react4j.service.internal.nodes.CarouselItemNode;
import org.omnaest.utils.element.cached.CachedElement;

public class CarouselSlideImpl extends AbstractUIComponentWithSubComponents<CarouselSlide> implements CarouselSlide
{
    private final int            index;

    private CachedElement<Image> image;
    private I18nText             caption;

    public CarouselSlideImpl(ComponentContext context, int index)
    {
        super(context);
        this.index = index;
        this.withId("carouselslide-" + index);
        this.image = CachedElement.of(() -> this.getUiComponentFactory()
                                                .newImage());
    }

    public CarouselSlideImpl(ComponentContext context, int index, Image image, I18nText caption)
    {
        super(context);
        this.index = index;
        this.withId("carouselslide-" + index);
        this.image = CachedElement.of(() -> image != null ? image
                : this.getUiComponentFactory()
                      .newImage());
        this.caption = caption;
    }

    @Override
    public CarouselSlide withImage(String imageName)
    {
        this.image.get()
                  .withImage(imageName);
        return this;
    }

    @Override
    public CarouselSlide withName(String name)
    {
        this.image.get()
                  .withName(name);
        return this;
    }

    @Override
    public CarouselSlide withCaption(String caption)
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
                return locationSupport.createLocation(CarouselSlideImpl.this.getId());
            }

            @Override
            public Node render(RenderingProcessor renderingProcessor, Location location, Optional<Data> data)
            {
                return new CarouselItemNode().setImage(renderingProcessor.process(CarouselSlideImpl.this.image.get(), location))
                                             .setCaption(CarouselSlideImpl.this.caption != null ? CarouselSlideImpl.this.getTextResolver()
                                                                                                                        .apply(CarouselSlideImpl.this.caption,
                                                                                                                               location)
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
                return Stream.of(ParentLocationAndComponent.of(parentLocation, CarouselSlideImpl.this.image.get()));
            }

        };
    }

    @Override
    public UIComponentProvider<CarouselSlide> asTemplateProvider()
    {
        return () -> new CarouselSlideImpl(this.context, this.index, this.image.get(), this.caption);
    }
}
