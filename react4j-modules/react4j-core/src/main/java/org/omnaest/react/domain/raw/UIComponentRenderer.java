package org.omnaest.react.domain.raw;

import org.omnaest.react.domain.Location;

public interface UIComponentRenderer
{
    public default Node render()
    {
        return this.render(Location.empty());
    }

    public Node render(Location parentLocation);
}