package org.omnaest.react4j.domain;

import java.util.function.BiConsumer;

import org.omnaest.react4j.domain.context.data.DefineableDataContext;
import org.omnaest.react4j.domain.context.data.TypedDataContext;
import org.omnaest.react4j.domain.context.ui.UIContext;

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

    public UIC withUIContext(BiConsumer<UIC, UIContext> uiContextConsumer);

    public <T> UIC withDataContext(Class<T> type, BiConsumer<UIC, TypedDataContext<T>> dataContextConsumer);

    public UIC registerParent(UIComponent<?> parent);

}