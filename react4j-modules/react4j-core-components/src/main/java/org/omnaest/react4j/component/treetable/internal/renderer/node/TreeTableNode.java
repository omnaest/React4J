package org.omnaest.react4j.component.treetable.internal.renderer.node;

import java.util.List;
import java.util.Map;

import org.omnaest.react4j.component.treetable.provider.SortColumn;
import org.omnaest.react4j.service.internal.handler.domain.Target;
import org.omnaest.react4j.service.internal.nodes.AbstractNode;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;

/**
 * Flattened tree-table node (Cliff C4): the tree is NOT emitted as a recursively-nested node structure, but as an ordered
 * list of {@link RowEntryNode}s (each carrying its own {@code depth}), so it maps onto a real {@code <table><tbody>} and
 * every row gets its own flat positional {@link Target} (Cliff C3).
 *
 * @author omnaest
 */
@Getter
@Builder
public class TreeTableNode extends AbstractNode
{
    @JsonProperty
    private final String       type = "TREETABLE";

    @JsonProperty
    private List<ColumnNode>   columns;

    @JsonProperty
    private List<RowEntryNode> rows;

    @JsonProperty
    private LoadMoreNode       loadMore;

    /**
     * Whether the frontend should render the per-column filter input row (plan-77 collapsible filter row). Server-owned
     * (Cliff C5 pattern, {@code react4j-server-driven-overlay-state-boolean-field}): carried in submitted
     * {@link org.omnaest.react4j.domain.context.data.Data}, defaults to the configured
     * {@code TreeTable.withFiltersInitiallyVisible(boolean)} value ({@code false} unless overridden) when the submitted
     * field is absent, flipped by the funnel toggle control's server-computed handler. The per-column filter data
     * ({@code filterValue}/{@code filterFieldKey}/{@code filterTarget} on {@link ColumnNode}) keeps being emitted
     * regardless of this flag - hiding the row must never clear active filters; the FRONTEND decides whether to render
     * the input row based on this field. Forced to {@code false} whenever {@link #filterEnabled} is {@code false} - there
     * is no filter row to show at all in that case.
     */
    @JsonProperty
    private boolean            filtersVisible;

    /**
     * Whether the per-column filtering feature is enabled at all (build-time component configuration, constant across
     * renders): {@code true} unless the app called {@code TreeTable.withFilterEnabled(false)}, or no configured column
     * is filterable ({@link ColumnNode#isFilterable()} on every column is {@code false}) - either condition collapses
     * the whole feature (no funnel toggle, no filter row, no per-column filter controls). The frontend gates all
     * filter-row rendering on this flag.
     */
    @JsonProperty
    private boolean            filterEnabled;

    /**
     * Whether per-column sorting is enabled at all (build-time component configuration, constant across renders):
     * {@code true} unless the app called {@code TreeTable.withSortEnabled(false)}. When {@code false}, no column emits
     * a routable {@code sortTarget} and no sort toggle sub-component is registered.
     */
    @JsonProperty
    private boolean            sortEnabled;

    /**
     * Whether the header stays put while the body scrolls (build-time component configuration, constant across
     * renders): {@code true} when the app called {@code TreeTable.withStickyHeader(true)}. Purely presentational -
     * the server emits the same rows either way.
     */
    @JsonProperty
    private boolean            stickyHeader;

    /**
     * Whether the per-column sort toggle accumulates a multi-column ordered sort spec, or replaces it with a single
     * active column on every click (build-time component configuration, constant across renders, see
     * {@code TreeTable.withMultiColumnSortEnabled(boolean)}, default {@code false} i.e. single-column). Drives
     * whether the frontend renders the priority re-rank {@code <select>} (only meaningful, and only emitted per
     * column, when this is {@code true} &mdash; see {@link ColumnNode#sortPriorityFieldKey}/
     * {@link ColumnNode#sortPriorityReorderTarget}).
     */
    @JsonProperty
    private boolean            multiColumnSortEnabled;

    /**
     * The number of columns currently present in the ordered sort list (equals the size of the {@code sorts} list
     * threaded into every {@code provider.fetch} this render pass) &mdash; {@code 0} when nothing is sorted. Drives
     * the valid {@code 1..activeSortCount} range for the frontend's priority re-rank {@code <select>}.
     */
    @JsonProperty
    private int                activeSortCount;

    /**
     * Number of columns whose submitted filter value is currently non-empty (plan-77) - computed from the SAME
     * submitted-{@link org.omnaest.react4j.domain.context.data.Data} filter values Slice 6 already reads (equals the size
     * of the {@link org.omnaest.react4j.component.treetable.provider.ColumnFilter} list built for this render pass).
     * Drives the frontend's "filters hidden but active" badge when {@link #filtersVisible} is {@code false}. Only
     * ever counts FILTERABLE columns ({@link ColumnNode#isFilterable()}) - a non-filterable column can never have an
     * active filter value in the first place, since it never gets a filter control to type into. Always {@code 0}
     * when {@link #filterEnabled} is {@code false}.
     */
    @JsonProperty
    private int                activeFilterCount;

    /**
     * Routable positional {@link Target} for the funnel show/hide toggle control (plan-77) - its registered handler
     * flips {@link #filtersVisible} in the submitted {@link org.omnaest.react4j.domain.context.data.Data}
     * server-side (mechanism (a), mirrors the sort toggle's server-computed cycle). Routable at initial render
     * whenever {@link #filterEnabled} is {@code true} (like the filter/sort header controls); {@link Target#empty()}
     * (no sub-component registered, nothing to click) whenever {@link #filterEnabled} is {@code false}.
     */
    @JsonProperty
    private Target             filterToggleTarget;

    /**
     * Whether the user-facing flat/tree toggle control is shown (plan-80, build-time component configuration,
     * constant across renders): {@code true} only when the app called {@code TreeTable.withFlatModeToggleEnabled(true)}.
     * When {@code false}, no toggle control is rendered or registered at all &mdash; {@link #flatMode} still reflects
     * the table's current mode, but there is no way for the user to change it.
     */
    @JsonProperty
    private boolean            flatModeToggleEnabled;

    /**
     * Whether this render is in flat mode (plan-80): server-owned boolean field (Cliff C5 pattern, mirrors
     * {@link #filtersVisible}), carried in submitted {@link org.omnaest.react4j.domain.context.data.Data}, defaults
     * to the configured {@code TreeTable.withInitiallyFlat(boolean)} value ({@code false} unless overridden) when the
     * submitted field is absent, flipped by the flat/tree toggle control's server-computed handler. When {@code true},
     * every emitted {@link RowEntryNode} has {@code depth == 0} and {@code expandable == false} (no carets, no
     * indentation, no {@link RowEntryNode#childLoadMore}) &mdash; a plain, non-hierarchical, fully sortable/filterable
     * list of ALL nodes (per the configured {@link org.omnaest.react4j.component.treetable.provider.TreeTableDataProvider}'s
     * flat semantics). The expanded-node-id set is ignored while {@code flatMode} is {@code true}.
     */
    @JsonProperty
    private boolean            flatMode;

    /**
     * Routable positional {@link Target} for the flat/tree toggle control (plan-80) &mdash; its registered handler
     * flips {@link #flatMode} in the submitted {@link org.omnaest.react4j.domain.context.data.Data} server-side
     * (mechanism (a), mirrors {@link #filterToggleTarget}'s server-computed toggle shape). Routable at initial render
     * whenever {@link #flatModeToggleEnabled} is {@code true}; {@link Target#empty()} (no sub-component registered,
     * nothing to click) whenever {@link #flatModeToggleEnabled} is {@code false}.
     */
    @JsonProperty
    private Target             flatToggleTarget;

    @Override
    public String getType()
    {
        return this.type;
    }

    /**
     * Column header + Slice 6 per-column filter/sort header-control descriptor.
     */
    @Data
    @Builder
    public static class ColumnNode
    {
        @JsonProperty
        private String                   key;

        @JsonProperty
        private String                   title;

        /**
         * Whether THIS column's filter control is rendered at all ({@code TreeTableColumn.isFilterable()}, per-column
         * config). When {@code false} (or when the whole-grid {@link TreeTableNode#filterEnabled} is {@code false}),
         * {@link #filterValue}/{@link #filterFieldKey} stay {@code null} and {@link #filterTarget} is
         * {@link Target#empty()}.
         */
        @JsonProperty
        private boolean                  filterable;

        /**
         * Whether THIS column's sort toggle is rendered at all ({@code TreeTableColumn.isSortable()}, per-column
         * config). When {@code false} (or when the whole-grid {@link TreeTableNode#sortEnabled} is {@code false}),
         * {@link #sortDirection} stays {@code null} and {@link #sortTarget} is {@link Target#empty()}.
         */
        @JsonProperty
        private boolean                  sortable;

        /**
         * The column's currently-active filter value (Slice 6), or {@code null} when no filter is active or this
         * column's filter control is not rendered (see {@link #filterable}).
         */
        @JsonProperty
        private String                   filterValue;

        /**
         * The submitted-{@link org.omnaest.react4j.domain.context.data.Data} field key the frontend must set the raw
         * typed filter text into (Slice 6, Cliff C1a mechanism (b) — unlike load-more/expand, a filter value
         * ORIGINATES client-side, so the frontend needs to know exactly which field to write before firing
         * {@link #filterTarget}).
         */
        @JsonProperty
        private String                   filterFieldKey;

        /**
         * Routable positional {@link Target} for this column's filter control (Cliff C3). Its registered handler is
         * a pure identity pass-through — the filter VALUE already lives under {@link #filterFieldKey} in the
         * submitted {@link org.omnaest.react4j.domain.context.data.Data} by the time this fires, so no server-side
         * computation is needed; the {@link Target} exists purely to make the change event resolvable (a legal,
         * non-bare-null {@code /ui/event} round trip).
         */
        @JsonProperty
        private Target                   filterTarget;

        /**
         * This column's current sort direction (Slice 6), or {@code null} when this column is not currently sorted.
         */
        @JsonProperty
        private SortColumn.SortDirection sortDirection;

        /**
         * Routable positional {@link Target} for this column's sort toggle (Cliff C3). Its registered handler cycles
         * ASCENDING -&gt; DESCENDING -&gt; none SERVER-SIDE (mirrors the row expand toggle), so the frontend only needs
         * to fire a click — no client-side sort-state computation.
         */
        @JsonProperty
        private Target                   sortTarget;

        /**
         * This column's 1-based position in the ordered sort list (1 = primary, 2 = secondary, &hellip;), or
         * {@code 0} when this column is not currently sorted (including when {@link #sortable} or the whole-grid
         * {@link TreeTableNode#sortEnabled} is {@code false}). Always emitted, in both single- and multi-column
         * sort mode.
         */
        @JsonProperty
        private int                      sortPriority;

        /**
         * The submitted-{@link org.omnaest.react4j.domain.context.data.Data} field key the frontend must write a
         * chosen new 1-based priority into before firing {@link #sortPriorityReorderTarget} (Cliff C1a mechanism
         * (b), mirrors {@link #filterFieldKey}). {@code null} unless {@link TreeTableNode#multiColumnSortEnabled}
         * is {@code true} AND this column is currently active ({@link #sortPriority} &gt; 0) &mdash; a re-rank
         * control only makes sense for a column that is already sorted, in multi-column mode.
         */
        @JsonProperty
        private String                   sortPriorityFieldKey;

        /**
         * Routable positional {@link Target} for this column's priority re-rank control (Cliff C3). Its registered
         * handler reads the new 1-based priority written under {@link #sortPriorityFieldKey} and moves this column
         * to that position in the ordered sort list, shifting the others while preserving their relative order and
         * every column's direction; an out-of-range or no-op value is ignored defensively.
         * {@link Target#empty()} whenever {@link #sortPriorityFieldKey} is {@code null} (multi-column sort
         * disabled, or this column is not currently active).
         */
        @JsonProperty
        private Target                   sortPriorityReorderTarget;
    }

    /**
     * One flattened row. {@code expanded} is a server-owned boolean field (Cliff C5) — the frontend derives its
     * caret/children-visible state from this field alone and never holds its own client state for it.
     */
    @Data
    @Builder
    public static class RowEntryNode
    {
        @JsonProperty
        private String              nodeId;

        @JsonProperty
        private int                 depth;

        @JsonProperty
        private boolean             expandable;

        @JsonProperty
        private boolean             expanded;

        @JsonProperty
        private Map<String, String> cells;

        /**
         * Deterministic positional {@link Target} for this row (Cliff C3) — since Slice 5, this is ALSO the caret's
         * click target: an expandable row's own gated expand/collapse {@link org.omnaest.react4j.service.internal.handler.domain.DataEventHandler}
         * is registered at exactly this {@link Target} (no separate {@code expandTarget} field was introduced — the
         * frozen {@code target} field already carried the row's positional identity and is reused for this purpose,
         * per the Slice 2 javadoc that anticipated it).
         */
        @JsonProperty
        private Target              target;

        /**
         * Present only when {@link #expanded} is {@code true} (Slice 5): the load-more descriptor for THIS row's
         * OWN child sibling group (own window + own routable {@link Target}), mirroring the top-level
         * {@link TreeTableNode#loadMore} but scoped to this row's {@code nodeId}. {@code null} for a collapsed or
         * non-expandable row. The frontend renders it immediately after this row's flattened children.
         */
        @JsonProperty
        private LoadMoreNode        childLoadMore;
    }

    /**
     * Per-sibling-group load-more descriptor (structure only this slice — wiring is Slice 4).
     */
    @Data
    @Builder
    public static class LoadMoreNode
    {
        @JsonProperty
        private boolean available;

        @JsonProperty
        private long    nextOffset;

        @JsonProperty
        private int     nextLimit;

        /**
         * {@code null} when the provider did not supply {@code getTotalChildCount()}.
         */
        @JsonProperty
        private Long    totalCount;

        @JsonProperty
        private Target  target;
    }
}
