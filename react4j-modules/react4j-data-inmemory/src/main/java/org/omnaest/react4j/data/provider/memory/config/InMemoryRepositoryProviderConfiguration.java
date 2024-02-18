package org.omnaest.react4j.data.provider.memory.config;

import org.omnaest.react4j.data.provider.RepositoryProvider;
import org.omnaest.react4j.data.provider.memory.InMemoryRepositoryProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InMemoryRepositoryProviderConfiguration
{
    @Bean
    @ConditionalOnMissingBean(RepositoryProvider.class)
    public InMemoryRepositoryProvider newInMemoryRepositoryProvider()
    {
        return new InMemoryRepositoryProvider();
    }
}
