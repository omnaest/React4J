package org.omnaest.react4j.service.internal.service.internal;

import org.omnaest.react4j.data.RepositoryProvider;
import org.omnaest.react4j.domain.Locations;
import org.omnaest.react4j.domain.data.DefineableDataContext;
import org.omnaest.react4j.service.internal.service.DataContextFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DataContextFactoryImpl implements DataContextFactory
{
    @Autowired(required = false)
    protected RepositoryProvider repositoryProvider;

    @Override
    public DefineableDataContext newInstance(Locations locations)
    {
        this.assertRepositoryProviderNotNull();
        return new DataContextImpl<Object>(locations, this.repositoryProvider);
    }

    private void assertRepositoryProviderNotNull()
    {
        if (this.repositoryProvider == null)
        {
            throw new IllegalStateException("No RepositoryProvider is available, you cannot use a DataContext without it. Please consider adding a repository providing dependency like react4j-data-datagrid or any custom provider.");
        }
    }

}
