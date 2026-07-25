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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.omnaest.react4j.domain.Carousel;
import org.omnaest.react4j.domain.Carousel.CarouselSlide;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.context.data.Data;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer;
import org.omnaest.react4j.domain.rendering.components.LocationSupport;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.domain.rendering.node.NodeRendererRegistry;
import org.omnaest.react4j.domain.support.UIComponentProvider;
import org.omnaest.react4j.service.internal.nodes.CarouselItemNode;
import org.omnaest.react4j.service.internal.nodes.CarouselNode;

public class CarouselImpl extends AbstractUIComponentWithSubComponents<Carousel> implements Carousel
{
    private List<CarouselSlideImpl> slides     = new ArrayList<>();
    private Integer                 interval;
    private boolean                 controls   = true;
    private boolean                 indicators = true;
    private boolean                 fade       = false;

    public CarouselImpl(ComponentContext context)
    {
        super(context);
    }

    public CarouselImpl(ComponentContext context, List<CarouselSlideImpl> slides, Integer interval, boolean controls, boolean indicators, boolean fade)
    {
        super(context);
        this.slides = slides;
        this.interval = interval;
        this.controls = controls;
        this.indicators = indicators;
        this.fade = fade;
    }

    @Override
    public Carousel addSlide(Consumer<CarouselSlide> carouselSlideConsumer)
    {
        CarouselSlideImpl slide = new CarouselSlideImpl(this.context, this.slides.size());
        carouselSlideConsumer.accept(slide);
        this.slides.add(slide);
        return this;
    }

    @Override
    public Carousel withInterval(int interval, TimeUnit timeUnit)
    {
        this.interval = (int) timeUnit.toMillis(interval);
        return this;
    }

    @Override
    public Carousel withControls(boolean controls)
    {
        this.controls = controls;
        return this;
    }

    @Override
    public Carousel withIndicators(boolean indicators)
    {
        this.indicators = indicators;
        return this;
    }

    @Override
    public Carousel withFade(boolean fade)
    {
        this.fade = fade;
        return this;
    }

    @Override
    public UIComponentRenderer asRenderer()
    {
        return new UIComponentRenderer() {
            @Override
            public Location getLocation(LocationSupport locationSupport)
            {
                return locationSupport.createLocation(CarouselImpl.this.getId());
            }

            @Override
            public Node render(RenderingProcessor renderingProcessor, Location location, Optional<Data> data)
            {
                return new CarouselNode().setItems(CarouselImpl.this.slides.stream()
                                                                           .map(slide -> (CarouselItemNode) renderingProcessor.process(slide, location))
                                                                           .collect(Collectors.toList()))
                                         .setInterval(CarouselImpl.this.interval)
                                         .setControls(CarouselImpl.this.controls)
                                         .setIndicators(CarouselImpl.this.indicators)
                                         .setFade(CarouselImpl.this.fade);
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
                return CarouselImpl.this.slides.stream()
                                               .map(slide -> ParentLocationAndComponent.of(parentLocation, slide));
            }

        };
    }

    @Override
    public UIComponentProvider<Carousel> asTemplateProvider()
    {
        return () -> new CarouselImpl(this.context, this.slides.stream()
                                                               .collect(Collectors.toList()),
                                      this.interval, this.controls, this.indicators, this.fade);
    }
}
