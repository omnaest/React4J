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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.stream.Collectors;

import org.omnaest.react4j.component.treetable.provider.ColumnFilter;
import org.omnaest.react4j.component.treetable.provider.ColumnFilter.FilterOperator;
import org.omnaest.react4j.component.treetable.provider.SortColumn;
import org.omnaest.react4j.component.treetable.provider.SortColumn.SortDirection;
import org.omnaest.react4j.component.treetable.provider.TreeTableDataProvider;
import org.omnaest.react4j.component.treetable.provider.TreeTablePage;
import org.omnaest.react4j.component.treetable.provider.TreeTableQuery;
import org.omnaest.react4j.component.treetable.provider.TreeTableRow;

/**
 * Large in-memory {@link TreeTableDataProvider} backing the standalone full-window TreeTable showcase
 * ({@link TreeTableFullWindowUI}) with a generated, deterministic, 3-level-deep hierarchy of
 * {@value #ROOT_COUNT} root folders x {@value #CHILD_COUNT} child folders x {@value #GRANDCHILD_COUNT} grandchild
 * files = 10,050 nodes total, so every interaction the {@code TreeTable} component supports is exercisable at
 * realistic scale: root-level load-more ({@value #ROOT_COUNT} roots against {@link TreeTableFullWindowUI}'s
 * configured window), depth-2 expand + its own load-more ({@value #CHILD_COUNT} children per root), depth-3 leaf
 * listing ({@value #GRANDCHILD_COUNT} grandchildren per child), multi-column sort, and - the scale-defining case -
 * a per-column filter that must find matches ANYWHERE in the full 10,050-node tree, not merely the level currently
 * on screen.
 * <p>
 * <b>Global (tree-wide) filter semantics - a deliberate choice, since {@link TreeTableDataProvider}'s javadoc makes
 * tree-filter semantics the provider's own responsibility:</b> {@code TreeTableRendererImpl} always issues a
 * root-scoped query ({@code parentNodeId == null}) for the visible root group, and re-issues the SAME filters for
 * every expanded row's own child-group query ({@code parentNodeId == that row's id}) - see
 * {@code TreeTableGridComponent.appendRows}/{@code buildNode}. This provider therefore distinguishes exactly those
 * two shapes: when a query carries a non-empty filter AND an EMPTY {@code parentNodeId} (root level, nothing
 * expanded yet - the shape a freshly-applied filter always starts from), it ignores normal root-only scoping and
 * searches the FLATTENED WHOLE TREE (all 10,050 nodes, any depth) - "flatten matches", not "keep-ancestor-paths":
 * a matched node is returned as a top-level result row in its own right, windowed by the query's offset/limit like
 * any other page. If that matched row itself has children, they stay independently reachable - expanding it issues
 * a query with a non-empty {@code parentNodeId}, which this provider always resolves as an ordinary single-level
 * fetch scoped to that one parent's direct children (optionally filtered further within that small group), exactly
 * matching {@link ShowcaseTreeTableDataProvider}'s existing, smaller-scale behavior. This keeps the two fetch
 * shapes' semantics simple and non-overlapping: "no parent id" always means "search everything relevant to this
 * query" (the whole tree when filtered, the root siblings when not); "a parent id" always means "list exactly this
 * node's own children".
 * <p>
 * A deterministic, deeply-nested "needle" subset - the {@value #GRANDCHILD_COUNT} grandchild files under root index
 * {@value #NEEDLE_ROOT_INDEX} / child index {@value #NEEDLE_CHILD_INDEX} - carries the literal token
 * {@value #NEEDLE_TOKEN} in their name, so a CONTAINS filter for that token demonstrably returns matches pulled
 * from deep inside the tree rather than only ones visible at the root level.
 * <p>
 * <b>Flat-mode support (plan-80):</b> {@link TreeTableQuery#isFlat()} selects the SAME {@link #allNodesFlattened}
 * candidate set the global-filter branch above already uses (unified into one {@code useFlattenedSet} condition -
 * {@code isFlat() OR globalFilterMode}), then runs the ordinary filter -&gt; sort -&gt; window pipeline over it - a flat
 * fetch is simply "every node at every depth", filtered/sorted/windowed exactly like any other candidate set.
 * <p>
 * <b>Four cell columns (plan-80):</b> each generated {@link Node} carries {@code name}/{@code owner}/
 * {@code modified}/{@code kind}. {@code owner} cycles the fixed {@link #OWNERS} name list and {@code modified} is an
 * ISO {@code yyyy-MM-dd} string offset from {@link #MODIFIED_BASE_DATE} - both keyed off a deterministic per-node
 * index derived from the node's own {@code (i, j, k)} tree-generation indices (see {@link #childIndex(int, int)}/
 * {@link #grandchildIndex(int, int, int)}), never a clock, so the demo stays reproducible while still varying across
 * siblings (a sort on either column visibly reshuffles rows).
 *
 * @author omnaest
 */
class LargeTreeTableDataProvider implements TreeTableDataProvider
{
    static final int                ROOT_COUNT         = 50;
    static final int                CHILD_COUNT        = 20;
    static final int                GRANDCHILD_COUNT   = 9;
    static final String             NEEDLE_TOKEN       = "NEEDLE";
    private static final int        NEEDLE_ROOT_INDEX  = 37;
    private static final int        NEEDLE_CHILD_INDEX = 14;

    /**
     * Fixed owner-name cycle (plan-80 4-column demo enrichment). Deliberately deterministic - NO clock / no
     * {@code Date.now()} - so the demo is reproducible: each node's owner is derived purely from its position in the
     * generated tree (see {@link #childIndex(int, int)}/{@link #grandchildIndex(int, int, int)}), never from
     * wall-clock time.
     */
    private static final String[]   OWNERS             = {"Alice", "Bob", "Carol", "Dave", "Eve", "Frank", "Grace", "Heidi"};

    /**
     * Base date the deterministic {@code modified} column offsets from (plan-80). ISO {@code yyyy-MM-dd} strings
     * sort correctly lexicographically, which is why this format was chosen for a sort-inspectable column.
     */
    private static final LocalDate  MODIFIED_BASE_DATE = LocalDate.of(2026, 1, 1);

    private final List<Node>        rootNodes;
    private final List<Node>        allNodesFlattened  = new ArrayList<>();
    private final Map<String, Node> nodesById          = new HashMap<>();

    LargeTreeTableDataProvider()
    {
        this.rootNodes = this.buildTree();
        this.index(this.rootNodes);
    }

    @Override
    public TreeTablePage fetch(TreeTableQuery query)
    {
        List<ColumnFilter> filters = query.getFilters();
        boolean globalFilterMode = query.getParentNodeId()
                                        .isEmpty()
                                   && filters != null && !filters.isEmpty();
        boolean useFlattenedSet = query.isFlat() || globalFilterMode;

        List<Node> candidateGroup = useFlattenedSet ? this.allNodesFlattened
                : query.getParentNodeId()
                       .map(this::resolveChildrenOf)
                       .orElse(this.rootNodes);

        List<Node> filtered = this.applyFilters(candidateGroup, filters);
        List<Node> sorted = this.applySort(filtered, query.getSorts());

        long offset = query.getOffset();
        int limit = query.getLimit();
        List<TreeTableRow> windowRows = sorted.stream()
                                              .skip(offset)
                                              .limit(limit)
                                              .map(node -> TreeTableRow.of(node.id, node.cells(), !node.children.isEmpty()))
                                              .collect(Collectors.toList());

        return TreeTablePage.of(windowRows, OptionalLong.of(sorted.size()));
    }

    private void index(List<Node> nodes)
    {
        for (Node node : nodes)
        {
            this.nodesById.put(node.id, node);
            this.allNodesFlattened.add(node);
            this.index(node.children);
        }
    }

    private List<Node> buildTree()
    {
        List<Node> roots = new ArrayList<>(ROOT_COUNT);
        for (int i = 0; i < ROOT_COUNT; i++)
        {
            List<Node> children = new ArrayList<>(CHILD_COUNT);
            for (int j = 0; j < CHILD_COUNT; j++)
            {
                boolean needleChild = i == NEEDLE_ROOT_INDEX && j == NEEDLE_CHILD_INDEX;
                List<Node> grandchildren = new ArrayList<>(GRANDCHILD_COUNT);
                for (int k = 0; k < GRANDCHILD_COUNT; k++)
                {
                    String id = String.format("r%02d-c%02d-g%02d", i, j, k);
                    String name = needleChild ? String.format("%s-Report-%02d.dat", NEEDLE_TOKEN, k)
                            : String.format("File-%02d-%02d-%02d.dat", i, j, k);
                    int deterministicIndex = this.grandchildIndex(i, j, k);
                    grandchildren.add(new Node(id, name, "File", Collections.emptyList(), this.ownerFor(deterministicIndex), this.modifiedFor(deterministicIndex)));
                }
                String childId = String.format("r%02d-c%02d", i, j);
                String childName = String.format("Folder-%02d-%02d", i, j);
                int deterministicIndex = this.childIndex(i, j);
                children.add(new Node(childId, childName, "Folder", grandchildren, this.ownerFor(deterministicIndex), this.modifiedFor(deterministicIndex)));
            }
            String rootId = String.format("r%02d", i);
            String rootName = String.format("Folder-%02d", i);
            roots.add(new Node(rootId, rootName, "Folder", children, this.ownerFor(i), this.modifiedFor(i)));
        }
        return roots;
    }

    /**
     * Deterministic per-node index (plan-80) that {@link #ownerFor(int)}/{@link #modifiedFor(int)} derive owner and
     * modified-date from - NOT a clock, purely a function of the node's own position in the generated tree, so
     * owner/modified are reproducible across runs AND vary across siblings (a sort on either column visibly
     * reshuffles rows) rather than being constant per group.
     */
    private int childIndex(int rootIndex, int childIndex)
    {
        return rootIndex * CHILD_COUNT + childIndex;
    }

    private int grandchildIndex(int rootIndex, int childIndex, int grandchildIndex)
    {
        return this.childIndex(rootIndex, childIndex) * GRANDCHILD_COUNT + grandchildIndex;
    }

    private String ownerFor(int deterministicIndex)
    {
        return OWNERS[deterministicIndex % OWNERS.length];
    }

    private String modifiedFor(int deterministicIndex)
    {
        return MODIFIED_BASE_DATE.plusDays(deterministicIndex)
                                 .toString();
    }

    private List<Node> resolveChildrenOf(String parentNodeId)
    {
        return Optional.ofNullable(this.nodesById.get(parentNodeId))
                       .map(node -> node.children)
                       .orElse(Collections.emptyList());
    }

    private List<Node> applyFilters(List<Node> nodes, List<ColumnFilter> filters)
    {
        if (filters == null || filters.isEmpty())
        {
            return nodes;
        }
        return nodes.stream()
                    .filter(node -> filters.stream()
                                           .allMatch(filter -> this.matches(node, filter)))
                    .collect(Collectors.toList());
    }

    private boolean matches(Node node, ColumnFilter filter)
    {
        Object cellValue = node.cells()
                               .get(filter.getColumnKey());
        if (cellValue == null)
        {
            return false;
        }
        Object filterValue = filter.getValue();
        FilterOperator operator = filter.getOperator();
        switch (operator)
        {
            case EQUALS :
                return cellValue.equals(filterValue);
            case CONTAINS :
                return String.valueOf(cellValue)
                             .toLowerCase()
                             .contains(String.valueOf(filterValue)
                                             .toLowerCase());
            case STARTS_WITH :
                return String.valueOf(cellValue)
                             .startsWith(String.valueOf(filterValue));
            case GREATER_THAN :
                return String.valueOf(cellValue)
                             .compareTo(String.valueOf(filterValue)) > 0;
            case LESS_THAN :
                return String.valueOf(cellValue)
                             .compareTo(String.valueOf(filterValue)) < 0;
            default :
                throw new IllegalArgumentException("Unsupported filter operator: " + operator);
        }
    }

    private List<Node> applySort(List<Node> nodes, List<SortColumn> sorts)
    {
        if (sorts == null || sorts.isEmpty())
        {
            return nodes;
        }

        Comparator<Node> comparator = null;
        for (SortColumn sort : sorts)
        {
            Comparator<Node> columnComparator = Comparator.comparing(node -> String.valueOf(node.cells()
                                                                                                .get(sort.getColumnKey())));
            if (sort.getDirection() == SortDirection.DESCENDING)
            {
                columnComparator = columnComparator.reversed();
            }
            comparator = comparator == null ? columnComparator : comparator.thenComparing(columnComparator);
        }

        List<Node> sorted = new ArrayList<>(nodes);
        sorted.sort(comparator);
        return sorted;
    }

    /**
     * One node of the generated ~10,050-node demo tree. {@code kind} is either {@code "Folder"} (has children,
     * always expandable) or {@code "File"} (leaf).
     */
    private static final class Node
    {
        private final String     id;
        private final String     name;
        private final String     kind;
        private final List<Node> children;
        private final String     owner;
        private final String     modified;

        private Node(String id, String name, String kind, List<Node> children, String owner, String modified)
        {
            this.id = id;
            this.name = name;
            this.kind = kind;
            this.children = children;
            this.owner = owner;
            this.modified = modified;
        }

        private Map<String, Object> cells()
        {
            Map<String, Object> cells = new LinkedHashMap<>();
            cells.put("name", this.name);
            cells.put("owner", this.owner);
            cells.put("modified", this.modified);
            cells.put("kind", this.kind);
            return cells;
        }
    }
}
