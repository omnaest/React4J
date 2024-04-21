package org.omnaest.react4j.domain.context.data.source.registry;

import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.context.data.source.DataSource;

public interface DataSourceRegistry
{

    public void register(Location location, DataSource dataSource);

}
