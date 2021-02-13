package org.omnaest.react4j.service.internal.service;

import java.util.Map;

import org.omnaest.react4j.domain.configuration.HomePageConfiguration;

/**
 * The internal service managing the {@link HomePageConfiguration}
 * 
 * @author omnaest
 */
public interface HomePageConfigurationService extends HomePageConfiguration
{

    Map<String, String> getConfigurations();

}
