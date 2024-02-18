package org.omnaest.react4j.data.datagrid.internal.config;

import org.omnaest.react4j.data.datagrid.internal.DataGridRepositoryProvider;
import org.springframework.context.annotation.Bean;

public class DataGridRepositoryProviderConfiguration
{

    @Bean
    public DataGridRepositoryProvider newDataGridRepositoryProvider()
    {
        return new DataGridRepositoryProvider();
    }

}
