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
 * Demo {@link TreeTableDataProvider} (plan-76 Slice 8) backing the {@link ComponentShowcaseUI} "TreeTable" section
 * with a small in-memory, multi-level, file-system-style tree (3 levels deep: root folders/files -&gt; nested
 * folders/files -&gt; grandchild files) so every {@code TreeTable} interaction is reviewable end-to-end: expanding a
 * root row AND a row it reveals (depth-2 expand), load-more (the root group has 6 entries, the "Documents/Reports"
 * group has 4 - both exceed the demo's small {@link org.omnaest.react4j.component.treetable.TreeTable#withWindowSize(int)}),
 * per-column CONTAINS filtering, and multi-column sort.
 * <p>
 * Honors the same {@link TreeTableQuery} contract as the SPI-contract-test {@code InMemoryTreeTableDataProvider}
 * (react4j-core-components test scope - not reusable here across module boundaries) and the hand-rolled equivalents
 * in the plan-76/77 seam tests: {@link TreeTableQuery#getParentNodeId()} scopes the fetch to one sibling group,
 * {@link TreeTableQuery#getFilters()} are AND-combined CONTAINS predicates applied within that group only (tree-
 * filter semantics are the provider's responsibility, see {@link TreeTableDataProvider}), and
 * {@link TreeTableQuery#getSorts()} is an ordered multi-column sort (index 0 = primary).
 *
 * @author omnaest
 */
class ShowcaseTreeTableDataProvider implements TreeTableDataProvider
{
    private final List<Node>        rootNodes = this.buildTree();
    private final Map<String, Node> nodesById = new HashMap<>();

    ShowcaseTreeTableDataProvider()
    {
        this.index(this.rootNodes);
    }

    @Override
    public TreeTablePage fetch(TreeTableQuery query)
    {
        List<Node> siblingGroup = query.getParentNodeId()
                                       .map(this::resolveChildrenOf)
                                       .orElse(this.rootNodes);

        List<Node> filtered = this.applyFilters(siblingGroup, query.getFilters());
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
            this.index(node.children);
        }
    }

    /**
     * Root order is deliberately NOT alphabetical ("Videos" before "Documents" before "Downloads" ...) so a
     * live "sort by name" click visibly reorders rows. "Documents/Reports" holds 4 files against a demo window of
     * 3 (see {@link ComponentShowcaseUI}), exercising load-more at a NON-root depth in addition to the 6-entry root
     * group.
     */
    private List<Node> buildTree()
    {
        Node q1 = new Node("q1", "Q1 Report.pdf", "File", Collections.emptyList());
        Node q2 = new Node("q2", "Q2 Report.pdf", "File", Collections.emptyList());
        Node q3 = new Node("q3", "Q3 Report.pdf", "File", Collections.emptyList());
        Node q4 = new Node("q4", "Q4 Report.pdf", "File", Collections.emptyList());
        Node reports = new Node("reports", "Reports", "Folder", List.of(q1, q2, q3, q4));
        Node notes = new Node("notes", "Notes.txt", "File", Collections.emptyList());
        Node budget = new Node("budget", "Budget.xlsx", "File", Collections.emptyList());
        Node documents = new Node("documents", "Documents", "Folder", List.of(reports, notes, budget));

        Node photo1 = new Node("photo1", "photo1.jpg", "File", Collections.emptyList());
        Node photo2 = new Node("photo2", "photo2.jpg", "File", Collections.emptyList());
        Node vacation = new Node("vacation", "Vacation", "Folder", List.of(photo1, photo2));
        Node profile = new Node("profile", "profile.png", "File", Collections.emptyList());
        Node pictures = new Node("pictures", "Pictures", "Folder", List.of(vacation, profile));

        Node clip1 = new Node("clip1", "clip1.mp4", "File", Collections.emptyList());
        Node videos = new Node("videos", "Videos", "Folder", List.of(clip1));

        Node song1 = new Node("song1", "song1.mp3", "File", Collections.emptyList());
        Node music = new Node("music", "Music", "Folder", List.of(song1));

        Node oldZip = new Node("oldzip", "old.zip", "File", Collections.emptyList());
        Node archive = new Node("archive", "Archive", "Folder", List.of(oldZip));

        Node installer = new Node("installer", "installer.exe", "File", Collections.emptyList());
        Node downloads = new Node("downloads", "Downloads", "Folder", List.of(installer));

        return List.of(videos, documents, downloads, pictures, music, archive);
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
     * One node of the small demo tree. {@code kind} is either {@code "Folder"} (has children, always expandable) or
     * {@code "File"} (leaf).
     */
    private static final class Node
    {
        private final String     id;
        private final String     name;
        private final String     kind;
        private final List<Node> children;

        private Node(String id, String name, String kind, List<Node> children)
        {
            this.id = id;
            this.name = name;
            this.kind = kind;
            this.children = children;
        }

        private Map<String, Object> cells()
        {
            Map<String, Object> cells = new LinkedHashMap<>();
            cells.put("name", this.name);
            cells.put("kind", this.kind);
            return cells;
        }
    }
}
