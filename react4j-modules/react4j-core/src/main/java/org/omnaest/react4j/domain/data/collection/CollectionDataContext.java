package org.omnaest.react4j.domain.data.collection;

import java.util.Map;

import org.omnaest.react4j.domain.data.DataContext;

public interface CollectionDataContext extends DataContext
{
    public PersistResult persist(String id, Map<String, Object> map, Map<String, Object> meta);
}
