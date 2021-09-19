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
package org.omnaest.react4j.service.internal.service.internal;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.UIComponentFactory;
import org.omnaest.react4j.domain.context.data.Data;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.RenderableUIComponent;
import org.omnaest.react4j.domain.rendering.RenderableUIComponent.UIComponentWrapper;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer;
import org.omnaest.react4j.domain.rendering.components.LocationSupport;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.utils.MapperUtils;
import org.omnaest.utils.SetUtils;
import org.omnaest.utils.stream.FilterMapper;

/**
 * @author omnaest
 */
public class RenderingProcessorImpl implements RenderingProcessor
{
    private UIComponentFactory componentFactory;

    public RenderingProcessorImpl(UIComponentFactory componentFactory)
    {
        this.componentFactory = componentFactory;
    }

    @Override
    public Node process(UIComponent<?> component, Location parentLocation, Optional<Data> data)
    {
        Set<UIComponent<?>> ignoredComponents = Collections.emptySet();
        return this.process(component, parentLocation, data, ignoredComponents);
    }

    public Node process(UIComponent<?> component, Location parentLocation, Optional<Data> data, Set<UIComponent<?>> ignoredComponents)
    {
        FilterMapper<UIComponent<?>, RenderableUIComponent<?>> renderableUIComponentFilterMapper = MapperUtils.filterMapper(iComponent -> iComponent instanceof RenderableUIComponent,
                                                                                                                            iComponent -> (RenderableUIComponent<?>) iComponent);

        Set<UIComponent<?>> currentIgnoredComponents = SetUtils.toNew(ignoredComponents);
        return Optional.ofNullable(component)
                       .filter(renderableUIComponentFilterMapper)
                       .map(renderableUIComponentFilterMapper)
                       .map(this.createComponentWrapperMapper(currentIgnoredComponents))
                       .filter(renderableUIComponentFilterMapper)
                       .map(renderableUIComponentFilterMapper)
                       .map(this.createComponentRenderer(parentLocation, data, currentIgnoredComponents))
                       .orElse(null);
    }

    @SuppressWarnings("unchecked")
    private Function<RenderableUIComponent<?>, UIComponent<?>> createComponentWrapperMapper(Set<UIComponent<?>> currentIgnoredComponents)
    {
        return iComponent ->
        {
            if (currentIgnoredComponents.contains(iComponent))
            {
                return iComponent;
            }
            else
            {
                UIComponentWrapper<UIComponent<?>> uiComponentWrapper = (UIComponentWrapper<UIComponent<?>>) iComponent.getWrapper();
                UIComponent<?> wrapperComponent = uiComponentWrapper.apply(this.componentFactory, iComponent);

                if (wrapperComponent != iComponent)
                {
                    currentIgnoredComponents.add(iComponent);
                }

                return wrapperComponent;
            }
        };
    }

    private Function<RenderableUIComponent<?>, Node> createComponentRenderer(Location parentLocation, Optional<Data> data,
                                                                             Set<UIComponent<?>> currentIgnoredComponents)
    {
        return component ->
        {
            LocationSupport locationSupport = new LocationSupportImpl(parentLocation);
            UIComponentRenderer renderer = component.asRenderer();
            Location location = renderer.getLocation(locationSupport);
            return renderer.render(this.createFilteringRenderingProcessor(currentIgnoredComponents), location, data);
        };
    }

    private RenderingProcessor createFilteringRenderingProcessor(Set<UIComponent<?>> currentIgnoredComponents)
    {
        return new RenderingProcessor()
        {
            @Override
            public Node process(UIComponent<?> component, Location parentLocation, Optional<Data> data)
            {
                return RenderingProcessorImpl.this.process(component, parentLocation, data, currentIgnoredComponents);
            }
        };
    }

}
