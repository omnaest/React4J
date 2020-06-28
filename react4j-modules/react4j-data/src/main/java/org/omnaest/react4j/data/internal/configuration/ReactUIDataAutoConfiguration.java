package org.omnaest.react4j.data.internal.configuration;

import org.omnaest.react4j.data.EnableReactUIRepository;
import org.omnaest.react4j.data.RepositoryProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackageClasses = RepositoryProvider.class)
@ConditionalOnWebApplication
@ConditionalOnBean(annotation = EnableReactUIRepository.class)
public class ReactUIDataAutoConfiguration
{
}
