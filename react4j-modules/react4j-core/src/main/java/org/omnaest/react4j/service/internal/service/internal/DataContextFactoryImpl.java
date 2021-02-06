package org.omnaest.react4j.service.internal.service.internal;

import org.omnaest.react4j.data.RepositoryProvider;
import org.omnaest.react4j.domain.Locations;
import org.omnaest.react4j.domain.data.DataContext;
import org.omnaest.react4j.service.internal.service.DataContextFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DataContextFactoryImpl implements DataContextFactory
{
    @Autowired(required = false)
    protected RepositoryProvider repositoryProvider;

    @Override
    public DataContext newInstance(Locations locations)
    {
        return new DataContextImpl(locations, this.repositoryProvider);
    }

}
