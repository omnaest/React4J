package org.omnaest.react.internal.handler.domain;

import org.omnaest.react.domain.data.Data;

@FunctionalInterface
public interface DataEventHandler
{
    public Data invoke(Data data);
}
