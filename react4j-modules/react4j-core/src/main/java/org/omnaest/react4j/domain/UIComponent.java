package org.omnaest.react4j.domain;

import java.util.function.BiConsumer;

import org.omnaest.react4j.domain.data.DefineableDataContext;
import org.omnaest.react4j.domain.data.TypedDataContext;
import org.omnaest.react4j.domain.raw.UIComponentRenderer;

public interface UIComponent<UIC extends UIComponent<?>>
{
    public String getId();

    public Locations getLocations();

    /**
     * @see DefineableDataContext
     * @param dataContextConsumer
     * @return
     */
    public UIC withDataContext(BiConsumer<UIC, DefineableDataContext> dataContextConsumer);

    public <T> UIC withDataContext(Class<T> type, BiConsumer<UIC, TypedDataContext<T>> dataContextConsumer);

    public UIComponentRenderer asRenderer();

    public UIC registerParent(UIComponent<?> parent);

}