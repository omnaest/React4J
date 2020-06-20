package org.omnaest.react.domain;

import java.util.function.BiConsumer;

import org.omnaest.react.domain.data.DataContext;
import org.omnaest.react.domain.raw.UIComponentRenderer;

public interface UIComponent<UIC extends UIComponent<?>>
{
    public String getId();

    public Locations getLocations();

    public UIC withDataContext(BiConsumer<UIC, DataContext> dataContextConsumer);

    public UIComponentRenderer asRenderer();

    public UIC registerParent(UIComponent<?> parent);

}