package org.omnaest.react4j.data.datagrid.internal.config;

import org.omnaest.react4j.data.datagrid.internal.DataGridRepositoryProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Registers the {@link DataGridRepositoryProvider}. Deliberately <b>not</b> annotated with a stereotype ({@code @Configuration}, {@code @Component}):
 * it is only ever imported by {@code @EnableReactUIDataGridRepository}. A stereotype would make it a candidate of the component scan
 * {@code @EnableReactUIRepository} performs over the whole {@code org.omnaest.react4j.data} package tree, which would start an embedded data grid in
 * every application that merely has react4j-data-datagrid on its classpath (guarded by EnableReactUIDataGridRepositoryTest).
 * <br>
 * The provider is {@link Primary}: when the in-memory provider is registered as well, whether it was registered before this configuration depends on
 * the order of the annotations, and without a primary two candidates would make the single {@code RepositoryProvider} injection fail.
 */
public class DataGridRepositoryProviderConfiguration
{

    @Bean
    @Primary
    public DataGridRepositoryProvider newDataGridRepositoryProvider()
    {
        return new DataGridRepositoryProvider();
    }

}
