package org.omnaest.react4j.data;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.omnaest.react4j.data.Repository.DataEntry;
import org.omnaest.react4j.data.Repository.Entry;
import org.omnaest.react4j.data.Repository.EntryOperationResult;
import org.omnaest.react4j.data.Repository.Entry.Type;

public interface Repository extends Iterable<DataEntry>, Function<Entry, EntryOperationResult>
{
    /**
     * Applies an {@link PutEntry}, {@link AddEntry} or {@link RemoveEntry} to the {@link Repository}
     * 
     * @see AddEntry
     * @see PutEntry
     * @see RemoveEntry
     */
    @Override
    public EntryOperationResult apply(Entry entry);

    /**
     * Applies a {@link Stream} of {@link Entry}s to the {@link Repository}
     * 
     * @see #apply(Entry)
     * @see AddEntry
     * @see PutEntry
     * @see RemoveEntry
     * @param entries
     * @return
     */
    public Stream<EntryOperationResult> apply(Stream<Entry> entries);

    @Override
    public default Iterator<DataEntry> iterator()
    {
        return this.stream()
                   .iterator();
    }

    public Stream<DataEntry> stream();

    /**
     * This is a general interface, please use the {@link PutEntry}, {@link AddEntry} or {@link RemoveEntry} interfaces.
     * 
     * @see PutEntry#of(String, Map)
     * @see AddEntry#of(Map)
     * @see RemoveEntry#of(String)
     * @author omnaest
     */
    public static interface Entry
    {
        public Type getType();

        public String getId();

        public static enum Type
        {
            CREATE, UPDATE, DELETE
        }
    }

    public static interface DataEntry extends Entry
    {

        public Map<String, Object> asDataMap();

        public Map<String, Object> asMetaMap();

        public static DataEntry of(String id, Map<String, Object> dataMap, Map<String, Object> metaMap)
        {
            return new DataEntry()
            {
                @Override
                public String getId()
                {
                    return id;
                }

                @Override
                public Map<String, Object> asMetaMap()
                {
                    return metaMap;
                }

                @Override
                public Map<String, Object> asDataMap()
                {
                    return dataMap;
                }

                @Override
                public Type getType()
                {
                    return null;
                }
            };
        }
    }

    public static interface PutEntry extends DataEntry
    {

        public static PutEntry of(String id, Map<String, ? extends Object> dataMap, Map<String, ? extends Object> metaMap)
        {
            return new PutEntry()
            {
                @Override
                public String getId()
                {
                    return id;
                }

                @SuppressWarnings("unchecked")
                @Override
                public Map<String, Object> asDataMap()
                {
                    return (Map<String, Object>) dataMap;
                }

                @SuppressWarnings("unchecked")
                @Override
                public Map<String, Object> asMetaMap()
                {
                    return (Map<String, Object>) metaMap;
                }

                @Override
                public Type getType()
                {
                    return Type.UPDATE;
                }

            };
        }
    }

    public static interface AddEntry extends DataEntry
    {
        public static AddEntry of(Map<String, ? extends Object> dataMap, Map<String, ? extends Object> metaMap)
        {
            return new AddEntry()
            {
                @SuppressWarnings("unchecked")
                @Override
                public Map<String, Object> asDataMap()
                {
                    return (Map<String, Object>) dataMap;
                }

                @SuppressWarnings("unchecked")
                @Override
                public Map<String, Object> asMetaMap()
                {
                    return (Map<String, Object>) metaMap;
                }

                @Override
                public String getId()
                {
                    return null;
                }

                @Override
                public Type getType()
                {
                    return Type.CREATE;
                }
            };
        }
    }

    public static interface RemoveEntry extends Entry
    {
        @Override
        public String getId();

        public static RemoveEntry of(String id)
        {
            return new RemoveEntry()
            {
                @Override
                public String getId()
                {
                    return id;
                }

                @Override
                public Type getType()
                {
                    return Type.DELETE;
                }
            };
        }
    }

    public static interface EntryOperationResult
    {
        public String getEntryId();

        public Entry getProvidedEntry();

        public default boolean isOfType(Type type)
        {
            return Objects.equals(this.getProvidedEntry()
                                      .getType(),
                                  type);
        }

        public boolean isSuccessful();

        public default boolean hasException()
        {
            return this.getException() != null;
        }

        public Exception getException();

        public default EntryOperationResult onSuccess(Consumer<EntryOperationResult> action)
        {
            if (this.isSuccessful())
            {
                action.accept(this);
            }
            return this;
        }

        public default EntryOperationResult onFailure(BiConsumer<EntryOperationResult, Exception> action)
        {
            if (!this.isSuccessful())
            {
                action.accept(this, this.getException());
            }
            return this;
        }

        public static EntryOperationResult failure(Exception exception, Entry entry)
        {
            return new EntryOperationResult()
            {
                @Override
                public boolean isSuccessful()
                {
                    return false;
                }

                @Override
                public Exception getException()
                {
                    return exception;
                }

                @Override
                public Entry getProvidedEntry()
                {
                    return entry;
                }

                @Override
                public String getEntryId()
                {
                    return this.getProvidedEntry()
                               .getId();
                }
            };
        }

        public static EntryOperationResult success(String entryId, Entry entry)
        {
            return new EntryOperationResult()
            {
                @Override
                public boolean isSuccessful()
                {
                    return true;
                }

                @Override
                public Exception getException()
                {
                    return null;
                }

                @Override
                public Entry getProvidedEntry()
                {
                    return entry;
                }

                @Override
                public String getEntryId()
                {
                    return entryId;
                }
            };
        }

        public static EntryOperationResult success(Entry entry)
        {
            return success(entry.getId(), entry);
        }

    }

    /**
     * @see #of(String...)
     * @author omnaest
     */
    public static interface Id extends Supplier<List<String>>
    {
        public static Id of(String... tokens)
        {
            return of(Arrays.asList(tokens));
        }

        public static Id of(List<String> tokens)
        {
            return () -> tokens;
        }
    }
}
