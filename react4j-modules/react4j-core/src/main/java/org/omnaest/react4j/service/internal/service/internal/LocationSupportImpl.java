package org.omnaest.react4j.service.internal.service.internal;

import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.rendering.components.LocationSupport;

public class LocationSupportImpl implements LocationSupport
{
    private final Location parentLocation;

    public LocationSupportImpl(Location parentLocation)
    {
        this.parentLocation = parentLocation;
    }

    @Override
    public Location getParentLocation()
    {
        return this.parentLocation;
    }

    @Override
    public Location createLocation(String id)
    {
        return Location.of(this.getParentLocation(), id);
    }
}