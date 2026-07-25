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
package org.omnaest.react4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.omnaest.react4j.component.treetable.provider.ColumnFilter;
import org.omnaest.react4j.component.treetable.provider.ColumnFilter.FilterOperator;
import org.omnaest.react4j.component.treetable.provider.SortColumn;
import org.omnaest.react4j.component.treetable.provider.SortColumn.SortDirection;
import org.omnaest.react4j.component.treetable.provider.TreeTablePage;
import org.omnaest.react4j.component.treetable.provider.TreeTableQuery;
import org.omnaest.react4j.component.treetable.provider.TreeTableRow;

/**
 * Algorithmic-node test (zero mocks, zero Spring context - pure {@code fetch(TreeTableQuery)} input/output) for
 * {@link LargeTreeTableDataProvider}, focused on the ONE piece of genuinely new logic this class adds over the
 * already-proven {@code TreeTableDataProvider} contract (covered elsewhere by
 * {@code InMemoryTreeTableDataProviderTest}, {@code TreeTableRendererImplTest}, and the react4j-core end-to-end
 * seam tests {@code TreeTableFilterSortEndToEndTest}/{@code TreeTableLoadMoreEndToEndTest}/
 * {@code TreeTableExpandCollapseEndToEndTest}/{@code TreeTableDepth2ExpandEndToEndTest}): the global-vs-scoped
 * filter branch that lets a root-level filter reach every depth of the generated ~10,050-node tree, while an
 * expanded-node's own child-group filter stays correctly scoped to just that one parent's children.
 *
 * @see LargeTreeTableDataProvider
 * @author omnaest
 */
public class LargeTreeTableDataProviderTest
{
    private final LargeTreeTableDataProvider provider = new LargeTreeTableDataProvider();

    @Test
    public void testRootLevelFetchReturnsWindowedRootsWithCorrectTotalCount()
    {
        TreeTablePage page = this.provider.fetch(TreeTableQuery.of(null, 0L, 10, Collections.emptyList(), Collections.emptyList()));

        assertEquals(10, page.getRows()
                             .size(),
                     "the window must cap at the requested limit even though far more root nodes exist");
        assertEquals(LargeTreeTableDataProvider.ROOT_COUNT, page.getTotalChildCount()
                                                                .getAsLong(),
                     "totalChildCount must report the true root count");
    }

    @Test
    public void testGlobalFilterAtRootLevelFindsDeepNeedleMatchesAcrossWholeTree()
    {
        List<ColumnFilter> filters = List.of(ColumnFilter.of("name", FilterOperator.CONTAINS, LargeTreeTableDataProvider.NEEDLE_TOKEN));
        TreeTableQuery rootQuery = TreeTableQuery.of(null, 0L, 50, Collections.emptyList(), filters);

        TreeTablePage page = this.provider.fetch(rootQuery);

        assertEquals(LargeTreeTableDataProvider.GRANDCHILD_COUNT, page.getRows()
                                                                      .size(),
                     "a root-level (nothing-expanded) filter must reach the needle's deeply-nested grandchildren, not just root-level rows");
        assertEquals(LargeTreeTableDataProvider.GRANDCHILD_COUNT, page.getTotalChildCount()
                                                                      .getAsLong());
        for (TreeTableRow row : page.getRows())
        {
            assertTrue(row.getNodeId()
                          .startsWith("r37-c14-g"),
                       "every match must come from the deeply-nested needle subset, proving the search reached beyond the root level: " + row.getNodeId());
        }
    }

    @Test
    public void testScopedChildFetchWithFilterStaysWithinThatOneParentNotTheWholeTree()
    {
        // "05" also matches other roots' own child-05 (e.g. "Folder-01-05") if the search were global - scoping to
        // r00's own children must return ONLY Folder-00-05, proving the parentNodeId-present path never falls back
        // to the global whole-tree search the root-level (parentNodeId-empty) path uses.
        List<ColumnFilter> filters = List.of(ColumnFilter.of("name", FilterOperator.CONTAINS, "05"));
        TreeTableQuery childQuery = TreeTableQuery.of("r00", 0L, 50, Collections.emptyList(), filters);

        TreeTablePage page = this.provider.fetch(childQuery);

        assertEquals(1, page.getRows()
                            .size(),
                     "a filtered child-group fetch must stay scoped to the one requested parent's own children");
        assertEquals("r00-c05", page.getRows()
                                    .get(0)
                                    .getNodeId());
    }

    @Test
    public void testSortDescendingOrdersRootsByName()
    {
        List<SortColumn> sorts = List.of(SortColumn.of("name", SortDirection.DESCENDING));
        TreeTablePage page = this.provider.fetch(TreeTableQuery.of(null, 0L, 3, sorts, Collections.emptyList()));

        assertEquals(List.of("Folder-49", "Folder-48", "Folder-47"),
                     page.getRows()
                         .stream()
                         .map(row -> row.getCells()
                                        .get("name"))
                         .map(String::valueOf)
                         .toList());
    }

    @Test
    public void testGeneratedTreeHasApproximatelyTenThousandNodesAcrossAllDepths()
    {
        // A CONTAINS "" predicate matches every node's "kind" cell (every string contains the empty string), so a
        // root-level (global) filtered fetch's totalChildCount reveals the TRUE total node count across all
        // depths - the concrete number backing the "~10,000 hierarchical rows" scale requirement.
        List<ColumnFilter> matchAllFilter = List.of(ColumnFilter.of("kind", FilterOperator.CONTAINS, ""));
        TreeTablePage page = this.provider.fetch(TreeTableQuery.of(null, 0L, 1, Collections.emptyList(), matchAllFilter));

        long expectedTotal = LargeTreeTableDataProvider.ROOT_COUNT
                             + (long) LargeTreeTableDataProvider.ROOT_COUNT * LargeTreeTableDataProvider.CHILD_COUNT
                             + (long) LargeTreeTableDataProvider.ROOT_COUNT * LargeTreeTableDataProvider.CHILD_COUNT * LargeTreeTableDataProvider.GRANDCHILD_COUNT;

        assertEquals(expectedTotal, page.getTotalChildCount()
                                        .getAsLong());
        assertTrue(expectedTotal >= 9_000 && expectedTotal <= 11_000, "the generated tree must be in the ~10,000-node ballpark the brief calls for");
    }

    @Test
    public void testCellsIncludeAllFourColumnsWithOwnerAndModifiedVaryingAcrossSiblings()
    {
        // The grandchild files under r00-c00 are siblings -- owner/modified must NOT be constant per group, or a
        // sort on either column would never visibly reshuffle rows.
        TreeTablePage page = this.provider.fetch(TreeTableQuery.of("r00-c00", 0L, LargeTreeTableDataProvider.GRANDCHILD_COUNT, Collections.emptyList(), Collections.emptyList()));

        assertEquals(LargeTreeTableDataProvider.GRANDCHILD_COUNT, page.getRows()
                                                                      .size());

        Set<String> owners = new HashSet<>();
        Set<String> modifiedDates = new HashSet<>();
        for (TreeTableRow row : page.getRows())
        {
            assertEquals(Set.of("name", "owner", "modified", "kind"), row.getCells()
                                                                         .keySet(),
                         "every row must carry exactly the four documented cell keys");
            owners.add((String) row.getCells()
                                   .get("owner"));
            String modified = (String) row.getCells()
                                          .get("modified");
            modifiedDates.add(modified);
            assertTrue(modified.matches("\\d{4}-\\d{2}-\\d{2}"), "modified must be an ISO yyyy-MM-dd date string: " + modified);
        }
        assertTrue(owners.size() > 1, "owner must vary across siblings, not stay constant per group: " + owners);
        assertTrue(modifiedDates.size() > 1, "modified must vary across siblings, not stay constant per group: " + modifiedDates);
    }

    @Test
    public void testFlatFetchUsesFlattenedAllNodesSetAcrossEveryDepthNotJustRoots()
    {
        TreeTableQuery flatQuery = TreeTableQuery.of(null, 0L, 5, Collections.emptyList(), Collections.emptyList(), true);
        TreeTablePage page = this.provider.fetch(flatQuery);

        long expectedTotal = LargeTreeTableDataProvider.ROOT_COUNT
                             + (long) LargeTreeTableDataProvider.ROOT_COUNT * LargeTreeTableDataProvider.CHILD_COUNT
                             + (long) LargeTreeTableDataProvider.ROOT_COUNT * LargeTreeTableDataProvider.CHILD_COUNT * LargeTreeTableDataProvider.GRANDCHILD_COUNT;

        assertEquals(expectedTotal, page.getTotalChildCount()
                                        .getAsLong(),
                     "a flat fetch's candidate set must be the WHOLE flattened tree (every depth), not merely the root level");
        assertEquals(5, page.getRows()
                            .size());
    }

    @Test
    public void testFlatFetchSortsFiltersAndWindowsOverTheFlattenedSetSpanningEveryDepth()
    {
        // Ascending name sort: "File-..." sorts before "Folder-..." lexicographically ('i' < 'o'), so an
        // ascending-by-name flat fetch's first page is drawn from grandchild FILE (leaf) nodes -- proving the
        // isFlat candidate set genuinely spans every depth, not just the 50 root folders, while sort/window still
        // apply exactly like the ordinary (non-flat) pipeline.
        List<SortColumn> ascendingByName = List.of(SortColumn.of("name", SortDirection.ASCENDING));
        TreeTableQuery flatQuery = TreeTableQuery.of(null, 0L, 5, ascendingByName, Collections.emptyList(), true);

        TreeTablePage page = this.provider.fetch(flatQuery);

        assertEquals(5, page.getRows()
                            .size());
        for (TreeTableRow row : page.getRows())
        {
            assertTrue(row.getNodeId()
                          .contains("-g"),
                       "ascending name-sorted flat fetch's first page should be grandchild leaf nodes, proving the set spans every depth: " + row.getNodeId());
        }

        // The same global-filter-reachable NEEDLE token, now reached purely via isFlat=true rather than an
        // implicit root+non-empty-filter condition -- both paths must select the same flattened candidate set.
        List<ColumnFilter> needleFilter = List.of(ColumnFilter.of("name", FilterOperator.CONTAINS, LargeTreeTableDataProvider.NEEDLE_TOKEN));
        TreeTableQuery flatFilteredQuery = TreeTableQuery.of(null, 0L, 50, Collections.emptyList(), needleFilter, true);
        TreeTablePage filteredPage = this.provider.fetch(flatFilteredQuery);

        assertEquals(LargeTreeTableDataProvider.GRANDCHILD_COUNT, filteredPage.getRows()
                                                                              .size());
    }
}
