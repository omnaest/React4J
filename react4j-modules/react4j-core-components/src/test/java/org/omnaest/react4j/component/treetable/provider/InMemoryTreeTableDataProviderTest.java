package org.omnaest.react4j.component.treetable.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.omnaest.react4j.component.treetable.provider.ColumnFilter.FilterOperator;
import org.omnaest.react4j.component.treetable.provider.InMemoryTreeTableDataProvider.Node;
import org.omnaest.react4j.component.treetable.provider.SortColumn.SortDirection;

/**
 * Provider-contract tests for {@link TreeTableDataProvider}, exercised against {@link InMemoryTreeTableDataProvider} as the
 * walking-skeleton example (plan-76 Slice 1). Algorithmic node, no mocks.
 *
 * @see InMemoryTreeTableDataProvider
 * @author omnaest
 */
public class InMemoryTreeTableDataProviderTest
{
    private static final int WINDOW_ROW_COUNT = 520;

    /**
     * Builds a small multi-level tree:
     * <ul>
     * <li>520 flat "row-XXXX" nodes at root, used for the windowing/load-more tests</li>
     * <li>"dept-eng" (root, expandable) with children alice/bob/carol, used for the child-fetch-scope test</li>
     * <li>"sort-demo" (root, expandable) with children p1..p4 carrying distinct name/score cells, used for the
     * sort/filter/total-count tests</li>
     * </ul>
     */
    private List<Node> buildTree()
    {
        List<Node> rootNodes = new ArrayList<>();
        for (int i = 0; i < WINDOW_ROW_COUNT; i++)
        {
            String id = String.format("row-%04d", i);
            rootNodes.add(new Node(id, cells("name", "Row " + i), Collections.emptyList()));
        }

        List<Node> engineeringChildren = Arrays.asList(new Node("alice", cells("name", "Alice"), Collections.emptyList()),
                                                       new Node("bob", cells("name", "Bob"), Collections.emptyList()),
                                                       new Node("carol", cells("name", "Carol"), Collections.emptyList()));
        rootNodes.add(new Node("dept-eng", cells("name", "Engineering"), engineeringChildren));

        List<Node> sortDemoChildren = Arrays.asList(new Node("p1", cells("name", "Charlie", "score", 30), Collections.emptyList()),
                                                    new Node("p2", cells("name", "Alice", "score", 10), Collections.emptyList()),
                                                    new Node("p3", cells("name", "Bob", "score", 20), Collections.emptyList()),
                                                    new Node("p4", cells("name", "Alice", "score", 5), Collections.emptyList()));
        rootNodes.add(new Node("sort-demo", cells("name", "Sort Demo"), sortDemoChildren));

        return rootNodes;
    }

    private Map<String, Object> cells(Object... keyValuePairs)
    {
        Map<String, Object> cells = new LinkedHashMap<>();
        for (int i = 0; i < keyValuePairs.length; i += 2)
        {
            cells.put((String) keyValuePairs[i], keyValuePairs[i + 1]);
        }
        return cells;
    }

    private List<String> nodeIds(TreeTablePage page)
    {
        return page.getRows()
                   .stream()
                   .map(TreeTableRow::getNodeId)
                   .collect(Collectors.toList());
    }

    /**
     * AC1 / plan section 3: root fetch returns the first 500-row window (offset 0, limit 500).
     */
    @Test
    public void testRootFetchReturnsFirst500RowWindow()
    {
        InMemoryTreeTableDataProvider provider = new InMemoryTreeTableDataProvider(this.buildTree(), false);

        TreeTablePage page = provider.fetch(TreeTableQuery.of(null, 0, 500, Collections.emptyList(), Collections.emptyList()));

        assertEquals(500, page.getRows()
                              .size());
        assertEquals("row-0000", page.getRows()
                                     .get(0)
                                     .getNodeId());
        assertEquals("row-0499", page.getRows()
                                     .get(499)
                                     .getNodeId());
    }

    /**
     * AC2 / plan section 3: load-more advances offset/limit and returns the next window correctly.
     */
    @Test
    public void testLoadMoreAdvancesOffsetAndReturnsNextWindow()
    {
        InMemoryTreeTableDataProvider provider = new InMemoryTreeTableDataProvider(this.buildTree(), false);

        TreeTablePage page = provider.fetch(TreeTableQuery.of(null, 500, 500, Collections.emptyList(), Collections.emptyList()));

        // 520 windowing rows + dept-eng + sort-demo = 522 root children; offset 500 leaves 22
        assertEquals(22, page.getRows()
                             .size());
        List<String> ids = this.nodeIds(page);
        assertEquals("row-0500", ids.get(0));
        assertEquals("row-0519", ids.get(19));
        assertTrue(ids.contains("dept-eng"));
        assertTrue(ids.contains("sort-demo"));
    }

    /**
     * AC3 / plan section 3: child fetch scoped to a parentNodeId returns only that parent's direct children.
     */
    @Test
    public void testChildFetchScopedToParentReturnsOnlyThatParentsChildren()
    {
        InMemoryTreeTableDataProvider provider = new InMemoryTreeTableDataProvider(this.buildTree(), false);

        TreeTablePage page = provider.fetch(TreeTableQuery.of("dept-eng", 0, 500, Collections.emptyList(), Collections.emptyList()));

        assertEquals(Arrays.asList("alice", "bob", "carol"), this.nodeIds(page));
    }

    /**
     * AC4 / plan section 3: multi-column sort orders by index 0 primary, then secondary.
     */
    @Test
    public void testMultiColumnSortOrdersByPrimaryThenSecondary()
    {
        InMemoryTreeTableDataProvider provider = new InMemoryTreeTableDataProvider(this.buildTree(), false);

        List<SortColumn> sorts = Arrays.asList(SortColumn.of("name", SortDirection.ASCENDING), SortColumn.of("score", SortDirection.DESCENDING));
        TreeTablePage page = provider.fetch(TreeTableQuery.of("sort-demo", 0, 500, sorts, Collections.emptyList()));

        // name asc: Alice, Alice, Bob, Charlie ; within the Alice tie, score desc: 10 before 5
        assertEquals(Arrays.asList("p2", "p4", "p3", "p1"), this.nodeIds(page));
    }

    /**
     * AC4 / plan section 3: per-column filter restricts the returned rows (EQUALS operator).
     */
    @Test
    public void testColumnFilterEqualsRestrictsReturnedRows()
    {
        InMemoryTreeTableDataProvider provider = new InMemoryTreeTableDataProvider(this.buildTree(), false);

        List<ColumnFilter> filters = Collections.singletonList(ColumnFilter.of("name", FilterOperator.EQUALS, "Alice"));
        TreeTablePage page = provider.fetch(TreeTableQuery.of("sort-demo", 0, 500, Collections.emptyList(), filters));

        assertEquals(Arrays.asList("p2", "p4"), this.nodeIds(page));
    }

    /**
     * AC4 / plan section 3: per-column filter restricts the returned rows (GREATER_THAN operator).
     */
    @Test
    public void testColumnFilterGreaterThanRestrictsReturnedRows()
    {
        InMemoryTreeTableDataProvider provider = new InMemoryTreeTableDataProvider(this.buildTree(), false);

        List<ColumnFilter> filters = Collections.singletonList(ColumnFilter.of("score", FilterOperator.GREATER_THAN, 15));
        TreeTablePage page = provider.fetch(TreeTableQuery.of("sort-demo", 0, 500, Collections.emptyList(), filters));

        assertEquals(Arrays.asList("p1", "p3"), this.nodeIds(page));
    }

    /**
     * AC1 / plan section 3: getTotalChildCount() is present when the provider computes it.
     */
    @Test
    public void testTotalChildCountPresentWhenProviderComputesIt()
    {
        InMemoryTreeTableDataProvider provider = new InMemoryTreeTableDataProvider(this.buildTree(), true);

        TreeTablePage page = provider.fetch(TreeTableQuery.of("sort-demo", 0, 500, Collections.emptyList(), Collections.emptyList()));

        assertTrue(page.getTotalChildCount()
                       .isPresent());
        assertEquals(4, page.getTotalChildCount()
                            .getAsLong());
    }

    /**
     * AC1 / plan section 3: getTotalChildCount() is empty when the provider does not compute it.
     */
    @Test
    public void testTotalChildCountEmptyWhenProviderOmitsIt()
    {
        InMemoryTreeTableDataProvider provider = new InMemoryTreeTableDataProvider(this.buildTree(), false);

        TreeTablePage page = provider.fetch(TreeTableQuery.of("sort-demo", 0, 500, Collections.emptyList(), Collections.emptyList()));

        assertFalse(page.getTotalChildCount()
                        .isPresent());
    }

    // ------------------------------------------------------------------------------------------------------------
    // plan-80 - flat-mode isFlat SPI + InMemoryTreeTableDataProvider flattened-all-nodes behavior
    // ------------------------------------------------------------------------------------------------------------

    /**
     * SPI: {@link TreeTableQuery#isFlat()} defaults to {@code false} via the original 5-arg
     * {@link TreeTableQuery#of(String, long, int, java.util.List, java.util.List)} factory (backward compatible).
     */
    @Test
    public void testTreeTableQueryIsFlatDefaultsFalse()
    {
        TreeTableQuery query = TreeTableQuery.of(null, 0, 500, Collections.emptyList(), Collections.emptyList());
        assertFalse(query.isFlat());
    }

    /**
     * SPI: the flat-constructing 6-arg {@link TreeTableQuery#of(String, long, int, java.util.List, java.util.List, boolean)}
     * overload sets {@link TreeTableQuery#isFlat()} true.
     */
    @Test
    public void testTreeTableQueryFlatConstructingOverloadSetsIsFlatTrue()
    {
        TreeTableQuery query = TreeTableQuery.of(null, 0, 500, Collections.emptyList(), Collections.emptyList(), true);
        assertTrue(query.isFlat());

        TreeTableQuery notFlatQuery = TreeTableQuery.of(null, 0, 500, Collections.emptyList(), Collections.emptyList(), false);
        assertFalse(notFlatQuery.isFlat());
    }

    /**
     * {@link InMemoryTreeTableDataProvider} honors {@code isFlat}: an {@code isFlat==true} query returns the
     * FLATTENED list of ALL nodes (every depth, folders and files), windowed/sorted/filtered exactly like an
     * ordinary sibling-group fetch - not just the root sibling group.
     */
    @Test
    public void testFlatQueryReturnsFlattenedAllNodesWindow()
    {
        InMemoryTreeTableDataProvider provider = new InMemoryTreeTableDataProvider(this.buildTree(), true);

        // 520 flat root rows + dept-eng (+3 children) + sort-demo (+4 children) = 520 + 1 + 3 + 1 + 4 = 529 total nodes.
        TreeTablePage page = provider.fetch(TreeTableQuery.of(null, 0, 1000, Collections.emptyList(), Collections.emptyList(), true));

        assertEquals(529, page.getRows()
                              .size(),
                     "a flat fetch must return EVERY node at every depth, not just the root sibling group");
        assertTrue(page.getTotalChildCount()
                       .isPresent());
        assertEquals(529L, page.getTotalChildCount()
                               .getAsLong());
        List<String> ids = this.nodeIds(page);
        assertTrue(ids.contains("alice"), "a flat fetch must include nodes nested under dept-eng");
        assertTrue(ids.contains("p1"), "a flat fetch must include nodes nested under sort-demo");
    }

    /**
     * A flat fetch honors {@code offset}/{@code limit} windowing over the flattened all-nodes list, exactly like an
     * ordinary sibling-group fetch.
     */
    @Test
    public void testFlatQueryHonorsOffsetAndLimitWindowing()
    {
        InMemoryTreeTableDataProvider provider = new InMemoryTreeTableDataProvider(this.buildTree(), true);

        TreeTablePage page = provider.fetch(TreeTableQuery.of(null, 0, 5, Collections.emptyList(), Collections.emptyList(), true));

        assertEquals(5, page.getRows()
                            .size(),
                     "a flat fetch must respect the requested window size");
        assertEquals("row-0000", page.getRows()
                                     .get(0)
                                     .getNodeId());
        assertEquals("row-0004", page.getRows()
                                     .get(4)
                                     .getNodeId());
    }

    /**
     * A flat fetch applies sort/filter across the WHOLE flattened node set (multi-depth), narrowing/ordering across
     * nodes that would otherwise live in different sibling groups.
     */
    @Test
    public void testFlatQuerySortsAndFiltersAcrossTheWholeFlattenedSet()
    {
        InMemoryTreeTableDataProvider provider = new InMemoryTreeTableDataProvider(this.buildTree(), true);

        List<ColumnFilter> filters = Collections.singletonList(ColumnFilter.of("name", FilterOperator.EQUALS, "Alice"));
        TreeTablePage page = provider.fetch(TreeTableQuery.of(null, 0, 500, Collections.emptyList(), filters, true));

        // "Alice" appears both as dept-eng's direct child AND as sort-demo's child p2/p4 (each cell "name"="Alice") -
        // a flat filter must match across depths/sibling-groups, something no single sibling-group fetch could do.
        assertEquals(Arrays.asList("alice", "p2", "p4"), this.nodeIds(page),
                     "a flat filter must match nodes across DIFFERENT sibling groups/depths, not just one parent's children");
    }

}
