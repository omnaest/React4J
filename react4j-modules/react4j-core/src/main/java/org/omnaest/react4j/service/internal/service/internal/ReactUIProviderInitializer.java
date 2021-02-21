package org.omnaest.react4j.service.internal.service.internal;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.annotation.PostConstruct;

import org.omnaest.react4j.ReactUIProvider;
import org.omnaest.react4j.service.ReactUIService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ReactUIProviderInitializer
{
    private static final Logger LOG = LoggerFactory.getLogger(ReactUIProviderInitializer.class);

    @Autowired(required = false)
    private List<ReactUIProvider> providers;

    @Autowired
    private ReactUIService reactUIService;

    @PostConstruct
    public void init()
    {
        Optional.ofNullable(this.providers)
                .orElseGet(() ->
                {
                    LOG.info("No ReactUIProvider instances are available");
                    return Collections.emptyList();
                })
                .forEach(provider -> this.reactUIService.getOrCreateRoot(provider.getContextPath(), provider));
    }
}
