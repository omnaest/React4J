package org.omnaest.react4j.domain.context.data;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import org.omnaest.react4j.domain.context.Context;
import org.omnaest.react4j.domain.context.document.Document;
import org.omnaest.react4j.domain.context.document.DocumentList;

/**
 * A {@link DataContext} represents a collection within the persistence and contains a list of entries.<br>
 * <br>
 * The {@link DataContext} can create a {@link Selector} instance, which does focus on a single entry in the collection.
 * 
 * @author omnaest
 */
public interface DataContext extends Context
{

    public PersistResult persist(Data data);

    public Selector selector();

    public View view();

    /**
     * Returns a {@link TypedDataContext} wrapping the current {@link DataContext}
     * 
     * @param type
     * @return
     */
    public <T> TypedDataContext<T> asTypedDataContext(Class<T> type);

    public static interface View extends DataContextDocumentList
    {
    }

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

    public static interface DataContextDocument extends Document
    {
        @Override
        public DataContextField getField(String fieldName);
    }

    public static interface DataContextDocumentList extends DocumentList
    {
        @Override
        public DataContextDocument get(int index);
    }

    public static interface Selection extends DataContextDocumentList
    {
    }

    public static interface DataContextField extends Document.Field
    {
    }
}
