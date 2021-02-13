package org.omnaest.react4j.domain.data;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import org.omnaest.react4j.domain.Location;

/**
 * A {@link DataContext} represents a collection within the persistence and contains a list of entries.<br>
 * <br>
 * The {@link DataContext} can create a {@link Selector} instance, which does focus on a single entry in the collection.
 * 
 * @author omnaest
 */
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

    public Selector selector();

    /**
     * A {@link Selection} of a single entry within a {@link DataContext}
     * 
     * @author omnaest
     */
    public static interface Selector
    {
        public Selector selectEntryById(String id);

        /**
         * Returns the current {@link Selection}
         * 
         * @return
         */
        public Selection getCurrentSelection();
    }

    public static interface DataContextEntry
    {
        public Field getField(String fieldName);
    }

    public static interface Selection extends DataContextEntry
    {
    }

    public static interface Field
    {
        public DataContext getDataContext();

        public String getFieldName();
    }

    /**
     * Returns a {@link TypedDataContext} wrapping the current {@link DataContext}
     * 
     * @param type
     * @return
     */
    public <T> TypedDataContext<T> asTypedDataContext(Class<T> type);
}
