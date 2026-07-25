package org.omnaest.react4j.component.treetable.provider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.stream.Collectors;

import org.omnaest.react4j.component.treetable.provider.ColumnFilter.FilterOperator;
import org.omnaest.react4j.component.treetable.provider.SortColumn.SortDirection;

/**
 * Walking-skeleton {@link TreeTableDataProvider} backed by a small in-memory multi-level tree, for use as the executable
 * example in the SPI contract tests. Not part of any published API — test-scoped only.
 *
 * @see InMemoryTreeTableDataProviderTest
 * @author omnaest
 */
public class InMemoryTreeTableDataProvider implements TreeTableDataProvider
{
    private final List<Node>        rootNodes;
    private final Map<String, Node> nodesById = new HashMap<>();
    private final boolean           includeTotalChildCount;

    public InMemoryTreeTableDataProvider(List<Node> rootNodes, boolean includeTotalChildCount)
    {
        this.rootNodes = rootNodes;
        this.includeTotalChildCount = includeTotalChildCount;
        this.index(rootNodes);
    }

    private void index(List<Node> nodes)
    {
        for (Node node : nodes)
        {
            this.nodesById.put(node.getId(), node);
            this.index(node.getChildren());
        }
    }

    @Override
    public TreeTablePage fetch(TreeTableQuery query)
    {
        List<Node> siblingGroup = query.isFlat() ? this.allNodesFlattened()
                : query.getParentNodeId()
                       .map(this::resolveChildrenOf)
                       .orElse(this.rootNodes);

        List<Node> filtered = this.applyFilters(siblingGroup, query.getFilters());
        List<Node> sorted = this.applySort(filtered, query.getSorts());

        long offset = query.getOffset();
        int limit = query.getLimit();
        List<TreeTableRow> windowRows = sorted.stream()
                                              .skip(offset)
                                              .limit(limit)
                                              .map(node -> TreeTableRow.of(node.getId(), node.getCells(), !node.getChildren()
                                                                                                               .isEmpty()))
                                              .collect(Collectors.toList());

        OptionalLong totalChildCount = this.includeTotalChildCount ? OptionalLong.of(sorted.size()) : OptionalLong.empty();

        return TreeTablePage.of(windowRows, totalChildCount);
    }

    /**
     * Flat mode (plan-80): flattens EVERY node at EVERY depth (folders and files alike) into one list, DFS
     * pre-order, so filter/sort/window apply across the whole tree rather than one sibling group &mdash; the
     * "all-nodes" default this test provider documents as its own flat-semantics choice (SPI contract: flat
     * semantics are the provider's responsibility, see {@link TreeTableQuery#isFlat()}).
     */
    private List<Node> allNodesFlattened()
    {
        List<Node> result = new ArrayList<>();
        this.collectAllNodes(this.rootNodes, result);
        return result;
    }

    private void collectAllNodes(List<Node> nodes, List<Node> result)
    {
        for (Node node : nodes)
        {
            result.add(node);
            this.collectAllNodes(node.getChildren(), result);
        }
    }

    private List<Node> resolveChildrenOf(String parentNodeId)
    {
        return Optional.ofNullable(this.nodesById.get(parentNodeId))
                       .map(Node::getChildren)
                       .orElseThrow(() -> new IllegalArgumentException("Unknown parent node id: " + parentNodeId));
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
        Object cellValue = node.getCells()
                               .get(filter.getColumnKey());
        Object filterValue = filter.getValue();
        FilterOperator operator = filter.getOperator();

        if (cellValue == null)
        {
            return false;
        }

        switch (operator)
        {
            case EQUALS :
                return cellValue.equals(filterValue);
            case CONTAINS :
                return String.valueOf(cellValue)
                             .contains(String.valueOf(filterValue));
            case STARTS_WITH :
                return String.valueOf(cellValue)
                             .startsWith(String.valueOf(filterValue));
            case GREATER_THAN :
                return this.compareValues(cellValue, filterValue) > 0;
            case LESS_THAN :
                return this.compareValues(cellValue, filterValue) < 0;
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
            Comparator<Node> columnComparator = Comparator.comparing(node -> node.getCells()
                                                                                 .get(sort.getColumnKey()),
                                                                     this::compareValues);
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

    @SuppressWarnings({"unchecked", "rawtypes"})
    private int compareValues(Object a, Object b)
    {
        if (a == null && b == null)
        {
            return 0;
        }
        if (a == null)
        {
            return -1;
        }
        if (b == null)
        {
            return 1;
        }
        if (a instanceof Comparable && a.getClass()
                                        .isInstance(b))
        {
            return ((Comparable) a).compareTo(b);
        }
        return String.valueOf(a)
                     .compareTo(String.valueOf(b));
    }

    /**
     * A node of the small in-memory tree backing {@link InMemoryTreeTableDataProvider}.
     */
    public static class Node
    {
        private final String              id;
        private final Map<String, Object> cells;
        private final List<Node>          children;

        public Node(String id, Map<String, Object> cells, List<Node> children)
        {
            this.id = id;
            this.cells = cells;
            this.children = children != null ? children : Collections.emptyList();
        }

        public String getId()
        {
            return this.id;
        }

        public Map<String, Object> getCells()
        {
            return this.cells;
        }

        public List<Node> getChildren()
        {
            return this.children;
        }
    }

}
