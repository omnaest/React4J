package org.omnaest.react4j.data.provider.memory.config;

import org.omnaest.react4j.data.annotations.EnableReactUIInMemoryRepository;
import org.omnaest.react4j.data.provider.memory.InMemoryRepositoryProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InMemoryRepositoryProviderConfiguration
{
    @Bean
    @ConditionalOnWebApplication
    @ConditionalOnBean(annotation = EnableReactUIInMemoryRepository.class)
    public InMemoryRepositoryProvider newInMemoryRepositoryProvider()
    {
        return new InMemoryRepositoryProvider();
    }
}
