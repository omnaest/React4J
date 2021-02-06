package org.omnaest.react4j.domain;

import java.util.function.BiConsumer;

import org.omnaest.react4j.domain.data.DataContext;
import org.omnaest.react4j.domain.data.DataContextMapper;
import org.omnaest.react4j.domain.data.DataContextType;
import org.omnaest.react4j.domain.raw.UIComponentRenderer;

public interface UIComponent<UIC extends UIComponent<?>>
{
    public String getId();

    public Locations getLocations();

    /**
     * @see DataContextType
     * @see DataContextType#COLLECTION
     * @see DataContextType#DOCUMENT
     * @param dataContextType
     * @param dataContextConsumer
     * @return
     */
    public <DC extends DataContext> UIC withDataContext(DataContextMapper<DC> dataContextType, BiConsumer<UIC, DC> dataContextConsumer);

    public UIComponentRenderer asRenderer();

    public UIC registerParent(UIComponent<?> parent);

}