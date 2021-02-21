package org.omnaest.react4j.data.internal.configuration;

import org.omnaest.react4j.data.annotations.EnableReactUIInMemoryRepository;
import org.omnaest.react4j.data.provider.memory.InMemoryRepositoryProviderMarker;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackageClasses = InMemoryRepositoryProviderMarker.class)
@ConditionalOnWebApplication
@ConditionalOnBean(annotation = EnableReactUIInMemoryRepository.class)
public class ReactUIDataInMemoryAutoConfiguration
{
}
