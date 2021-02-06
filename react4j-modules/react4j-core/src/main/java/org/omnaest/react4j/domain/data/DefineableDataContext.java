package org.omnaest.react4j.domain.data;

import org.omnaest.react4j.domain.data.collection.CollectionDataContext;
import org.omnaest.react4j.domain.data.collection.TypedCollectionDataContext;

public interface DefineableDataContext extends DataContext
{
    public SingletonDataContext enableSingleton();

    public CollectionDataContext asUntypedCollection();

    public <T> TypedCollectionDataContext<T> asTypedCollection(Class<T> elementType);

}
