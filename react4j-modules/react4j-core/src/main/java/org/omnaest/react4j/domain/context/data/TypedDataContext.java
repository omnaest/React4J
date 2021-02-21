package org.omnaest.react4j.domain.context.data;

import java.util.Optional;

public interface TypedDataContext<T> extends DataContext
{
    public PersistResult persist(String id, T dataElement);

    public Optional<T> findById(String id);
}
