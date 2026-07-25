package org.omnaest.react4j.component.treetable.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.omnaest.react4j.component.treetable.provider.SortColumn.SortDirection;

/**
 * Value-type unit tests for {@link TreeTableColumn} (plan-78): {@link #of(String, String)} default flags, and
 * {@link TreeTableColumn#withFilterable(boolean)} / {@link TreeTableColumn#withSortable(boolean)} immutability.
 * <p>
 * plan-79 adds {@link TreeTableColumn#getInitialSortDirection()} / {@link TreeTableColumn#withInitialSortDirection(SortDirection)}
 * coverage below.
 *
 * @author omnaest
 */
public class TreeTableColumnTest
{
    @Test
    public void testOfDefaultsFilterableAndSortableToTrue()
    {
        TreeTableColumn column = TreeTableColumn.of("name", "Name");

        assertEquals("name", column.getKey());
        assertEquals("Name", column.getTitle());
        assertTrue(column.isFilterable(), "of(key,title) must default filterable to true (backward compatible)");
        assertTrue(column.isSortable(), "of(key,title) must default sortable to true (backward compatible)");
    }

    @Test
    public void testWithFilterableFalseReturnsANewInstanceWithFlagFlippedAndOthersPreserved()
    {
        TreeTableColumn original = TreeTableColumn.of("name", "Name");

        TreeTableColumn nonFilterable = original.withFilterable(false);

        assertFalse(nonFilterable.isFilterable(), "withFilterable(false) must flip the filterable flag");
        assertTrue(nonFilterable.isSortable(), "withFilterable(false) must leave sortable unchanged");
        assertEquals("name", nonFilterable.getKey(), "withFilterable(false) must leave the key unchanged");
        assertEquals("Name", nonFilterable.getTitle(), "withFilterable(false) must leave the title unchanged");

        assertTrue(original.isFilterable(), "the original instance must remain unmutated (immutable value type)");
    }

    @Test
    public void testWithSortableFalseReturnsANewInstanceWithFlagFlippedAndOthersPreserved()
    {
        TreeTableColumn original = TreeTableColumn.of("name", "Name");

        TreeTableColumn nonSortable = original.withSortable(false);

        assertFalse(nonSortable.isSortable(), "withSortable(false) must flip the sortable flag");
        assertTrue(nonSortable.isFilterable(), "withSortable(false) must leave filterable unchanged");
        assertEquals("name", nonSortable.getKey(), "withSortable(false) must leave the key unchanged");
        assertEquals("Name", nonSortable.getTitle(), "withSortable(false) must leave the title unchanged");

        assertTrue(original.isSortable(), "the original instance must remain unmutated (immutable value type)");
    }

    @Test
    public void testWithFilterableAndWithSortableCanBeCombinedIndependently()
    {
        TreeTableColumn column = TreeTableColumn.of("age", "Age")
                                                .withFilterable(false)
                                                .withSortable(false);

        assertFalse(column.isFilterable());
        assertFalse(column.isSortable());
        assertEquals("age", column.getKey());
        assertEquals("Age", column.getTitle());
    }

    @Test
    public void testOfDefaultsInitialSortDirectionToEmpty()
    {
        TreeTableColumn column = TreeTableColumn.of("name", "Name");

        assertFalse(column.getInitialSortDirection()
                          .isPresent(),
                    "of(key,title) must default getInitialSortDirection() to empty (no initial sort)");
    }

    @Test
    public void testWithInitialSortDirectionReturnsANewInstanceCarryingItWithOthersPreserved()
    {
        TreeTableColumn original = TreeTableColumn.of("name", "Name");

        TreeTableColumn withInitialSort = original.withInitialSortDirection(SortDirection.ASCENDING);

        assertEquals(SortDirection.ASCENDING, withInitialSort.getInitialSortDirection()
                                                             .get(),
                     "withInitialSortDirection(ASCENDING) must set the initial sort direction on the new instance");
        assertEquals("name", withInitialSort.getKey(), "withInitialSortDirection must leave the key unchanged");
        assertEquals("Name", withInitialSort.getTitle(), "withInitialSortDirection must leave the title unchanged");
        assertTrue(withInitialSort.isFilterable(), "withInitialSortDirection must leave filterable unchanged");
        assertTrue(withInitialSort.isSortable(), "withInitialSortDirection must leave sortable unchanged");

        assertFalse(original.getInitialSortDirection()
                            .isPresent(),
                    "the original instance must remain unmutated (immutable value type)");
    }

    @Test
    public void testWithInitialSortDirectionComposesWithWithSortableAndWithFilterablePreservingAllFlags()
    {
        TreeTableColumn column = TreeTableColumn.of("age", "Age")
                                                .withFilterable(false)
                                                .withSortable(true)
                                                .withInitialSortDirection(SortDirection.DESCENDING);

        assertEquals(SortDirection.DESCENDING, column.getInitialSortDirection()
                                                     .get());
        assertFalse(column.isFilterable(), "composing withInitialSortDirection must not disturb an earlier withFilterable(false)");
        assertTrue(column.isSortable());
        assertEquals("age", column.getKey());
        assertEquals("Age", column.getTitle());

        // order independence: withSortable/withFilterable applied AFTER withInitialSortDirection must also preserve it.
        TreeTableColumn reordered = TreeTableColumn.of("age", "Age")
                                                   .withInitialSortDirection(SortDirection.DESCENDING)
                                                   .withFilterable(false)
                                                   .withSortable(true);
        assertEquals(SortDirection.DESCENDING, reordered.getInitialSortDirection()
                                                        .get(),
                     "withFilterable/withSortable applied AFTER withInitialSortDirection must preserve the initial sort direction");
    }

    @Test
    public void testWithInitialSortDirectionNullClearsItBackToEmpty()
    {
        TreeTableColumn withInitialSort = TreeTableColumn.of("name", "Name")
                                                         .withInitialSortDirection(SortDirection.ASCENDING);

        TreeTableColumn cleared = withInitialSort.withInitialSortDirection(null);

        assertFalse(cleared.getInitialSortDirection()
                           .isPresent(),
                    "withInitialSortDirection(null) must clear the initial sort direction back to empty");
        assertTrue(withInitialSort.getInitialSortDirection()
                                  .isPresent(),
                   "the original instance must remain unmutated (immutable value type)");
    }
}
