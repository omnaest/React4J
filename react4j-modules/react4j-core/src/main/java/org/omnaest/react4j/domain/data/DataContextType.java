package org.omnaest.react4j.domain.data;

import org.omnaest.react4j.domain.data.collection.CollectionDataContext;
import org.omnaest.react4j.domain.data.collection.TypedCollectionDataContext;

public enum DataContextType implements DataContextMapper<DataContext>
{
    DOCUMENT, COLLECTION;

    private DataContextType()
    {
    }

    public UntypedCollectionDataContextMapper untyped()
    {
        return new UntypedCollectionDataContextMapper()
        {
            @Override
            public CollectionDataContext apply(DefineableDataContext dataContext)
            {
                return dataContext.asUntypedCollection();
            }
        };
    }

    public <T> TypedCollectionDataContextMapper<T> typed(Class<T> elementType)
    {
        return new TypedCollectionDataContextMapper<T>()
        {
            @Override
            public TypedCollectionDataContext<T> apply(DefineableDataContext dataContext)
            {
                return dataContext.asTypedCollection(elementType);
            }
        };
    }

    @Override
    public DataContext apply(DefineableDataContext dataContext)
    {
        return this.untyped()
                   .apply(dataContext);
    }

}
