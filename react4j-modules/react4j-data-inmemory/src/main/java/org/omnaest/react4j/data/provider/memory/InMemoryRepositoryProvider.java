package org.omnaest.react4j.data.provider.memory;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.commons.lang3.math.NumberUtils;
import org.omnaest.react4j.data.Repository;
import org.omnaest.react4j.data.provider.RepositoryProvider;
import org.omnaest.utils.functional.Provider;
import org.omnaest.utils.repository.ElementRepositoryUtils;
import org.omnaest.utils.repository.IndexElementRepository;
import org.springframework.stereotype.Service;

@Service
public class InMemoryRepositoryProvider implements RepositoryProvider
{
    private Provider<IndexElementRepository<Map<String, Object>>> provider = () -> ElementRepositoryUtils.newConcurrentHashMapIndexElementRepository();

    @Override
    public Tenant apply(Tenant.Id tenantId)
    {
        return new Tenant()
        {
            @Override
            public Repository apply(Repository.Id repositoryId)
            {
                IndexElementRepository<Map<String, Object>> elementRepository = InMemoryRepositoryProvider.this.provider.get();

                return new Repository()
                {
                    @Override
                    public Stream<DataEntry> stream()
                    {
                        //TODO metamap is missing
                        return elementRepository.stream()
                                                .map(bi -> DataEntry.of("" + bi.getFirst(), bi.getSecond(), null));
                    }

                    @Override
                    public Stream<EntryOperationResult> apply(Stream<Entry> entries)
                    {
                        return Optional.ofNullable(entries)
                                       .map(stream -> stream.map(entry -> this.apply(entry)))
                                       .orElse(Stream.empty());
                    }

                    @Override
                    public EntryOperationResult apply(Entry entry)
                    {
                        if (entry instanceof PutEntry)
                        {
                            try
                            {
                                long id = NumberUtils.toLong(((PutEntry) entry).getId());
                                elementRepository.put(id, ((DataEntry) entry).asDataMap());
                                return EntryOperationResult.success(entry);
                            }
                            catch (Exception e)
                            {
                                return EntryOperationResult.failure(e, entry);
                            }
                        }
                        else if (entry instanceof AddEntry)
                        {
                            try
                            {
                                Long id = elementRepository.add(((DataEntry) entry).asDataMap());
                                return EntryOperationResult.success("" + id, entry);
                            }
                            catch (Exception e)
                            {
                                return EntryOperationResult.failure(e, entry);
                            }
                        }
                        else if (entry instanceof RemoveEntry)
                        {
                            try
                            {
                                long id = NumberUtils.toLong(((PutEntry) entry).getId());
                                elementRepository.remove(id);
                                return EntryOperationResult.success("" + id, entry);
                            }
                            catch (Exception e)
                            {
                                return EntryOperationResult.failure(e, entry);
                            }
                        }
                        else
                        {
                            throw new IllegalArgumentException("Entry must be one of AddEntry, PutEntry or RemoveEntry: " + entry);
                        }
                    }
                };
            }
        };
    }

}
