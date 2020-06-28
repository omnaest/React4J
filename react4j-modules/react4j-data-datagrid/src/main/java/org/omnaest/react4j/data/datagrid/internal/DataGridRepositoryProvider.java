package org.omnaest.react4j.data.datagrid.internal;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.lang3.math.NumberUtils;
import org.omnaest.datagrid.DataGridUtils;
import org.omnaest.datagrid.DataGridUtils.DataGrid;
import org.omnaest.react4j.data.Repository;
import org.omnaest.react4j.data.RepositoryProvider;
import org.omnaest.utils.repository.IndexElementRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DataGridRepositoryProvider implements RepositoryProvider
{
    private static Logger LOG = LoggerFactory.getLogger(DataGridRepositoryProvider.class);

    private DataGrid dataGrid;

    @PostConstruct
    public void init()
    {
        this.dataGrid = DataGridUtils.newLocalInstance();
    }

    @Override
    public Tenant apply(Tenant.Id tenantId)
    {
        return new Tenant()
        {
            @Override
            public Repository apply(Repository.Id repositoryId)
            {
                IndexElementRepository<Map<String, Object>> elementRepository = DataGridRepositoryProvider.this.dataGrid.<Map<String, Object>>newIndexRepository(repositoryId.get(),
                                                                                                                                                                 Map.class,
                                                                                                                                                                 String.class,
                                                                                                                                                                 Object.class);

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

    @PreDestroy
    public void destroy()
    {
        try
        {
            this.dataGrid.close();
        }
        catch (Exception e)
        {
            LOG.error("Failed to gracefully shutdown datagrid", e);
        }
    }
}
