package org.omnaest.react4j.service.internal.handler.domain;

import org.omnaest.react4j.domain.data.Data;

@FunctionalInterface
public interface DataEventHandler
{
    public Data invoke(Data data);
}