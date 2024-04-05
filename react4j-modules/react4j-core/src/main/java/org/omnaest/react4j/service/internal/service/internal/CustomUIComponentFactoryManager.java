package org.omnaest.react4j.service.internal.service.internal;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.support.CustomUIComponentFactory;
import org.omnaest.react4j.service.internal.component.ComponentContext;
import org.omnaest.utils.element.cached.CachedElement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
public class CustomUIComponentFactoryManager
{
    @Autowired
    protected List<CustomUIComponentFactory<?>> uiComponentFactories;

    protected CachedElement<CustomUIComponentFactoryIndex> customUIComponentFactoryIndexHolder = CachedElement.of(this::newIndex);

    public <UIC extends UIComponent<UIC>> Optional<UIC> newInstance(Class<UIC> type, ComponentContext context)
    {
        return this.customUIComponentFactoryIndexHolder.get()
                                                       .getFactory(type)
                                                       .map(factory -> factory.newInstance(context));
    }

    private CustomUIComponentFactoryIndex newIndex()
    {
        return new CustomUIComponentFactoryIndex(Optional.ofNullable(this.uiComponentFactories)
                                                         .orElse(Collections.emptyList())
                                                         .stream()
                                                         .collect(Collectors.toMap(f -> f.getType(), Function.identity())));
    }

    @RequiredArgsConstructor
    protected static class CustomUIComponentFactoryIndex
    {
        private final Map<Class<?>, CustomUIComponentFactory<?>> typeToCustomUIComponentFactory;

        @SuppressWarnings("unchecked")
        public <UIC extends UIComponent<UIC>> Optional<CustomUIComponentFactory<UIC>> getFactory(Class<UIC> type)
        {
            return Optional.ofNullable((CustomUIComponentFactory<UIC>) this.typeToCustomUIComponentFactory.get(type));
        }
    }
}
