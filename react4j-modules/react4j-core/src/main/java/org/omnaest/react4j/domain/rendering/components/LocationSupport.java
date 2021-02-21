package org.omnaest.react4j.domain.rendering.components;

import org.omnaest.react4j.domain.Location;

public interface LocationSupport
{
    public Location getParentLocation();

    /**
     * Creates the current {@link Location} based on a given unique identifier and the {@link #getParentLocation()}
     * 
     * @param id
     * @return
     */
    public Location createLocation(String id);
}