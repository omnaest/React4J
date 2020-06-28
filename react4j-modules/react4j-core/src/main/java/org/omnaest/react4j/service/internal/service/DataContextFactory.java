package org.omnaest.react4j.service.internal.service;

import org.omnaest.react4j.domain.Locations;
import org.omnaest.react4j.domain.data.DataContext;

public interface DataContextFactory
{
    public DataContext newInstance(Locations locations);
}
