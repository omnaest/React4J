/*******************************************************************************
 * Copyright 2021 Danny Kunz
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
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
                       .map(ReactUIInternalProvider::get);
    }
}
