package org.omnaest.react4j.service.internal.service;

import org.omnaest.react4j.domain.Locations;
import org.omnaest.react4j.domain.data.DefineableDataContext;

public interface DataContextFactory
{
    public DefineableDataContext newInstance(Locations locations);
}
