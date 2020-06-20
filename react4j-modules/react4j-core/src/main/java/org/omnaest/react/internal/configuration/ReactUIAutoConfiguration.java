package org.omnaest.react.internal.configuration;

import org.omnaest.react.EnableReactUI;
import org.omnaest.react.ReactUIService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackageClasses = ReactUIService.class)
@ConditionalOnWebApplication
@ConditionalOnBean(annotation = EnableReactUI.class)
public class ReactUIAutoConfiguration
{
}
