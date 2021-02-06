package org.omnaest.react4j.domain.data;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import org.omnaest.react4j.domain.Location;

public interface DataContext
{
    public String getId(Location location);

    public PersistResult persist(Data data);

    public static interface PersistResult extends Supplier<Data>
    {
        public boolean isSuccessful();

        /**
         * Returns the given {@link Data} object with modified meta data on success and otherwise it returns {@link Data#getInitial()}.
         */
        @Override
        public Data get();

        public PersistResult onSuccessGet(Function<PersistResult, Data> supplier);

        public PersistResult onFailureGet(BiFunction<Exception, PersistResult, Data> supplier);
    }

}
