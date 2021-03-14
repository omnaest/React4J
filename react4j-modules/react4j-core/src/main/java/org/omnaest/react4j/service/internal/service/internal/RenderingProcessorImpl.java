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

import java.util.Optional;
import java.util.function.Function;

import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.context.data.Data;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.RenderableUIComponent;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer;
import org.omnaest.react4j.domain.rendering.components.LocationSupport;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;

/**
 * @author omnaest
 */
public class RenderingProcessorImpl implements RenderingProcessor
{
    @Override
    public Node process(UIComponent<?> component, Location parentLocation, Optional<Data> data)
    {
        return Optional.ofNullable(component)
                       .map(this.createComponentRenderer(parentLocation, data))
                       .orElse(null);
    }

    private Function<UIComponent<?>, Node> createComponentRenderer(Location parentLocation, Optional<Data> data)
    {
        return component ->
        {
            LocationSupport locationSupport = new LocationSupportImpl(parentLocation);

            UIComponentRenderer renderer = ((RenderableUIComponent<?>) component).asRenderer();
            Location location = renderer.getLocation(locationSupport);
            return renderer.render(this, location, data);
        };
    }

    @Override
    public Node process(UIComponent<?> component, Location parentLocation)
    {
        return this.process(component, parentLocation, Optional.empty());
    }
}
