package org.omnaest.react4j.service.internal.service.internal;

import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.UIComponent;
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
    public Node process(UIComponent<?> component, Location parentLocation)
    {
        LocationSupport locationSupport = new LocationSupport()
        {
            @Override
            public Location getParentLocation()
            {
                return parentLocation;
            }

            @Override
            public Location createLocation(String id)
            {
                return Location.of(this.getParentLocation(), id);
            }
        };

        UIComponentRenderer renderer = ((RenderableUIComponent<?>) component).asRenderer();
        Location location = renderer.getLocation(locationSupport);
        return renderer.render(this, location);
    }
}