/*******************************************************************************
 * Copyright 2021 Danny Kunz
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package org.omnaest.react4j.service.internal.service.internal;

import org.omnaest.react4j.data.provider.RepositoryProvider;
import org.omnaest.react4j.domain.Locations;
import org.omnaest.react4j.domain.context.data.DefineableDataContext;
import org.omnaest.react4j.domain.context.ui.UIContext;
import org.omnaest.react4j.service.internal.service.ContextFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ContextFactoryImpl implements ContextFactory
{
    @Autowired(required = false)
    protected RepositoryProvider repositoryProvider;

    @Override
    public DefineableDataContext newDataContextInstance(Locations locations)
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

    @Override
    public UIContext newUIContextInstance(Locations locations)
    {
        return new UIContextImpl();
    }

}
