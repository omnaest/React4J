package org.omnaest.react4j.service.internal.service.internal;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.annotation.PostConstruct;

import org.omnaest.react4j.ReactUIProvider;
import org.omnaest.react4j.service.ReactUIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ReactUIProviderInitializer
{
    @Autowired
    private List<ReactUIProvider> providers;

    @Autowired
    private ReactUIService reactUIService;

    @PostConstruct
    public void init()
    {
        Optional.ofNullable(this.providers)
                .orElse(Collections.emptyList())
                .forEach(provider -> this.reactUIService.getOrCreateRoot(provider.getContextPath(), provider));
    }
}
