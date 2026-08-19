package org.omnaest.react4j.component.treetable.internal.renderer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.omnaest.react4j.component.treetable.internal.data.TreeTableData;
import org.omnaest.react4j.component.treetable.internal.renderer.node.TreeTableNode;
import org.omnaest.react4j.component.treetable.internal.renderer.node.TreeTableNode.ColumnNode;
import org.omnaest.react4j.component.treetable.internal.renderer.node.TreeTableNode.LoadMoreNode;
import org.omnaest.react4j.component.treetable.internal.renderer.node.TreeTableNode.RowEntryNode;
import org.omnaest.react4j.component.treetable.provider.ColumnFilter;
import org.omnaest.react4j.component.treetable.provider.ColumnFilter.FilterOperator;
import org.omnaest.react4j.component.treetable.provider.SortColumn;
import org.omnaest.react4j.component.treetable.provider.SortColumn.SortDirection;
import org.omnaest.react4j.component.treetable.provider.TreeTableColumn;
import org.omnaest.react4j.component.treetable.provider.TreeTableDataProvider;
import org.omnaest.react4j.component.treetable.provider.TreeTablePage;
import org.omnaest.react4j.component.treetable.provider.TreeTableQuery;
import org.omnaest.react4j.component.treetable.provider.TreeTableRow;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.RerenderingContainer;
import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.UIComponentFactory;
import org.omnaest.react4j.domain.context.data.Data;
import org.omnaest.react4j.domain.context.data.Value;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.RenderableUIComponent;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer;
import org.omnaest.react4j.domain.rendering.components.LocationSupport;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.domain.rendering.node.NodeRendererRegistry;
import org.omnaest.react4j.domain.support.UIComponentProvider;
import org.omnaest.react4j.service.internal.component.AbstractUIComponent;
import org.omnaest.react4j.service.internal.component.ComponentContext;
import org.omnaest.react4j.service.internal.handler.domain.DataEventHandler;
import org.omnaest.react4j.service.internal.handler.domain.Target;

import lombok.RequiredArgsConstructor;

/**
 * Renders a {@link org.omnaest.react4j.component.treetable.TreeTable} root group.
 * <p>
 * Reads the {@link TreeTableQuery} the round trip carries (root only, offset 0, {@link TreeTableData#getWindowSize()}
 * this slice), calls {@link TreeTableDataProvider#fetch(TreeTableQuery)}, and emits the flattened
 * {@link TreeTableNode} for the root sibling group. The whole body is wrapped in ONE {@link RerenderingContainer}
 * (Cliff C1 / trap #3 {@code react4j-atomic-coupdate-siblings-under-one-rerenderingcontainer}) fed via
 * {@link RerenderingContainer#withDataDrivenContent(org.omnaest.react4j.domain.support.UIComponentProviderWithData)} so the
 * submitted {@link Data} reaches the content builder on every round trip (never
 * {@code withContent(UIComponentFactoryFunction)} — that overload has no access to the submitted {@link Data}).
 *
 * @author omnaest
 */
@RequiredArgsConstructor
public class TreeTableRendererImpl implements UIComponentRenderer
{
    private final TreeTableData      data;
    private final UIComponentFactory uiComponentFactory;
    private final ComponentContext   context;

    private UIComponentRenderer delegate()
    {
        RerenderingContainer container = this.uiComponentFactory.newRerenderingContainer()
                                                                .withDataDrivenContent(submittedData -> new TreeTableGridComponent(this.context, this.data,
                                                                                                                                   submittedData))
                                                                .enableStaticNodeRerendering();
        return ((RenderableUIComponent<?>) container).asRenderer();
    }

    @Override
    public Location getLocation(LocationSupport locationSupport)
    {
        return this.delegate()
                   .getLocation(locationSupport);
    }

    @Override
    public Node render(RenderingProcessor renderingProcessor, Location location, Optional<Data> data)
    {
        return this.delegate()
                   .render(renderingProcessor, location, data);
    }

    @Override
    public void manageEventHandler(EventHandlerRegistrationSupport eventHandlerRegistrationSupport)
    {
        this.delegate()
            .manageEventHandler(eventHandlerRegistrationSupport);
    }

    @Override
    public void manageNodeRenderers(NodeRendererRegistry registry)
    {
        // no server-side HTML fallback template this slice; TREETABLE is rendered natively by the frontend (Slice 3)
    }

    @Override
    public Stream<ParentLocationAndComponent> getSubComponents(Location parentLocation)
    {
        return this.delegate()
                   .getSubComponents(parentLocation);
    }

    @Override
    public Stream<ParentLocationAndComponent> getSubComponents(Location parentLocation, Optional<Data> data)
    {
        // plan-77 Cliff F2: this class hand-forwards every UIComponentRenderer method to the delegate
        // RerenderingContainer's renderer, so it must ALSO forward the Data-aware 2-arg overload explicitly -
        // without this override, the interface's own additive default would resolve to THIS class's 1-arg
        // override above (this.getSubComponents(parentLocation)), which forwards to the delegate's 1-arg overload
        // (hardcoded empty Data), silently losing the submitted Data before it ever reaches the RerenderingContainer.
        return this.delegate()
                   .getSubComponents(parentLocation, data);
    }

    /**
     * Internal-only content component that satisfies {@link RerenderingContainer#withDataDrivenContent} 's
     * {@code UIComponentProviderWithData<UIC>} contract. Its renderer performs the actual query build +
     * {@link TreeTableDataProvider#fetch(TreeTableQuery)} + flattening into the emitted {@link TreeTableNode}.
     */
    private static class TreeTableGridComponent extends AbstractUIComponent<TreeTableGridComponent>
    {
        private final TreeTableData data;
        private final Data          submittedData;

        TreeTableGridComponent(ComponentContext context, TreeTableData data, Data submittedData)
        {
            super(context);
            this.data = data;
            this.submittedData = submittedData;
        }

        @Override
        public UIComponentRenderer asRenderer()
        {
            return new UIComponentRenderer() {
                @Override
                public Location getLocation(LocationSupport locationSupport)
                {
                    return locationSupport.createLocation(TreeTableGridComponent.this.getId());
                }

                @Override
                public Node render(RenderingProcessor renderingProcessor, Location location, Optional<Data> ignoredData)
                {
                    return TreeTableGridComponent.this.buildNode(location);
                }

                @Override
                public void manageNodeRenderers(NodeRendererRegistry registry)
                {
                    // no server-side HTML fallback template this slice
                }

                @Override
                public Stream<ParentLocationAndComponent> getSubComponents(Location parentLocation)
                {
                    // Slice 5: full C3 modeling. This traversal must discover exactly the same subcomponents that
                    // buildNode(...) below would emit AS ROWS for the SAME submittedData - the root load-more
                    // (unconditional, unchanged since Slice 4), one expand-control per EXPANDABLE row (gated:
                    // react4j-gate-optin-handler-on-existing-node), and - recursively, for every row currently
                    // EXPANDED per submittedData's expanded-node-id set - that row's OWN child-group load-more plus
                    // its children's row/load-more subcomponents. See buildNode(Location) for the identical
                    // recursion this mirrors (react4j-routing-key-must-be-stable-and-unique-use-positional-id: both
                    // traversals must agree).
                    if (parentLocation == null)
                    {
                        return Stream.empty();
                    }
                    List<ParentLocationAndComponent> result = new ArrayList<>();
                    result.add(ParentLocationAndComponent.of(parentLocation,
                                                             new TreeTableLoadMoreControlImpl(TreeTableGridComponent.this.context,
                                                                                              TreeTableGridComponent.this.buildLoadMoreHandler(parentLocation, null))));

                    // plan-78: the funnel filter-visibility toggle is present at INITIAL render (unconditional, same
                    // as the filter/sort header controls below, not gated per react4j-gate-optin-handler-on-existing-node
                    // - it must be clickable from the very first render since the filter row starts collapsed) - but
                    // ONLY when filtering is enabled at all (build-time config, constant across renders, safe to gate
                    // this registration walk on - see TreeTableNode.filterEnabled javadoc). When disabled, no
                    // sub-component is constructed at all, so its Target never routes.
                    boolean filterEnabled = TreeTableGridComponent.this.effectiveFilterEnabled();
                    boolean sortEnabled = TreeTableGridComponent.this.data.isSortEnabled();
                    boolean multiColumnSortEnabled = TreeTableGridComponent.this.data.isMultiColumnSortEnabled();
                    List<SortColumn> registrationSorts = TreeTableGridComponent.this.readSorts(parentLocation);
                    if (filterEnabled)
                    {
                        result.add(ParentLocationAndComponent.of(parentLocation,
                                                                 new TreeTableFilterToggleControlImpl(TreeTableGridComponent.this.context,
                                                                                                      TreeTableGridComponent.this.buildFilterToggleHandler(parentLocation))));
                    }

                    // plan-80: the flat/tree toggle is present at INITIAL render (unconditional, same reasoning as
                    // the funnel toggle above - not gated per react4j-gate-optin-handler-on-existing-node), but ONLY
                    // when TreeTable.withFlatModeToggleEnabled(true) was configured (build-time config, constant
                    // across renders, safe to gate this registration walk on).
                    boolean flatModeToggleEnabled = TreeTableGridComponent.this.data.isFlatModeToggleEnabled();
                    if (flatModeToggleEnabled)
                    {
                        result.add(ParentLocationAndComponent.of(parentLocation,
                                                                 new TreeTableFlatModeToggleControlImpl(TreeTableGridComponent.this.context,
                                                                                                        TreeTableGridComponent.this.buildFlatModeToggleHandler(parentLocation))));
                    }

                    // Slice 6 / plan-78: per-column filter + sort header controls are present at INITIAL render
                    // (unconditional, not gated on any data), so they register normally at every render pass
                    // (react4j-gate-optin-handler-on-existing-node does not apply here - contrast with the expand
                    // control, which IS gated on row.isExpandable()) - EXCEPT a column whose own filterable/sortable
                    // config is false, or whose feature is globally disabled, gets no control at all (plan-78).
                    List<TreeTableColumn> columns = TreeTableGridComponent.this.data.getColumns();
                    for (int columnIndex = 0; columnIndex < columns.size(); columnIndex++)
                    {
                        TreeTableColumn column = columns.get(columnIndex);
                        if (filterEnabled && column.isFilterable())
                        {
                            result.add(ParentLocationAndComponent.of(parentLocation,
                                                                     new TreeTableFilterControlImpl(TreeTableGridComponent.this.context, columnIndex,
                                                                                                    TreeTableGridComponent.this.buildFilterChangeHandler())));
                        }
                        if (sortEnabled && column.isSortable())
                        {
                            result.add(ParentLocationAndComponent.of(parentLocation,
                                                                     new TreeTableSortToggleControlImpl(TreeTableGridComponent.this.context, columnIndex,
                                                                                                        TreeTableGridComponent.this.buildSortToggleHandler(parentLocation,
                                                                                                                                                           column.getKey()))));
                        }
                        // Reorder control: only for a sortable column that is CURRENTLY ACTIVE (present in
                        // registrationSorts, the SAME field buildNode's sortPriority/sortPriorityFieldKey below
                        // derive from) AND only when multi-column sort is enabled - a re-rank control has no
                        // meaning for a column that isn't sorted yet, or in single-column mode.
                        if (multiColumnSortEnabled && sortEnabled && column.isSortable()
                            && TreeTableGridComponent.this.indexOfSort(registrationSorts, column.getKey()) >= 0)
                        {
                            result.add(ParentLocationAndComponent.of(parentLocation,
                                                                     new TreeTableSortPriorityControlImpl(TreeTableGridComponent.this.context, columnIndex,
                                                                                                          TreeTableGridComponent.this.buildSortPriorityReorderHandler(parentLocation,
                                                                                                                                                                      column.getKey()))));
                        }
                    }

                    TreeTableDataProvider provider = TreeTableGridComponent.this.data.getDataProvider();
                    int rootLimit = TreeTableGridComponent.this.currentWindowLimit(parentLocation, null);
                    List<SortColumn> sorts = registrationSorts;
                    List<ColumnFilter> filters = TreeTableGridComponent.this.readFilters(parentLocation);
                    boolean flatMode = TreeTableGridComponent.this.currentFlatMode(parentLocation);
                    TreeTableQuery rootQuery = TreeTableQuery.of(null, 0L, rootLimit, sorts, filters, flatMode);
                    TreeTablePage rootPage = provider.fetch(rootQuery);

                    // plan-80: in flat mode there are no per-row expand controls, no child fetches, and the
                    // expanded-node-id set is ignored entirely - this traversal must agree with buildNode(...) below,
                    // which likewise skips the recursive appendRows(...) path in flat mode (both traversals must
                    // discover/emit identically for the SAME submittedData).
                    if (!flatMode)
                    {
                        Set<String> expandedNodeIds = TreeTableGridComponent.this.readExpandedNodeIds(parentLocation);
                        TreeTableGridComponent.this.collectRowSubComponents(result, parentLocation, provider, rootPage.getRows(), new AtomicInteger(0),
                                                                            expandedNodeIds, sorts, filters);
                    }
                    return result.stream();
                }
            };
        }

        @Override
        public UIComponentProvider<TreeTableGridComponent> asTemplateProvider()
        {
            return () -> new TreeTableGridComponent(this.context, this.data, this.submittedData);
        }

        /**
         * Recursively discovers row-expand-control and child-group-load-more subcomponents (Slice 5), mirroring
         * {@link #appendRows(List, Location, TreeTableDataProvider, List, int, AtomicInteger, Set)}'s traversal order
         * exactly (same flatIndex counter, same fetch-then-recurse-if-expanded shape) so both traversals compute
         * identical positional {@link Location}s for the same submittedData (Cliff C3).
         */
        private void collectRowSubComponents(List<ParentLocationAndComponent> result, Location gridLocation, TreeTableDataProvider provider, List<TreeTableRow> providerRows, AtomicInteger flatIndexCounter, Set<String> expandedNodeIds, List<SortColumn> sorts, List<ColumnFilter> filters)
        {
            for (TreeTableRow row : providerRows)
            {
                int flatIndex = flatIndexCounter.getAndIncrement();
                if (row.isExpandable())
                {
                    DataEventHandler expandHandler = this.buildExpandToggleHandler(gridLocation, row.getNodeId());
                    result.add(ParentLocationAndComponent.of(gridLocation,
                                                             new TreeTableRowExpandControlImpl(this.context, flatIndex, expandHandler)));
                }
                boolean expanded = row.isExpandable() && expandedNodeIds.contains(row.getNodeId());
                if (expanded)
                {
                    Location rowLocation = this.createRowLocation(gridLocation, flatIndex);
                    int childLimit = this.currentWindowLimit(gridLocation, row.getNodeId());
                    TreeTableQuery childQuery = TreeTableQuery.of(row.getNodeId(), 0L, childLimit, sorts, filters);
                    TreeTablePage childPage = provider.fetch(childQuery);
                    result.add(ParentLocationAndComponent.of(rowLocation,
                                                             new TreeTableLoadMoreControlImpl(this.context,
                                                                                              this.buildLoadMoreHandler(gridLocation, row.getNodeId()))));
                    this.collectRowSubComponents(result, gridLocation, provider, childPage.getRows(), flatIndexCounter, expandedNodeIds, sorts, filters);
                }
            }
        }

        private Node buildNode(Location location)
        {
            TreeTableDataProvider provider = this.data.getDataProvider();
            Set<String> expandedNodeIds = this.readExpandedNodeIds(location);

            // Slice 6: filter/sort state is read ONCE per render pass and threaded through every fetch at every
            // depth - "changing a filter or sort re-queries every visible group through the same provider.fetch"
            // (interaction-with-expansion/windowing decision, see class javadoc addendum below). Per-group window
            // limits (currentWindowLimit) are intentionally left untouched by a filter/sort change - the least
            // surprising behavior is documented on readFilters/readSorts.
            List<SortColumn> sorts = this.readSorts(location);
            List<ColumnFilter> filters = this.readFilters(location);

            // plan-80: flatMode is read ONCE per render pass, mirroring filtersVisible/expandedNodes - it decides
            // BOTH the query shape (empty parentNodeId + isFlat==true, always at the root) AND how the returned rows
            // are flattened (see appendFlatRows vs appendRows below). The expanded-node-id set is ignored entirely
            // while flatMode is true (AC-per-brief: "the expanded-node-id set is ignored in flat mode").
            boolean flatMode = this.currentFlatMode(location);

            // Cliff C1a mechanism (a): the load-more window state is carried as a field on the submitted Data
            // (mutated by TreeTableLoadMoreControlImpl's handler, never a handler-mutated SERVER field), so this
            // re-render derives the query's limit from the SAME Data the round trip's second render pass sees -
            // the lag-immune path proven by TreeTableLoadMoreEndToEndTest in react4j-core.
            int rootLimit = this.currentWindowLimit(location, null);
            TreeTableQuery rootQuery = TreeTableQuery.of(null, 0L, rootLimit, sorts, filters, flatMode);
            TreeTablePage rootPage = provider.fetch(rootQuery);

            List<RowEntryNode> rows = new ArrayList<>();
            if (flatMode)
            {
                this.appendFlatRows(rows, location, rootPage.getRows());
            }
            else
            {
                this.appendRows(rows, location, provider, rootPage.getRows(), 0, new AtomicInteger(0), expandedNodeIds, sorts, filters);
            }

            // plan-77: activeFilterCount is derived from the SAME `filters` list this render pass already built from
            // submitted Data (Slice 6) - one entry per column with a non-empty submitted filter value.
            // plan-78: filterEnabled/sortEnabled are build-time configuration (constant across renders, see
            // TreeTableNode javadoc) - readFilters/readSorts above already gate on them, so `filters`/`sorts` are
            // naturally empty when their feature is disabled.
            boolean filterEnabled = this.effectiveFilterEnabled();
            boolean sortEnabled = this.data.isSortEnabled();
            boolean multiColumnSortEnabled = this.data.isMultiColumnSortEnabled();
            Location filterToggleLocation = filterEnabled ? this.createFilterOrSortLocation(location, "filtertoggle") : null;

            // plan-80: flatModeToggleEnabled is build-time component configuration (constant across renders, see
            // TreeTableNode javadoc), mirroring filterEnabled/sortEnabled above.
            boolean flatModeToggleEnabled = this.data.isFlatModeToggleEnabled();
            Location flatToggleLocation = flatModeToggleEnabled ? this.createFilterOrSortLocation(location, "flattoggle") : null;
            return TreeTableNode.builder()
                                .columns(this.buildColumnNodes(location, sorts))
                                .rows(rows)
                                .loadMore(this.buildLoadMoreNode(rootQuery, rootPage, location))
                                .filtersVisible(filterEnabled ? this.currentFiltersVisible(location) : false)
                                .activeFilterCount(filters.size())
                                .filterToggleTarget(filterToggleLocation != null ? Target.from(filterToggleLocation) : Target.empty())
                                .filterEnabled(filterEnabled)
                                .sortEnabled(sortEnabled)
                                .stickyHeader(this.data.isStickyHeader())
                                .multiColumnSortEnabled(multiColumnSortEnabled)
                                .activeSortCount(sorts.size())
                                .flatModeToggleEnabled(flatModeToggleEnabled)
                                .flatMode(flatMode)
                                .flatToggleTarget(flatToggleLocation != null ? Target.from(flatToggleLocation) : Target.empty())
                                .build();
        }

        /**
         * Whole-grid filter feature gate (plan-78): {@code true} only when the app has NOT called
         * {@code TreeTable.withFilterEnabled(false)} AND at least one configured column is filterable - either
         * condition being false collapses the ENTIRE filter feature (no funnel toggle, no filter row, no per-column
         * filter controls at all). Both inputs are build-time component configuration (constant across renders,
         * never derived from submitted Data), so gating BOTH the registration walk ({@code getSubComponents}) and the
         * render ({@code buildNode}) on this same method is safe (the exact same reasoning as the plan-77 registration
         * walk comment on the funnel toggle).
         */
        private boolean effectiveFilterEnabled()
        {
            return this.data.isFilterEnabled() && this.data.getColumns()
                                                           .stream()
                                                           .anyMatch(TreeTableColumn::isFilterable);
        }

        /**
         * Builds the per-column {@link ColumnNode} header-control descriptors (Slice 6): the current filter value +
         * deterministic {@code filterFieldKey} + routable {@code filterTarget}, and the current sort direction +
         * routable {@code sortTarget}. Columns keep a stable index-based {@link Location} segment ({@code "filter" +
         * i} / {@code "sort" + i}), mirroring the row/load-more positional-Location discipline (Cliff C3).
         */
        private List<ColumnNode> buildColumnNodes(Location gridLocation, List<SortColumn> sorts)
        {
            boolean filterEnabled = this.effectiveFilterEnabled();
            boolean sortEnabled = this.data.isSortEnabled();
            boolean multiColumnSortEnabled = this.data.isMultiColumnSortEnabled();
            List<TreeTableColumn> columns = this.data.getColumns();
            List<ColumnNode> columnNodes = new ArrayList<>(columns.size());
            for (int columnIndex = 0; columnIndex < columns.size(); columnIndex++)
            {
                TreeTableColumn column = columns.get(columnIndex);
                // plan-78: a column's filter/sort control is built only when the WHOLE feature is enabled AND this
                // column's own per-column filterable/sortable flag is true.
                boolean columnFilterActive = filterEnabled && column.isFilterable() && gridLocation != null;
                boolean columnSortActive = sortEnabled && column.isSortable();

                String filterValue = columnFilterActive ? this.currentFilterValue(gridLocation, column.getKey()) : null;
                // plan-81: sortIndex (-1 when not currently sorted) drives BOTH the per-column sortDirection
                // indicator AND the new sortPriority (1-based, 0 when inactive) from the SAME ordered `sorts` list.
                int sortIndex = columnSortActive ? this.indexOfSort(sorts, column.getKey()) : -1;
                SortDirection sortDirection = sortIndex >= 0 ? sorts.get(sortIndex)
                                                                    .getDirection()
                        : null;
                int sortPriority = sortIndex >= 0 ? sortIndex + 1 : 0;
                Location filterLocation = columnFilterActive ? this.createFilterOrSortLocation(gridLocation, "filter" + columnIndex) : null;
                Location sortLocation = columnSortActive ? this.createFilterOrSortLocation(gridLocation, "sort" + columnIndex) : null;

                // plan-81: the priority re-rank control only makes sense for a column that is CURRENTLY ACTIVE
                // (sortIndex >= 0), and only in multi-column mode - mirrors the getSubComponents registration gate
                // above (react4j-gate-optin-handler-on-existing-node: both traversals must agree).
                boolean sortPriorityReorderActive = multiColumnSortEnabled && sortIndex >= 0 && gridLocation != null;
                Location sortPriorityLocation = sortPriorityReorderActive ? this.createFilterOrSortLocation(gridLocation, "sortpriority" + columnIndex) : null;

                columnNodes.add(ColumnNode.builder()
                                          .key(column.getKey())
                                          .title(column.getTitle())
                                          .filterable(column.isFilterable())
                                          .sortable(column.isSortable())
                                          .filterValue(filterValue)
                                          .filterFieldKey(columnFilterActive ? this.filterFieldKey(gridLocation, column.getKey()) : null)
                                          .filterTarget(filterLocation != null ? Target.from(filterLocation) : Target.empty())
                                          .sortDirection(sortDirection)
                                          .sortTarget(sortLocation != null ? Target.from(sortLocation) : Target.empty())
                                          .sortPriority(sortPriority)
                                          .sortPriorityFieldKey(sortPriorityReorderActive ? this.sortPriorityFieldKey(gridLocation, column.getKey()) : null)
                                          .sortPriorityReorderTarget(sortPriorityLocation != null ? Target.from(sortPriorityLocation) : Target.empty())
                                          .build());
            }
            return columnNodes;
        }

        /**
         * Index of {@code columnKey} in the ordered {@code sorts} list, or {@code -1} when not present (plan-81).
         */
        private int indexOfSort(List<SortColumn> sorts, String columnKey)
        {
            for (int i = 0; i < sorts.size(); i++)
            {
                if (columnKey.equals(sorts.get(i)
                                          .getColumnKey()))
                {
                    return i;
                }
            }
            return -1;
        }

        private Location createFilterOrSortLocation(Location gridLocation, String segment)
        {
            return Optional.ofNullable(gridLocation)
                           .map(iLocation -> iLocation.and(segment))
                           .orElse(null);
        }

        /**
         * Recursively flattens one sibling group into {@code rows} (Cliff C4: flat row-list, depth is just a field -
         * never a nested node structure). For every row currently EXPANDED per {@code expandedNodeIds}, fetches that
         * row's OWN child sibling group (own window via {@link #windowFieldKey(Location, String)} keyed by the
         * row's {@code nodeId}, own {@code childLoadMore}) and recurses at {@code depth + 1}, positioning the
         * children immediately after their parent row - satisfying AC3's "recurse for nested expanded nodes" for a
         * child that is itself expanded.
         */
        private void appendRows(List<RowEntryNode> rows, Location gridLocation, TreeTableDataProvider provider, List<TreeTableRow> providerRows, int depth, AtomicInteger flatIndexCounter, Set<String> expandedNodeIds, List<SortColumn> sorts, List<ColumnFilter> filters)
        {
            for (TreeTableRow row : providerRows)
            {
                int flatIndex = flatIndexCounter.getAndIncrement();
                Location rowLocation = this.createRowLocation(gridLocation, flatIndex);
                boolean expanded = row.isExpandable() && expandedNodeIds.contains(row.getNodeId());

                RowEntryNode.RowEntryNodeBuilder rowBuilder = RowEntryNode.builder()
                                                                          .nodeId(row.getNodeId())
                                                                          .depth(depth)
                                                                          .expandable(row.isExpandable())
                                                                          .expanded(expanded)
                                                                          .cells(this.toStringifiedCells(row.getCells()))
                                                                          .target(rowLocation != null ? Target.from(rowLocation) : Target.empty());

                if (expanded)
                {
                    int childLimit = this.currentWindowLimit(gridLocation, row.getNodeId());
                    TreeTableQuery childQuery = TreeTableQuery.of(row.getNodeId(), 0L, childLimit, sorts, filters);
                    TreeTablePage childPage = provider.fetch(childQuery);
                    rowBuilder.childLoadMore(this.buildLoadMoreNode(childQuery, childPage, rowLocation));
                    rows.add(rowBuilder.build());
                    this.appendRows(rows, gridLocation, provider, childPage.getRows(), depth + 1, flatIndexCounter, expandedNodeIds, sorts, filters);
                }
                else
                {
                    rows.add(rowBuilder.build());
                }
            }
        }

        /**
         * Flattens ONE already-flat provider page into {@code rows} (plan-80): every row is emitted at
         * {@code depth == 0} with {@code expandable == false} (no carets, no indentation, no
         * {@code childLoadMore}) &mdash; the provider already returned the WHOLE flattened node set for this window
         * ({@link TreeTableQuery#isFlat()}), so there is no recursion and no per-row expand control here, unlike
         * {@link #appendRows(List, Location, TreeTableDataProvider, List, int, AtomicInteger, Set, List, List)}. Rows
         * still get their own deterministic positional {@link Location}/{@link Target} (Cliff C3), even though
         * nothing registers an expand handler at it in flat mode.
         */
        private void appendFlatRows(List<RowEntryNode> rows, Location gridLocation, List<TreeTableRow> providerRows)
        {
            int flatIndex = 0;
            for (TreeTableRow row : providerRows)
            {
                Location rowLocation = this.createRowLocation(gridLocation, flatIndex++);
                rows.add(RowEntryNode.builder()
                                     .nodeId(row.getNodeId())
                                     .depth(0)
                                     .expandable(false)
                                     .expanded(false)
                                     .cells(this.toStringifiedCells(row.getCells()))
                                     .target(rowLocation != null ? Target.from(rowLocation) : Target.empty())
                                     .build());
            }
        }

        /**
         * Deterministic positional row {@link Location} (Cliff C3 / traps #4a-#4b) — mirrors
         * {@code TableRendererImpl.createCellLocation}. NEVER an {@link java.util.concurrent.atomic.AtomicLong}: the
         * segment is derived purely from the row's flat index (over the WHOLE flattened row list, across all
         * depths), so it is identical on every render for the same submittedData.
         */
        private Location createRowLocation(Location location, int flatIndex)
        {
            return Optional.ofNullable(location)
                           .map(iLocation -> iLocation.and("row" + flatIndex))
                           .orElse(null);
        }

        private Map<String, String> toStringifiedCells(Map<String, Object> cells)
        {
            if (cells == null)
            {
                return Collections.emptyMap();
            }
            return cells.entrySet()
                        .stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, entry -> Objects.toString(entry.getValue(), "")));
        }

        /**
         * @param loadMoreBaseLocation
         *            the {@link Location} the {@code "loadmore"} segment is appended to - the grid's OWN Location
         *            for the root group, or the parent row's own {@link Location} for a child group (Slice 5),
         *            giving every group's load-more control a distinct, deterministic positional {@link Target}.
         */
        private LoadMoreNode buildLoadMoreNode(TreeTableQuery query, TreeTablePage page, Location loadMoreBaseLocation)
        {
            long nextOffset = query.getOffset() + page.getRows()
                                                      .size();
            OptionalLong totalChildCount = page.getTotalChildCount();
            boolean available = totalChildCount.isPresent() ? nextOffset < totalChildCount.getAsLong()
                    : page.getRows()
                          .size() == query.getLimit();

            Location loadMoreLocation = Optional.ofNullable(loadMoreBaseLocation)
                                                .map(iLocation -> iLocation.and("loadmore"))
                                                .orElse(null);
            // Gate the emitted target on availability (mirrors ButtonImpl/PaginationItemImpl only emitting
            // ServerHandler when an eventHandler is actually configured): the handler stays registered
            // unconditionally (see getSubComponents above), but a client has no routable affordance to click once
            // the whole group is already loaded.
            Target target = available && loadMoreLocation != null ? Target.from(loadMoreLocation) : Target.empty();
            return LoadMoreNode.builder()
                               .available(available)
                               .nextOffset(nextOffset)
                               .nextLimit(query.getLimit())
                               .totalCount(totalChildCount.isPresent() ? totalChildCount.getAsLong() : null)
                               .target(target)
                               .build();
        }

        /**
         * Builds the load-more click handler for one sibling group (root when {@code parentNodeId == null}, else the
         * child group of that node), advancing exactly that group's window field (Cliff C1a mechanism (a)). Shared by
         * the root load-more (registered unconditionally, Slice 4) and every expanded row's own child-group load-more
         * (Slice 5) so the two groups' windows never interfere.
         */
        private DataEventHandler buildLoadMoreHandler(Location gridLocation, String parentNodeId)
        {
            String windowFieldKey = this.windowFieldKey(gridLocation, parentNodeId);
            int increment = Math.max(1, this.data.getWindowSize());
            return (eventData, internalData) ->
            {
                int currentLimit = eventData.getFieldValue(windowFieldKey)
                                            .map(Value::asInteger)
                                            .orElse(increment);
                eventData.setFieldValue(windowFieldKey, currentLimit + increment);
                return DataEventHandler.MappedData.builder()
                                                  .data(eventData)
                                                  .internalData(internalData)
                                                  .build();
            };
        }

        /**
         * Builds the expand/collapse toggle handler for one row (Cliff C5): mutates the SAME submitted-{@link Data}
         * field the re-render reads (mechanism (a)) by adding/removing {@code nodeId} from the whole grid's single
         * expanded-node-id set (see {@link #expandedNodesFieldKey(Location)} - one field for the ENTIRE tree, not
         * per-group, since expansion is a property of the node, not of any one sibling group).
         */
        private DataEventHandler buildExpandToggleHandler(Location gridLocation, String nodeId)
        {
            String fieldKey = this.expandedNodesFieldKey(gridLocation);
            return (eventData, internalData) ->
            {
                List<String> current = new ArrayList<>(eventData.getFieldValue(fieldKey)
                                                                .map(Value::asStringList)
                                                                .orElse(Collections.emptyList()));
                if (current.contains(nodeId))
                {
                    current.remove(nodeId);
                }
                else
                {
                    current.add(nodeId);
                }
                eventData.setFieldValue(fieldKey, current);
                return DataEventHandler.MappedData.builder()
                                                  .data(eventData)
                                                  .internalData(internalData)
                                                  .build();
            };
        }

        /**
         * Deterministic submitted-{@link Data} field key carrying the CURRENT (possibly load-more-widened) window
         * limit for one sibling group (Cliff C1a mechanism (a)). Keyed by this grid's own positional {@link Location}
         * plus the group ({@code parentNodeId}, {@code "root"} when {@code null}) - forward-compatible with
         * per-{@code parentNodeId} child groups (Slice 5) unchanged since Slice 4.
         */
        private String windowFieldKey(Location location, String parentNodeId)
        {
            String group = parentNodeId != null ? parentNodeId : "root";
            return "treetable." + String.join(".", location.get()) + "." + group + ".windowLimit";
        }

        private int currentWindowLimit(Location gridLocation, String parentNodeId)
        {
            if (gridLocation == null)
            {
                return this.data.getWindowSize();
            }
            return this.submittedData.getFieldValue(this.windowFieldKey(gridLocation, parentNodeId))
                                     .map(Value::asInteger)
                                     .orElse(this.data.getWindowSize());
        }

        /**
         * Deterministic submitted-{@link Data} field key carrying the SET of currently-expanded {@code nodeId}s for
         * the WHOLE grid (Cliff C5, one field regardless of depth/group - a node's expansion is its own property).
         * Toggled by {@link #buildExpandToggleHandler(Location, String)}, read by both {@link #buildNode(Location)}
         * and the {@code getSubComponents} traversal above.
         */
        private String expandedNodesFieldKey(Location location)
        {
            return "treetable." + String.join(".", location.get()) + ".expandedNodes";
        }

        private Set<String> readExpandedNodeIds(Location location)
        {
            if (location == null)
            {
                return Collections.emptySet();
            }
            return new LinkedHashSet<>(this.submittedData.getFieldValue(this.expandedNodesFieldKey(location))
                                                         .map(Value::asStringList)
                                                         .orElse(Collections.emptyList()));
        }

        // ------------------------------------------------------------------------------------------------------
        // Slice 6 - per-column filter + multi-column sort
        // ------------------------------------------------------------------------------------------------------

        /**
         * Deterministic submitted-{@link Data} field key carrying ONE column's raw filter text (Slice 6, Cliff C1a
         * mechanism (b)): unlike the window/expanded-set fields (mutated server-side by a handler), this field is
         * written DIRECTLY by the frontend before firing the filter control's {@link Target} - the value already
         * lives here by the time {@link #buildFilterChangeHandler()}'s identity handler runs.
         */
        private String filterFieldKey(Location gridLocation, String columnKey)
        {
            return "treetable." + String.join(".", gridLocation.get()) + ".filter." + columnKey;
        }

        /**
         * Deterministic submitted-{@link Data} field key carrying the WHOLE grid's ordered multi-column sort spec
         * (Slice 6) as a list of {@code "columnKey:DIRECTION"} encoded entries (index 0 = primary) - Data only
         * supports string/string-list values (see {@link Value}), so the ordered {@code (columnKey, direction)}
         * pairs are encoded into this simple colon-joined form. One field for the WHOLE grid, mirroring
         * {@link #expandedNodesFieldKey(Location)}.
         */
        private String sortFieldKey(Location gridLocation)
        {
            return "treetable." + String.join(".", gridLocation.get()) + ".sort";
        }

        private String currentFilterValue(Location gridLocation, String columnKey)
        {
            if (gridLocation == null)
            {
                return null;
            }
            return this.submittedData.getFieldValue(this.filterFieldKey(gridLocation, columnKey))
                                     .map(Value::asString)
                                     .filter(value -> value != null && !value.trim()
                                                                             .isEmpty())
                                     .orElse(null);
        }

        /**
         * Builds one {@link ColumnFilter} per column with a non-empty filter value (empty/blank/absent -&gt; no
         * filter for that column, per AC4). {@link FilterOperator#CONTAINS} is the chosen default operator for text
         * columns (documented decision, Slice 6) - the SPI's {@code FilterOperator} vocabulary stays closed under
         * the Rule of Three until a concrete provider use case needs another operator (e.g. numeric range); nothing
         * in this slice exposes an operator-selection control. Read ONCE per render pass and threaded unchanged into
         * every {@link TreeTableQuery} at every depth (root + every expanded group) - "changing a filter re-queries
         * every visible group through the same {@code provider.fetch}" (interaction decision, Slice 6).
         */
        private List<ColumnFilter> readFilters(Location gridLocation)
        {
            // plan-78: no filters at all when the whole feature is disabled (globally OR because no column is
            // filterable) - naturally makes activeFilterCount 0 and keeps every fetch un-narrowed in that case.
            if (gridLocation == null || !this.effectiveFilterEnabled())
            {
                return Collections.emptyList();
            }
            List<ColumnFilter> filters = new ArrayList<>();
            for (TreeTableColumn column : this.data.getColumns())
            {
                if (!column.isFilterable())
                {
                    // plan-78: a non-filterable column never gets a filter control to type into, so defensively
                    // never build a ColumnFilter for it either, even if a stale submitted value happened to survive.
                    continue;
                }
                String value = this.currentFilterValue(gridLocation, column.getKey());
                if (value != null)
                {
                    filters.add(ColumnFilter.of(column.getKey(), FilterOperator.CONTAINS, value));
                }
            }
            return filters;
        }

        /**
         * plan-82 bugfix (live-testing report: initial sort dropped on the first sort-toggle/reorder click): the
         * SINGLE shared "current effective sort list" derivation, in RAW {@code "columnKey:DIRECTION"} encoded
         * String form, used by BOTH the render path ({@link #readSorts(Location)}) AND the mutating handlers
         * ({@link #buildSortToggleHandler(Location, String)}'s cycle branches, {@link #buildSortPriorityReorderHandler(Location, String)}).
         * Field present in {@code data} (the render path's {@code submittedData}, or a handler's {@code eventData})
         * &mdash; return the raw submitted encoded list; field ABSENT &mdash; seed it from the columns' declared
         * {@link TreeTableColumn#getInitialSortDirection()} (declaration order = priority, non-sortable columns
         * ignored), EXACTLY the plan-79 seed {@link #readSorts(Location)} already applied at render time.
         * <p>
         * Before this fix the two paths disagreed: {@link #readSorts(Location)} seeded the default from column
         * config, but {@code buildSortToggleHandler}/{@code buildSortPriorityReorderHandler} read the sort field
         * straight off their {@code eventData} with a hardcoded {@code Collections.emptyList()} fallback - on the
         * FIRST sort-toggle/reorder click for a grid (the sort field is still ABSENT from the submitted
         * {@link Data} then, exactly like the very first render) the handler silently started from an EMPTY list,
         * discarding the seeded initial sort instead of building on top of it (e.g. seeded Name DESC + click Owner
         * in multi-column mode collapsed to [Owner ASC] instead of [Name DESC, Owner ASC]). Routing every reader
         * through this one helper keeps the two traversals in agreement (the same class of bug as
         * {@code react4j-routing-key-must-be-stable-and-unique-use-positional-id}: two independent derivations of
         * the same thing must not be allowed to disagree).
         */
        private List<String> effectiveEncodedSorts(Data data, Location gridLocation)
        {
            return data.getFieldValue(this.sortFieldKey(gridLocation))
                       .map(value -> value.asStringList())
                       .orElseGet(() -> this.data.getColumns()
                                                 .stream()
                                                 .filter(TreeTableColumn::isSortable)
                                                 .filter(column -> column.getInitialSortDirection()
                                                                         .isPresent())
                                                 .map(column -> column.getKey() + ":" + column.getInitialSortDirection()
                                                                                              .get()
                                                                                              .name())
                                                 .collect(Collectors.toList()));
        }

        /**
         * Decodes the grid's ordered multi-column sort spec (Slice 6) back into {@link SortColumn}s, preserving
         * click order (index 0 = primary). Malformed/stale entries (e.g. a removed column key surviving a client
         * cache) are skipped defensively rather than failing the whole render.
         * <p>
         * plan-79 (per-column INITIAL sort direction): when the sort field is ABSENT from the submitted {@link Data}
         * &mdash; i.e. the very first render, before the user has ever clicked a sort toggle for this grid &mdash;
         * the ordered default sort list is seeded from {@link TreeTableColumn#getInitialSortDirection()} (via
         * {@link #effectiveEncodedSorts(Data, Location)}, plan-82) instead of decoded from a submitted value. This
         * is a strictly READ-TIME default, exactly like {@code currentFiltersVisible}'s {@code isFiltersInitiallyVisible()}
         * fallback &mdash; it is never written back into {@code Data}, so once the user clicks ANY sort toggle the
         * field becomes present (even as an empty list after cycling off) and the submitted value wins from then
         * on; other round trips that never touch the sort field (expand/filter/load-more) keep re-deriving this
         * same seed until the user's first sort click.
         */
        private List<SortColumn> readSorts(Location gridLocation)
        {
            // plan-78: no sorts at all when sorting is globally disabled.
            if (gridLocation == null || !this.data.isSortEnabled())
            {
                return Collections.emptyList();
            }
            // plan-78: a column whose own sortable flag is false never gets a sort toggle to click, so defensively
            // skip its entry too, even if a stale submitted value happened to survive a config change.
            Set<String> sortableColumnKeys = this.data.getColumns()
                                                      .stream()
                                                      .filter(TreeTableColumn::isSortable)
                                                      .map(TreeTableColumn::getKey)
                                                      .collect(Collectors.toSet());
            List<String> encoded = this.effectiveEncodedSorts(this.submittedData, gridLocation);
            List<SortColumn> sorts = new ArrayList<>();
            for (String entry : encoded)
            {
                String[] parts = entry.split(":", 2);
                if (parts.length == 2 && sortableColumnKeys.contains(parts[0]))
                {
                    try
                    {
                        sorts.add(SortColumn.of(parts[0], SortDirection.valueOf(parts[1])));
                    }
                    catch (IllegalArgumentException e)
                    {
                        // ignore a malformed/stale encoded entry defensively rather than failing the whole render
                    }
                }
            }
            return sorts;
        }

        /**
         * Identity pass-through handler for a column's filter control (Slice 6, Cliff C1a mechanism (b)): the raw
         * filter text already lives in the submitted {@link Data} under {@link #filterFieldKey(Location, String)} by
         * the time this fires (the frontend writes it directly), so no server-side mutation is needed - the
         * registered handler exists purely so the filter control's {@link Target} resolves to something (a legal,
         * non-bare-null {@code /ui/event} round trip, avoiding the plan-29 regression class).
         */
        private DataEventHandler buildFilterChangeHandler()
        {
            return (eventData, internalData) -> DataEventHandler.MappedData.builder()
                                                                           .data(eventData)
                                                                           .internalData(internalData)
                                                                           .build();
        }

        /**
         * Builds one column's sort-toggle click handler (Slice 6, branched by plan-81's
         * {@code TreeTable.withMultiColumnSortEnabled(boolean)}): cycles ASCENDING -&gt; DESCENDING -&gt; none,
         * mirroring {@link #buildExpandToggleHandler(Location, String)}'s server-side-computed-toggle shape.
         * <ul>
         * <li>multi-column mode &mdash; newly-sorted columns are APPENDED to the ordered spec (so click order
         * determines primary/secondary/...); toggling an ALREADY-sorted column's direction keeps its existing
         * position (does not move it to the end); cycling off (DESCENDING -&gt; none) removes its entry
         * entirely.</li>
         * <li>single-column mode (default) &mdash; a newly-sorted column REPLACES the whole ordered spec with
         * itself alone (clearing whatever other column was previously active); toggling the ALREADY-sole-active
         * column cycles its own direction in place; cycling off empties the list.</li>
         * </ul>
         */
        private DataEventHandler buildSortToggleHandler(Location gridLocation, String columnKey)
        {
            String fieldKey = this.sortFieldKey(gridLocation);
            boolean multiColumnSortEnabled = this.data.isMultiColumnSortEnabled();
            return (eventData, internalData) ->
            {
                // plan-82 bugfix: build on the SAME seeded default (effectiveEncodedSorts) the render path already
                // uses when the sort field is still absent - not a hardcoded empty list - so the FIRST sort-toggle
                // click for this grid builds on top of any columns' declared initial sort direction instead of
                // discarding it.
                List<String> current = this.effectiveEncodedSorts(eventData, gridLocation);
                List<String> updated = multiColumnSortEnabled ? this.cycleMultiColumnSort(current, columnKey) : this.cycleSingleColumnSort(current, columnKey);
                eventData.setFieldValue(fieldKey, updated);
                return DataEventHandler.MappedData.builder()
                                                  .data(eventData)
                                                  .internalData(internalData)
                                                  .build();
            };
        }

        /**
         * Multi-column accumulate cycle (unchanged Slice 6 behavior): see
         * {@link #buildSortToggleHandler(Location, String)}'s multi-column-mode bullet.
         */
        private List<String> cycleMultiColumnSort(List<String> current, String columnKey)
        {
            List<String> updated = new ArrayList<>(current);
            int existingIndex = -1;
            SortDirection existingDirection = null;
            for (int i = 0; i < updated.size(); i++)
            {
                String[] parts = updated.get(i)
                                        .split(":", 2);
                if (parts.length == 2 && parts[0].equals(columnKey))
                {
                    existingIndex = i;
                    existingDirection = SortDirection.valueOf(parts[1]);
                    break;
                }
            }
            if (existingIndex < 0)
            {
                updated.add(columnKey + ":" + SortDirection.ASCENDING.name());
            }
            else if (existingDirection == SortDirection.ASCENDING)
            {
                updated.set(existingIndex, columnKey + ":" + SortDirection.DESCENDING.name());
            }
            else
            {
                updated.remove(existingIndex);
            }
            return updated;
        }

        /**
         * Single-column replace cycle (plan-81 default): see
         * {@link #buildSortToggleHandler(Location, String)}'s single-column-mode bullet. The resulting list is
         * ALWAYS either empty or exactly one entry for {@code columnKey} - any other column's entry that may have
         * been present in {@code current} is dropped, never carried forward.
         */
        private List<String> cycleSingleColumnSort(List<String> current, String columnKey)
        {
            SortDirection existingDirection = null;
            for (String entry : current)
            {
                String[] parts = entry.split(":", 2);
                if (parts.length == 2 && parts[0].equals(columnKey))
                {
                    existingDirection = SortDirection.valueOf(parts[1]);
                    break;
                }
            }
            List<String> updated = new ArrayList<>();
            if (existingDirection == SortDirection.ASCENDING)
            {
                updated.add(columnKey + ":" + SortDirection.DESCENDING.name());
            }
            else if (existingDirection != SortDirection.DESCENDING)
            {
                // absent, or a DIFFERENT column was the sole active one - this click makes columnKey the sole
                // active column, ASCENDING, clearing whatever else was active.
                updated.add(columnKey + ":" + SortDirection.ASCENDING.name());
            }
            // existingDirection == DESCENDING -> cycle off: updated stays empty.
            return updated;
        }

        /**
         * Deterministic submitted-{@link Data} field key carrying the new 1-based priority the user chose for ONE
         * column via the priority re-rank {@code <select>} (plan-81, Cliff C1a mechanism (b) &mdash; mirrors
         * {@link #filterFieldKey(Location, String)}: the frontend writes the raw chosen value directly before
         * firing {@link #buildSortPriorityReorderHandler(Location, String)}'s {@link Target}).
         */
        private String sortPriorityFieldKey(Location gridLocation, String columnKey)
        {
            return "treetable." + String.join(".", gridLocation.get()) + ".sortpriority." + columnKey;
        }

        /**
         * Builds one column's priority re-rank click handler (plan-81): reads the 1-based priority written under
         * {@link #sortPriorityFieldKey(Location, String)} for {@code columnKey} and moves that column's entry to
         * the {@code (priority - 1)}-th position of the ordered {@link #sortFieldKey(Location)} list, shifting the
         * others while preserving their relative order and every column's encoded direction (a plain
         * remove-then-insert on the encoded string list). Defensively a no-op &mdash; leaves the sort list
         * untouched &mdash; when: the column is not currently present in the sort list; no priority value was
         * submitted; or the submitted priority is out of the valid {@code 1..size} range or equal to the column's
         * current position already (clamp/ignore, never throws).
         */
        private DataEventHandler buildSortPriorityReorderHandler(Location gridLocation, String columnKey)
        {
            String sortFieldKey = this.sortFieldKey(gridLocation);
            String priorityFieldKey = this.sortPriorityFieldKey(gridLocation, columnKey);
            return (eventData, internalData) ->
            {
                // plan-82 bugfix: same shared seeded-default derivation as buildSortToggleHandler - a reorder click
                // fired before the sort field was ever written must still operate on the seeded initial sort list,
                // not an empty one.
                List<String> current = new ArrayList<>(this.effectiveEncodedSorts(eventData, gridLocation));
                int currentIndex = this.indexOfEncodedSort(current, columnKey);
                Optional<Integer> requestedPriority = eventData.getFieldValue(priorityFieldKey)
                                                               .map(Value::asInteger);
                if (currentIndex >= 0 && requestedPriority.isPresent())
                {
                    int targetIndex = requestedPriority.get() - 1;
                    if (targetIndex >= 0 && targetIndex < current.size() && targetIndex != currentIndex)
                    {
                        String entry = current.remove(currentIndex);
                        current.add(targetIndex, entry);
                        eventData.setFieldValue(sortFieldKey, current);
                    }
                    // else: out-of-range or a no-op (already at that position) - ignore defensively.
                }
                return DataEventHandler.MappedData.builder()
                                                  .data(eventData)
                                                  .internalData(internalData)
                                                  .build();
            };
        }

        /**
         * Analogous to {@link #indexOfSort(List, String)} but operating on the raw {@code "columnKey:DIRECTION"}
         * encoded list (as carried in submitted {@link Data}), used by
         * {@link #buildSortPriorityReorderHandler(Location, String)} which must locate a column's position WITHOUT
         * decoding the whole list into {@link SortColumn}s.
         */
        private int indexOfEncodedSort(List<String> encodedSorts, String columnKey)
        {
            for (int i = 0; i < encodedSorts.size(); i++)
            {
                String[] parts = encodedSorts.get(i)
                                             .split(":", 2);
                if (parts.length == 2 && parts[0].equals(columnKey))
                {
                    return i;
                }
            }
            return -1;
        }

        // ------------------------------------------------------------------------------------------------------
        // plan-77 - collapsible filter row (server-owned filtersVisible + funnel toggle)
        // ------------------------------------------------------------------------------------------------------

        /**
         * Deterministic submitted-{@link Data} field key carrying the WHOLE grid's filter-row visibility flag
         * (plan-77), keyed off the grid's own {@link Location} exactly like {@link #expandedNodesFieldKey(Location)} /
         * {@link #sortFieldKey(Location)} - one field for the WHOLE grid, not per-column/per-group.
         */
        private String filtersVisibleFieldKey(Location gridLocation)
        {
            return "treetable." + String.join(".", gridLocation.get()) + ".filtersVisible";
        }

        /**
         * Reads the current filter-row visibility (plan-77), defaulting to the configured
         * {@code TreeTable.withFiltersInitiallyVisible(boolean)} value (plan-78; {@code false} unless overridden) when
         * absent - both at a genuinely-root {@code gridLocation == null} call site and when the field was never
         * toggled.
         */
        private boolean currentFiltersVisible(Location gridLocation)
        {
            if (gridLocation == null)
            {
                return this.data.isFiltersInitiallyVisible();
            }
            return this.submittedData.getFieldValue(this.filtersVisibleFieldKey(gridLocation))
                                     .map(Value::asBoolean)
                                     .orElse(this.data.isFiltersInitiallyVisible());
        }

        /**
         * Builds the funnel toggle's click handler (plan-77): a server-computed FLIP of the shared
         * {@code filtersVisible} boolean field (Cliff C1a mechanism (a), mirrors
         * {@link #buildExpandToggleHandler(Location, String)}'s server-computed-toggle shape) - the frontend fires a
         * plain click with no value to write, unlike the per-column filter inputs (mechanism (b)).
         */
        private DataEventHandler buildFilterToggleHandler(Location gridLocation)
        {
            String fieldKey = this.filtersVisibleFieldKey(gridLocation);
            return (eventData, internalData) ->
            {
                boolean current = eventData.getFieldValue(fieldKey)
                                           .map(Value::asBoolean)
                                           .orElse(false);
                eventData.setFieldValue(fieldKey, !current);
                return DataEventHandler.MappedData.builder()
                                                  .data(eventData)
                                                  .internalData(internalData)
                                                  .build();
            };
        }

        // ------------------------------------------------------------------------------------------------------
        // plan-80 - flat-mode (flatten-the-tree) toggle
        // ------------------------------------------------------------------------------------------------------

        /**
         * Deterministic submitted-{@link Data} field key carrying the WHOLE grid's flat/tree mode flag (plan-80),
         * keyed off the grid's own {@link Location} exactly like {@link #filtersVisibleFieldKey(Location)} - one
         * field for the WHOLE grid.
         */
        private String flatModeFieldKey(Location gridLocation)
        {
            return "treetable." + String.join(".", gridLocation.get()) + ".flatMode";
        }

        /**
         * Reads the current flat/tree mode (plan-80), defaulting to the configured
         * {@code TreeTable.withInitiallyFlat(boolean)} value ({@code false} unless overridden) when absent - both at
         * a genuinely-root {@code gridLocation == null} call site and when the field was never toggled. Mirrors
         * {@link #currentFiltersVisible(Location)} exactly.
         */
        private boolean currentFlatMode(Location gridLocation)
        {
            if (gridLocation == null)
            {
                return this.data.isInitiallyFlat();
            }
            return this.submittedData.getFieldValue(this.flatModeFieldKey(gridLocation))
                                     .map(Value::asBoolean)
                                     .orElse(this.data.isInitiallyFlat());
        }

        /**
         * Builds the flat/tree toggle's click handler (plan-80): a server-computed FLIP of the shared
         * {@code flatMode} boolean field (Cliff C1a mechanism (a), mirrors {@link #buildFilterToggleHandler(Location)}'s
         * server-computed-toggle shape). Unlike {@link #buildFilterToggleHandler(Location)} (whose "current" fallback
         * is hardcoded {@code false}), this handler's "current" fallback is {@code TreeTable.withInitiallyFlat(boolean)}
         * - the SAME default {@link #currentFlatMode(Location)} uses for rendering - so the FIRST click on a table
         * configured {@code withInitiallyFlat(true)} correctly flips it to {@code false} rather than silently
         * re-asserting {@code true} (deliberate, since an initially-flat table's first click must actually switch to
         * tree view).
         */
        private DataEventHandler buildFlatModeToggleHandler(Location gridLocation)
        {
            String fieldKey = this.flatModeFieldKey(gridLocation);
            boolean initiallyFlat = this.data.isInitiallyFlat();
            return (eventData, internalData) ->
            {
                boolean current = eventData.getFieldValue(fieldKey)
                                           .map(Value::asBoolean)
                                           .orElse(initiallyFlat);
                eventData.setFieldValue(fieldKey, !current);
                return DataEventHandler.MappedData.builder()
                                                  .data(eventData)
                                                  .internalData(internalData)
                                                  .build();
            };
        }
    }

    /**
     * Marker interface so {@link TreeTableLoadMoreControlImpl} satisfies the {@code UIC}-bound of
     * {@link AbstractUIComponent} (Cliff C3 requirement, mirrors {@code Pagination.PaginationItem}). Internal-only -
     * never exposed on {@link org.omnaest.react4j.component.treetable.TreeTable}.
     */
    private interface TreeTableLoadMoreControl extends UIComponent<TreeTableLoadMoreControl>
    {
    }

    /**
     * Distinct sub-{@link UIComponent} for the load-more control (Cliff C3 pattern, mirrors
     * {@code PaginationItemImpl}): a click needs its OWN routable {@link Target}/{@link Location} (resolved to the
     * SAME {@code "loadmore"}-suffixed positional Location {@link TreeTableGridComponent#buildLoadMoreNode} embeds)
     * AND its OWN {@code manageEventHandler} registration, discovered only through
     * {@link TreeTableGridComponent#getSubComponents(Location)}.
     * <p>
     * {@link #render(RenderingProcessor, Location, Optional)} is never invoked in this slice: the emitted
     * {@code loadMore} JSON fields are still built directly by {@code TreeTableGridComponent.buildLoadMoreNode(...)}
     * (Slice 2 shortcut, unchanged here) - nothing calls {@code renderingProcessor.process(...)} on this
     * subcomponent, mirroring the row-Location shortcut already in place for Slice 2/5.
     */
    private static class TreeTableLoadMoreControlImpl extends AbstractUIComponent<TreeTableLoadMoreControl> implements TreeTableLoadMoreControl
    {
        private final DataEventHandler eventHandler;

        TreeTableLoadMoreControlImpl(ComponentContext context, DataEventHandler eventHandler)
        {
            super(context);
            this.eventHandler = eventHandler;
        }

        @Override
        public UIComponentRenderer asRenderer()
        {
            return new UIComponentRenderer() {
                @Override
                public Location getLocation(LocationSupport locationSupport)
                {
                    return locationSupport.createLocation("loadmore");
                }

                @Override
                public Node render(RenderingProcessor renderingProcessor, Location location, Optional<Data> data)
                {
                    return null;
                }

                @Override
                public void manageNodeRenderers(NodeRendererRegistry registry)
                {
                    // no server-side HTML fallback template this slice
                }

                @Override
                public void manageEventHandler(EventHandlerRegistrationSupport eventHandlerRegistrationSupport)
                {
                    if (TreeTableLoadMoreControlImpl.this.eventHandler != null)
                    {
                        eventHandlerRegistrationSupport.register(TreeTableLoadMoreControlImpl.this.eventHandler);
                    }
                }

                @Override
                public Stream<ParentLocationAndComponent> getSubComponents(Location parentLocation)
                {
                    return Stream.empty();
                }
            };
        }

        @Override
        public UIComponentProvider<TreeTableLoadMoreControl> asTemplateProvider()
        {
            return () -> new TreeTableLoadMoreControlImpl(this.context, this.eventHandler);
        }
    }

    /**
     * Marker interface so {@link TreeTableRowExpandControlImpl} satisfies the {@code UIC}-bound of
     * {@link AbstractUIComponent} (Cliff C3 requirement, mirrors {@code Pagination.PaginationItem}). Internal-only -
     * never exposed on {@link org.omnaest.react4j.component.treetable.TreeTable}.
     */
    private interface TreeTableRowExpandControl extends UIComponent<TreeTableRowExpandControl>
    {
    }

    /**
     * Distinct sub-{@link UIComponent} for ONE expandable row's expand/collapse control (Cliff C3 pattern +
     * {@code react4j-per-item-server-handler-needs-distinct-subcomponent}): a click needs its OWN routable
     * {@link Target}/{@link Location} - resolved to the SAME {@code "row" + flatIndex} positional {@link Location}
     * {@link TreeTableGridComponent#createRowLocation(Location, int)} embeds as the row's {@code target} field, i.e.
     * this control's {@link Location} equals the row's own {@link Location} (no extra {@code "expand"} segment -
     * the row IS the caret's click target) - AND its OWN {@code manageEventHandler} registration, discovered only
     * through {@link TreeTableGridComponent#getSubComponents(Location)}. Only constructed for rows where
     * {@link TreeTableRow#isExpandable()} is {@code true} (gate at the call site, mirrors
     * {@code react4j-gate-optin-handler-on-existing-node}); {@link #manageEventHandler} additionally gates on a
     * non-null handler defensively, mirroring {@link TreeTableLoadMoreControlImpl}.
     * <p>
     * {@link #render(RenderingProcessor, Location, Optional)} is never invoked: the emitted row JSON is still built
     * directly by {@code TreeTableGridComponent.appendRows(...)}, mirroring the load-more control's shortcut.
     */
    private static class TreeTableRowExpandControlImpl extends AbstractUIComponent<TreeTableRowExpandControl> implements TreeTableRowExpandControl
    {
        private final int              flatIndex;
        private final DataEventHandler eventHandler;

        TreeTableRowExpandControlImpl(ComponentContext context, int flatIndex, DataEventHandler eventHandler)
        {
            super(context);
            this.flatIndex = flatIndex;
            this.eventHandler = eventHandler;
        }

        @Override
        public UIComponentRenderer asRenderer()
        {
            return new UIComponentRenderer() {
                @Override
                public Location getLocation(LocationSupport locationSupport)
                {
                    return locationSupport.createLocation("row" + TreeTableRowExpandControlImpl.this.flatIndex);
                }

                @Override
                public Node render(RenderingProcessor renderingProcessor, Location location, Optional<Data> data)
                {
                    return null;
                }

                @Override
                public void manageNodeRenderers(NodeRendererRegistry registry)
                {
                    // no server-side HTML fallback template this slice
                }

                @Override
                public void manageEventHandler(EventHandlerRegistrationSupport eventHandlerRegistrationSupport)
                {
                    if (TreeTableRowExpandControlImpl.this.eventHandler != null)
                    {
                        eventHandlerRegistrationSupport.register(TreeTableRowExpandControlImpl.this.eventHandler);
                    }
                }

                @Override
                public Stream<ParentLocationAndComponent> getSubComponents(Location parentLocation)
                {
                    return Stream.empty();
                }
            };
        }

        @Override
        public UIComponentProvider<TreeTableRowExpandControl> asTemplateProvider()
        {
            return () -> new TreeTableRowExpandControlImpl(this.context, this.flatIndex, this.eventHandler);
        }
    }

    /**
     * Marker interface so {@link TreeTableFilterControlImpl} satisfies the {@code UIC}-bound of
     * {@link AbstractUIComponent} (Cliff C3 requirement, mirrors {@code Pagination.PaginationItem}). Internal-only -
     * never exposed on {@link org.omnaest.react4j.component.treetable.TreeTable}.
     */
    private interface TreeTableFilterControl extends UIComponent<TreeTableFilterControl>
    {
    }

    /**
     * Distinct sub-{@link UIComponent} for ONE column's filter control (Slice 6, Cliff C3 pattern +
     * {@code react4j-per-item-server-handler-needs-distinct-subcomponent}): its OWN routable {@link Target}/
     * {@link Location} - resolved to the SAME {@code "filter" + columnIndex} positional {@link Location}
     * {@link TreeTableGridComponent#createFilterOrSortLocation(Location, String)} embeds as the column's
     * {@code filterTarget} field - AND its OWN {@code manageEventHandler} registration (an identity pass-through,
     * see {@link TreeTableGridComponent#buildFilterChangeHandler()}), discovered only through
     * {@link TreeTableGridComponent#getSubComponents(Location)}. Present at INITIAL render for EVERY column
     * unconditionally (no gate) - contrast with the row-expand control, which is gated on
     * {@code row.isExpandable()}.
     * <p>
     * {@link #render(RenderingProcessor, Location, Optional)} is never invoked: the emitted column JSON is still
     * built directly by {@code TreeTableGridComponent.buildColumnNodes(...)}, mirroring the load-more/row-expand
     * controls' shortcut.
     */
    private static class TreeTableFilterControlImpl extends AbstractUIComponent<TreeTableFilterControl> implements TreeTableFilterControl
    {
        private final int              columnIndex;
        private final DataEventHandler eventHandler;

        TreeTableFilterControlImpl(ComponentContext context, int columnIndex, DataEventHandler eventHandler)
        {
            super(context);
            this.columnIndex = columnIndex;
            this.eventHandler = eventHandler;
        }

        @Override
        public UIComponentRenderer asRenderer()
        {
            return new UIComponentRenderer() {
                @Override
                public Location getLocation(LocationSupport locationSupport)
                {
                    return locationSupport.createLocation("filter" + TreeTableFilterControlImpl.this.columnIndex);
                }

                @Override
                public Node render(RenderingProcessor renderingProcessor, Location location, Optional<Data> data)
                {
                    return null;
                }

                @Override
                public void manageNodeRenderers(NodeRendererRegistry registry)
                {
                    // no server-side HTML fallback template this slice
                }

                @Override
                public void manageEventHandler(EventHandlerRegistrationSupport eventHandlerRegistrationSupport)
                {
                    if (TreeTableFilterControlImpl.this.eventHandler != null)
                    {
                        eventHandlerRegistrationSupport.register(TreeTableFilterControlImpl.this.eventHandler);
                    }
                }

                @Override
                public Stream<ParentLocationAndComponent> getSubComponents(Location parentLocation)
                {
                    return Stream.empty();
                }
            };
        }

        @Override
        public UIComponentProvider<TreeTableFilterControl> asTemplateProvider()
        {
            return () -> new TreeTableFilterControlImpl(this.context, this.columnIndex, this.eventHandler);
        }
    }

    /**
     * Marker interface so {@link TreeTableSortToggleControlImpl} satisfies the {@code UIC}-bound of
     * {@link AbstractUIComponent} (Cliff C3 requirement, mirrors {@code Pagination.PaginationItem}). Internal-only -
     * never exposed on {@link org.omnaest.react4j.component.treetable.TreeTable}.
     */
    private interface TreeTableSortToggleControl extends UIComponent<TreeTableSortToggleControl>
    {
    }

    /**
     * Distinct sub-{@link UIComponent} for ONE column's sort toggle (Slice 6, Cliff C3 pattern +
     * {@code react4j-per-item-server-handler-needs-distinct-subcomponent}): its OWN routable {@link Target}/
     * {@link Location} - resolved to the SAME {@code "sort" + columnIndex} positional {@link Location}
     * {@link TreeTableGridComponent#createFilterOrSortLocation(Location, String)} embeds as the column's
     * {@code sortTarget} field - AND its OWN {@code manageEventHandler} registration (the ASCENDING -&gt;
     * DESCENDING -&gt; none cycle, see {@link TreeTableGridComponent#buildSortToggleHandler(Location, String)}),
     * discovered only through {@link TreeTableGridComponent#getSubComponents(Location)}. Present at INITIAL render
     * for EVERY column unconditionally (no gate), mirroring {@link TreeTableFilterControlImpl}.
     * <p>
     * {@link #render(RenderingProcessor, Location, Optional)} is never invoked: the emitted column JSON is still
     * built directly by {@code TreeTableGridComponent.buildColumnNodes(...)}, mirroring the load-more/row-expand
     * controls' shortcut.
     */
    private static class TreeTableSortToggleControlImpl extends AbstractUIComponent<TreeTableSortToggleControl> implements TreeTableSortToggleControl
    {
        private final int              columnIndex;
        private final DataEventHandler eventHandler;

        TreeTableSortToggleControlImpl(ComponentContext context, int columnIndex, DataEventHandler eventHandler)
        {
            super(context);
            this.columnIndex = columnIndex;
            this.eventHandler = eventHandler;
        }

        @Override
        public UIComponentRenderer asRenderer()
        {
            return new UIComponentRenderer() {
                @Override
                public Location getLocation(LocationSupport locationSupport)
                {
                    return locationSupport.createLocation("sort" + TreeTableSortToggleControlImpl.this.columnIndex);
                }

                @Override
                public Node render(RenderingProcessor renderingProcessor, Location location, Optional<Data> data)
                {
                    return null;
                }

                @Override
                public void manageNodeRenderers(NodeRendererRegistry registry)
                {
                    // no server-side HTML fallback template this slice
                }

                @Override
                public void manageEventHandler(EventHandlerRegistrationSupport eventHandlerRegistrationSupport)
                {
                    if (TreeTableSortToggleControlImpl.this.eventHandler != null)
                    {
                        eventHandlerRegistrationSupport.register(TreeTableSortToggleControlImpl.this.eventHandler);
                    }
                }

                @Override
                public Stream<ParentLocationAndComponent> getSubComponents(Location parentLocation)
                {
                    return Stream.empty();
                }
            };
        }

        @Override
        public UIComponentProvider<TreeTableSortToggleControl> asTemplateProvider()
        {
            return () -> new TreeTableSortToggleControlImpl(this.context, this.columnIndex, this.eventHandler);
        }
    }

    /**
     * Marker interface so {@link TreeTableSortPriorityControlImpl} satisfies the {@code UIC}-bound of
     * {@link AbstractUIComponent} (Cliff C3 requirement, mirrors {@code Pagination.PaginationItem}). Internal-only -
     * never exposed on {@link org.omnaest.react4j.component.treetable.TreeTable}.
     */
    private interface TreeTableSortPriorityControl extends UIComponent<TreeTableSortPriorityControl>
    {
    }

    /**
     * Distinct sub-{@link UIComponent} for ONE column's priority re-rank control (plan-81, Cliff C3 pattern +
     * {@code react4j-per-item-server-handler-needs-distinct-subcomponent}): its OWN routable {@link Target}/
     * {@link Location} - resolved to the SAME {@code "sortpriority" + columnIndex} positional {@link Location}
     * {@link TreeTableGridComponent#createFilterOrSortLocation(Location, String)} embeds as the column's
     * {@code sortPriorityReorderTarget} field - AND its OWN {@code manageEventHandler} registration (the
     * remove-then-insert reorder, see {@link TreeTableGridComponent#buildSortPriorityReorderHandler(Location, String)}),
     * discovered only through {@link TreeTableGridComponent#getSubComponents(Location)}. Unlike
     * {@link TreeTableFilterControlImpl}/{@link TreeTableSortToggleControlImpl} (present unconditionally at initial
     * render), this control is gated on DATA state - only constructed for a column that is BOTH sortable AND
     * currently active in the ordered sort list (mirrors the row-expand control's {@code row.isExpandable()} gate,
     * {@code react4j-gate-optin-handler-on-existing-node}) - AND only when multi-column sort is enabled (build-time
     * config, safe to gate the registration walk on).
     * <p>
     * {@link #render(RenderingProcessor, Location, Optional)} is never invoked: the emitted column JSON is still
     * built directly by {@code TreeTableGridComponent.buildColumnNodes(...)}, mirroring the load-more/row-expand/
     * filter/sort controls' shortcut.
     */
    private static class TreeTableSortPriorityControlImpl extends AbstractUIComponent<TreeTableSortPriorityControl> implements TreeTableSortPriorityControl
    {
        private final int              columnIndex;
        private final DataEventHandler eventHandler;

        TreeTableSortPriorityControlImpl(ComponentContext context, int columnIndex, DataEventHandler eventHandler)
        {
            super(context);
            this.columnIndex = columnIndex;
            this.eventHandler = eventHandler;
        }

        @Override
        public UIComponentRenderer asRenderer()
        {
            return new UIComponentRenderer() {
                @Override
                public Location getLocation(LocationSupport locationSupport)
                {
                    return locationSupport.createLocation("sortpriority" + TreeTableSortPriorityControlImpl.this.columnIndex);
                }

                @Override
                public Node render(RenderingProcessor renderingProcessor, Location location, Optional<Data> data)
                {
                    return null;
                }

                @Override
                public void manageNodeRenderers(NodeRendererRegistry registry)
                {
                    // no server-side HTML fallback template this slice
                }

                @Override
                public void manageEventHandler(EventHandlerRegistrationSupport eventHandlerRegistrationSupport)
                {
                    if (TreeTableSortPriorityControlImpl.this.eventHandler != null)
                    {
                        eventHandlerRegistrationSupport.register(TreeTableSortPriorityControlImpl.this.eventHandler);
                    }
                }

                @Override
                public Stream<ParentLocationAndComponent> getSubComponents(Location parentLocation)
                {
                    return Stream.empty();
                }
            };
        }

        @Override
        public UIComponentProvider<TreeTableSortPriorityControl> asTemplateProvider()
        {
            return () -> new TreeTableSortPriorityControlImpl(this.context, this.columnIndex, this.eventHandler);
        }
    }

    /**
     * Marker interface so {@link TreeTableFilterToggleControlImpl} satisfies the {@code UIC}-bound of
     * {@link AbstractUIComponent} (Cliff C3 requirement, mirrors {@code Pagination.PaginationItem}). Internal-only -
     * never exposed on {@link org.omnaest.react4j.component.treetable.TreeTable}.
     */
    private interface TreeTableFilterToggleControl extends UIComponent<TreeTableFilterToggleControl>
    {
    }

    /**
     * Distinct sub-{@link UIComponent} for the WHOLE grid's funnel filter-visibility toggle (plan-77, Cliff C3
     * pattern + {@code react4j-per-item-server-handler-needs-distinct-subcomponent}): its OWN routable
     * {@link Target}/{@link Location} - resolved to the SAME {@code "filtertoggle"} positional {@link Location}
     * {@link TreeTableGridComponent#createFilterOrSortLocation(Location, String)} embeds as the top-level
     * {@code filterToggleTarget} field - AND its OWN {@code manageEventHandler} registration (a server-computed
     * boolean FLIP, see {@link TreeTableGridComponent#buildFilterToggleHandler(Location)}), discovered only through
     * {@link TreeTableGridComponent#getSubComponents(Location)}. Present at INITIAL render unconditionally (no gate,
     * per {@code react4j-gate-optin-handler-on-existing-node} - contrast with the row-expand control), mirroring
     * {@link TreeTableFilterControlImpl}/{@link TreeTableSortToggleControlImpl}. Unlike those two, there is exactly
     * ONE instance per grid (not one per column) since {@code filtersVisible} is a single whole-grid flag.
     * <p>
     * {@link #render(RenderingProcessor, Location, Optional)} is never invoked: the emitted {@code filterToggleTarget}
     * field is still built directly by {@code TreeTableGridComponent.buildNode(...)}, mirroring the load-more/
     * row-expand/filter/sort controls' shortcut.
     */
    private static class TreeTableFilterToggleControlImpl extends AbstractUIComponent<TreeTableFilterToggleControl> implements TreeTableFilterToggleControl
    {
        private final DataEventHandler eventHandler;

        TreeTableFilterToggleControlImpl(ComponentContext context, DataEventHandler eventHandler)
        {
            super(context);
            this.eventHandler = eventHandler;
        }

        @Override
        public UIComponentRenderer asRenderer()
        {
            return new UIComponentRenderer() {
                @Override
                public Location getLocation(LocationSupport locationSupport)
                {
                    return locationSupport.createLocation("filtertoggle");
                }

                @Override
                public Node render(RenderingProcessor renderingProcessor, Location location, Optional<Data> data)
                {
                    return null;
                }

                @Override
                public void manageNodeRenderers(NodeRendererRegistry registry)
                {
                    // no server-side HTML fallback template this slice
                }

                @Override
                public void manageEventHandler(EventHandlerRegistrationSupport eventHandlerRegistrationSupport)
                {
                    if (TreeTableFilterToggleControlImpl.this.eventHandler != null)
                    {
                        eventHandlerRegistrationSupport.register(TreeTableFilterToggleControlImpl.this.eventHandler);
                    }
                }

                @Override
                public Stream<ParentLocationAndComponent> getSubComponents(Location parentLocation)
                {
                    return Stream.empty();
                }
            };
        }

        @Override
        public UIComponentProvider<TreeTableFilterToggleControl> asTemplateProvider()
        {
            return () -> new TreeTableFilterToggleControlImpl(this.context, this.eventHandler);
        }
    }

    /**
     * Marker interface so {@link TreeTableFlatModeToggleControlImpl} satisfies the {@code UIC}-bound of
     * {@link AbstractUIComponent} (Cliff C3 requirement, mirrors {@code Pagination.PaginationItem}). Internal-only -
     * never exposed on {@link org.omnaest.react4j.component.treetable.TreeTable}.
     */
    private interface TreeTableFlatModeToggleControl extends UIComponent<TreeTableFlatModeToggleControl>
    {
    }

    /**
     * Distinct sub-{@link UIComponent} for the WHOLE grid's flat/tree toggle (plan-80, Cliff C3 pattern +
     * {@code react4j-per-item-server-handler-needs-distinct-subcomponent}): its OWN routable {@link Target}/
     * {@link Location} - resolved to the SAME {@code "flattoggle"} positional {@link Location}
     * {@link TreeTableGridComponent#createFilterOrSortLocation(Location, String)} embeds as the top-level
     * {@code flatToggleTarget} field - AND its OWN {@code manageEventHandler} registration (a server-computed
     * boolean FLIP, see {@link TreeTableGridComponent#buildFlatModeToggleHandler(Location)}), discovered only
     * through {@link TreeTableGridComponent#getSubComponents(Location)}. Present at INITIAL render unconditionally
     * (no gate, per {@code react4j-gate-optin-handler-on-existing-node}), but ONLY when
     * {@code TreeTable.withFlatModeToggleEnabled(true)} was configured - mirroring
     * {@link TreeTableFilterToggleControlImpl}'s {@code filterEnabled} gate exactly (a build-time, constant-across-
     * renders gate is safe on the registration walk). Exactly ONE instance per grid, mirroring
     * {@link TreeTableFilterToggleControlImpl}.
     * <p>
     * {@link #render(RenderingProcessor, Location, Optional)} is never invoked: the emitted {@code flatToggleTarget}
     * field is still built directly by {@code TreeTableGridComponent.buildNode(...)}, mirroring the filter-toggle/
     * row-expand/filter/sort controls' shortcut.
     */
    private static class TreeTableFlatModeToggleControlImpl extends AbstractUIComponent<TreeTableFlatModeToggleControl> implements TreeTableFlatModeToggleControl
    {
        private final DataEventHandler eventHandler;

        TreeTableFlatModeToggleControlImpl(ComponentContext context, DataEventHandler eventHandler)
        {
            super(context);
            this.eventHandler = eventHandler;
        }

        @Override
        public UIComponentRenderer asRenderer()
        {
            return new UIComponentRenderer() {
                @Override
                public Location getLocation(LocationSupport locationSupport)
                {
                    return locationSupport.createLocation("flattoggle");
                }

                @Override
                public Node render(RenderingProcessor renderingProcessor, Location location, Optional<Data> data)
                {
                    return null;
                }

                @Override
                public void manageNodeRenderers(NodeRendererRegistry registry)
                {
                    // no server-side HTML fallback template this slice
                }

                @Override
                public void manageEventHandler(EventHandlerRegistrationSupport eventHandlerRegistrationSupport)
                {
                    if (TreeTableFlatModeToggleControlImpl.this.eventHandler != null)
                    {
                        eventHandlerRegistrationSupport.register(TreeTableFlatModeToggleControlImpl.this.eventHandler);
                    }
                }

                @Override
                public Stream<ParentLocationAndComponent> getSubComponents(Location parentLocation)
                {
                    return Stream.empty();
                }
            };
        }

        @Override
        public UIComponentProvider<TreeTableFlatModeToggleControl> asTemplateProvider()
        {
            return () -> new TreeTableFlatModeToggleControlImpl(this.context, this.eventHandler);
        }
    }
}
