package org.omnaest.react4j.data.provider.memory;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.omnaest.react4j.data.Repository;
import org.omnaest.react4j.data.Repository.AddEntry;
import org.omnaest.react4j.data.Repository.EntryOperationResult;
import org.omnaest.react4j.data.Repository.RemoveEntry;
import org.omnaest.react4j.data.provider.RepositoryProvider;
import org.omnaest.react4j.data.provider.RepositoryProvider.Tenant;

/**
 * Tests that InMemoryRepositoryProvider handles RemoveEntry by its own type and does not cast it to PutEntry.
 */
public class InMemoryRepositoryProviderTest
{
    @Test
    public void testRemoveEntryByOwnType() throws Exception
    {
        InMemoryRepositoryProvider provider = new InMemoryRepositoryProvider();

        Repository repository = provider.apply(Tenant.Id.of("tenant"))
                                         .apply(Repository.Id.of("test", "removeByOwnType"));

        // First add an entry so we have an id to remove
        EntryOperationResult added = repository.apply(AddEntry.of(Map.of("name", "value"), null));
        assertTrue(added.isSuccessful(), "add should succeed: " + added.getException());

        String id = added.getEntryId();
        assertNotNull(id, "added entry should have an id");

        // Now remove by id via RemoveEntry - this must NOT throw ClassCastException
        EntryOperationResult removed = repository.apply(RemoveEntry.of(id));
        assertTrue(removed.isSuccessful(),
                   "RemoveEntry should succeed, not throw ClassCastException. Exception: " + removed.getException());

        // Verify the entry is gone
        assertTrue(repository.stream().noneMatch(de -> de.getId().equals(id)),
                   "removed entry should no longer be present");
    }
}