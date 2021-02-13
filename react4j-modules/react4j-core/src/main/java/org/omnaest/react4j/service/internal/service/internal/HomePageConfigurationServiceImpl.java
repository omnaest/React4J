package org.omnaest.react4j.service.internal.service.internal;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.omnaest.react4j.domain.configuration.HomePageConfiguration;
import org.omnaest.react4j.service.internal.service.HomePageConfigurationService;
import org.springframework.stereotype.Service;

@Service
public class HomePageConfigurationServiceImpl implements HomePageConfigurationService
{
    private Map<String, String> configurations = new ConcurrentHashMap<>();

    @Override
    public HomePageConfiguration setTitle(String title)
    {
        this.configurations.put("$TITLE$", title);
        return this;
    }

    @Override
    public Map<String, String> getConfigurations()
    {
        return this.configurations;
    }

    @Override
    public HomePageConfiguration setDescription(String description)
    {
        this.configurations.put("$DESCRIPTION$", description);
        return this;
    }

}
