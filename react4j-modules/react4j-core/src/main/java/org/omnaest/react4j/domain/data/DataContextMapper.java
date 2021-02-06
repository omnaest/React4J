package org.omnaest.react4j.domain.data;

import java.util.function.Function;

import org.omnaest.react4j.domain.data.collection.CollectionDataContext;
import org.omnaest.react4j.domain.data.collection.TypedCollectionDataContext;

public interface DataContextMapper<DC extends DataContext> extends Function<DefineableDataContext, DataContext>
{

    public static interface UntypedCollectionDataContextMapper extends DataContextMapper<CollectionDataContext>
    {
    }

    public static interface TypedCollectionDataContextMapper<T> extends DataContextMapper<TypedCollectionDataContext<T>>
    {
    }
}