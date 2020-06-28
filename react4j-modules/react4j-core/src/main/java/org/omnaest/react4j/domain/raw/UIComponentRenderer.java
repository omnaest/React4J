package org.omnaest.react4j.domain.raw;

import org.omnaest.react4j.domain.Location;

public interface UIComponentRenderer
{
    public default Node render()
    {
        return this.render(Location.empty());
    }

    public Node render(Location parentLocation);
}