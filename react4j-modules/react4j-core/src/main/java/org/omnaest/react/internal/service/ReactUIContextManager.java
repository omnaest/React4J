package org.omnaest.react.internal.service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.omnaest.react.domain.ReactUI;
import org.omnaest.react.internal.domain.ReactUIInternal;

public class ReactUIContextManager
{
    private Map<String, ReactUIInternal> contextPathToReactUI = new ConcurrentHashMap<>();

    public ReactUI computeIfAbsent(String contextPath, Supplier<ReactUIInternal> factory)
    {
        return this.contextPathToReactUI.computeIfAbsent(contextPath, cp -> factory.get());
    }

    public ReactUI putAndGet(String contextPath, Supplier<ReactUIInternal> factory)
    {
        ReactUIInternal reactUI = factory.get();
        this.contextPathToReactUI.put(contextPath, reactUI);
        return reactUI;
    }

    public Optional<ReactUIInternal> get(String contextPath)
    {
        return Optional.ofNullable(this.contextPathToReactUI.get(contextPath));
    }
}
