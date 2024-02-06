package org.omnaest.react4j.data.datagrid.internal.config;

import org.omnaest.react4j.data.annotations.EnableReactUIRepository;
import org.omnaest.react4j.data.datagrid.internal.DataGridRepositoryProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

public class DataGridRepositoryProviderConfiguration
{

    @Bean
    @ConditionalOnWebApplication
    @ConditionalOnBean(annotation = EnableReactUIRepository.class)
    public DataGridRepositoryProvider newDataGridRepositoryProvider()
    {
        return new DataGridRepositoryProvider();
    }

}
