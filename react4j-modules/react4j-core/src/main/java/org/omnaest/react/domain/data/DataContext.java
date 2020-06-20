package org.omnaest.react.domain.data;

import org.omnaest.react.domain.Location;
import org.omnaest.react.domain.Locations;

public interface DataContext
{
    public String getId(Location location);

    public SingletonDataContext enableSingleton();

    public static DataContext newInstance(Locations locations)
    {
        return new DataContextImpl(locations);
    }

}
