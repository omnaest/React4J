/*******************************************************************************
 * Copyright 2021 Danny Kunz
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package org.omnaest.react4j.service.internal.service.internal.context;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.omnaest.react4j.data.Repository;
import org.omnaest.react4j.data.Repository.AddEntry;
import org.omnaest.react4j.data.Repository.Entry;
import org.omnaest.react4j.data.Repository.EntryOperationResult;
import org.omnaest.react4j.data.Repository.Id;
import org.omnaest.react4j.data.Repository.PutEntry;
import org.omnaest.react4j.data.provider.RepositoryProvider;
import org.omnaest.react4j.data.provider.RepositoryProvider.Tenant;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.Locations;
import org.omnaest.react4j.domain.context.Context;
import org.omnaest.react4j.domain.context.data.Data;
import org.omnaest.react4j.domain.context.data.DataContext;
import org.omnaest.react4j.domain.context.data.DefineableDataContext;
import org.omnaest.react4j.domain.context.data.TypedDataContext;
import org.omnaest.react4j.domain.context.document.Document;
import org.omnaest.utils.EncoderUtils.TextEncoderAndDecoderFactory;
import org.omnaest.utils.MapUtils;
import org.omnaest.utils.StringUtils;
import org.omnaest.utils.StringUtils.StringEncoderAndDecoder;
import org.omnaest.utils.element.cached.CachedElement;

public class DataContextImpl<T> implements DefineableDataContext, TypedDataContext<T>
{
    private Function<Location, String> idFunction = location -> this.encodeLocation(location);
    private Locations                  locations;

    private RepositoryProvider repositoryProvider;

    private StringEncoderAndDecoder encoderAndDecoder = StringUtils.encoder()
                                                                   .with(TextEncoderAndDecoderFactory::forAlphaNumericText);

    public DataContextImpl(Locations locations, RepositoryProvider repositoryProvider)
    {
        this.locations = locations;
        this.repositoryProvider = repositoryProvider;
    }

    @Override
    public String getId(Location location)
    {
        return this.idFunction.apply(location);
    }

    @Override
    public DefineableDataContext enableSingleton()
    {
        CachedElement<String> firstId = CachedElement.of(() -> null);
        this.idFunction = location -> firstId.setSuppliedValueLazy(this.encodeLocation(location))
                                             .get();
        return this;
    }

    private String encodeLocation(Location location)
    {

        return this.encoderAndDecoder.encodeList(this.locations.findParentOf(location)
                                                               .get(),
                                                 ".");
    }

    private Location decodeContextId(String contextId)
    {
        return Location.of(this.encoderAndDecoder.decodeList(contextId, "."));
    }

    @Override
    public DefineableDataContext withId(String id)
    {
        this.idFunction = location -> id;
        return this;
    }

    private Id determineRepositoryId(Data data)
    {
        String contextId = data.getContextId();
        return Repository.Id.of(this.decodeContextId(contextId)
                                    .get());
    }

    @Override
    public PersistResult persist(Data data)
    {
        //TODO determine tenant id
        Repository repository = this.repositoryProvider.apply(Tenant.Id.empty())
                                                       .apply(this.determineRepositoryId(data));
        Map<String, Object> dataMap = data.toMap();
        Map<String, Object> metaMap = MapUtils.builder()
                                              .build();
        Entry entry = data.getId()
                          .map(id -> (Entry) PutEntry.of(id, dataMap, metaMap))
                          .orElseGet(() -> AddEntry.of(dataMap, metaMap));
        EntryOperationResult result = repository.apply(entry);
        Data resultData = data.withId(result.getEntryId());

        return new PersistResult()
        {
            private Supplier<Data> dataSuccessSupplier = () -> resultData;
            private Supplier<Data> dataFailureSupplier = () -> resultData.getInitial();

            @Override
            public PersistResult onSuccessGet(Function<PersistResult, Data> supplier)
            {
                if (result.isSuccessful())
                {
                    this.dataSuccessSupplier = () -> supplier.apply(this);
                }
                return this;
            }

            @Override
            public PersistResult onFailureGet(BiFunction<Exception, PersistResult, Data> supplier)
            {
                if (!result.isSuccessful())
                {
                    this.dataFailureSupplier = () -> supplier.apply(result.getException(), this);
                }
                return this;
            }

            @Override
            public boolean isSuccessful()
            {
                return result.isSuccessful();
            }

            @Override
            public Data get()
            {
                if (result.isSuccessful())
                {
                    return this.dataSuccessSupplier.get();
                }
                else
                {
                    return this.dataFailureSupplier.get();
                }
            }
        };
    }

    @SuppressWarnings("unchecked")
    @Override
    public <TN> TypedDataContext<TN> asTypedDataContext(Class<TN> elementType)
    {
        return (TypedDataContext<TN>) this;
    }

    @Override
    public Selector selector()
    {
        return new Selector()
        {
            private AtomicReference<String> selectedId                 = new AtomicReference<>();
            private Supplier<DataContext>   currentDataContextProvider = () -> DataContextImpl.this; // TODO this has to resolve the data context in the threadcontext also

            @Override
            public Selector selectEntryById(String id)
            {
                this.selectedId.set(id);
                return this;
            }

            @Override
            public Selection getCurrentSelection()
            {
                return new Selection()
                {
                    @Override
                    public DataContextDocument get(int index)
                    {
                        return new DataContextDocumentImpl(currentDataContextProvider);
                    }

                    @Override
                    public int size()
                    {
                        // TODO Auto-generated method stub
                        return 1;
                    }
                };
            }
        };
    }

    @Override
    public PersistResult persist(String id, T dataElement)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Optional<T> findById(String id)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public View view()
    {
        return new View()
        {
            @Override
            public Stream<Document> stream()
            {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public int size()
            {
                // TODO Auto-generated method stub
                return 0;
            }

            @Override
            public DataContextDocument get(int index)
            {
                // TODO Auto-generated method stub
                return null;
            }
        };
    }

    @Override
    public Optional<DataContext> asDataContext()
    {
        return Optional.of(this);
    }

    private static class DataContextDocumentImpl implements DataContextDocument
    {
        private Supplier<DataContext> dataContextProvider;

        public DataContextDocumentImpl(Supplier<DataContext> dataContextProvider)
        {
            super();
            this.dataContextProvider = dataContextProvider;
        }

        @Override
        public Context getContext()
        {
            return this.dataContextProvider.get();
        }

        @Override
        public DataContextField getField(String fieldName)
        {
            return new DataContextField()
            {

                @Override
                public String getFieldName()
                {
                    return fieldName;
                }

                @Override
                public Document getDocument()
                {
                    return DataContextDocumentImpl.this;
                }

                @Override
                public Context getContext()
                {
                    return this.getDocument()
                               .getContext();
                }

            };
        }
    }

}
