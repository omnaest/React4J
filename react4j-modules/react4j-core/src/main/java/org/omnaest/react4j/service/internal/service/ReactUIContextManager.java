package org.omnaest.react4j.service.internal.service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.omnaest.react4j.domain.ReactUI;
import org.omnaest.react4j.service.internal.domain.ReactUIInternal;
import org.omnaest.utils.duration.TimeDuration;
import org.omnaest.utils.element.cached.CachedElement;

public class ReactUIContextManager
{
    private Map<String, ReactUIInternalProvider> contextPathToReactUIFactory = new ConcurrentHashMap<>();

    public static interface ReactUIInternalProvider extends Supplier<ReactUIInternal>
    {
        public static ReactUIInternalProvider cached(Supplier<ReactUIInternal> factory, int cacheDurationInSeconds)
        {
            CachedElement<ReactUIInternal> cachedReactUI = CachedElement.of(factory)
                                                                        .asDurationLimitedCachedElement(TimeDuration.of(cacheDurationInSeconds,
                                                                                                                        TimeUnit.SECONDS));
            return () -> cachedReactUI.get();
        }

        public static ReactUIInternalProvider nonCached(Supplier<ReactUIInternal> factory)
        {
            return () -> factory.get();
        }

        public static ReactUIInternalProvider fromFactory(boolean cachingEnabled, int cacheDurationInSeconds, Supplier<ReactUIInternal> factory)
        {
            if (cachingEnabled)
            {
                return cached(factory, cacheDurationInSeconds);
            }
            else
            {
                return nonCached(factory);
            }
        }
    }

    public ReactUI computeIfAbsent(String contextPath, ReactUIInternalProvider provider)
    {
        return this.contextPathToReactUIFactory.computeIfAbsent(contextPath, cp -> provider)
                                               .get();
    }

    public ReactUI putAndGet(String contextPath, ReactUIInternalProvider provider)
    {
        this.contextPathToReactUIFactory.put(contextPath, provider);
        return provider.get();
    }

    public Optional<ReactUIInternal> get(String contextPath)
    {
        return Optional.ofNullable(this.contextPathToReactUIFactory.get(contextPath))
                       .map(provider -> provider.get());
    }
}
