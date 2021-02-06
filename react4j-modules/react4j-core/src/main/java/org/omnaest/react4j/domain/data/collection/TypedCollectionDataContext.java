package org.omnaest.react4j.domain.data.collection;

import java.util.Map;

import org.omnaest.react4j.domain.data.DataContext;

public interface TypedCollectionDataContext<T> extends DataContext
{
    public PersistResult persist(String id, T dataElement, Map<String, Object> meta);
}
