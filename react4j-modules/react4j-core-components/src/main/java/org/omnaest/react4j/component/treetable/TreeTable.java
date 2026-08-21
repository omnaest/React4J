package org.omnaest.react4j.component.treetable;

import java.util.List;

import org.omnaest.react4j.component.treetable.provider.TreeTableColumn;
import org.omnaest.react4j.component.treetable.provider.TreeTableDataProvider;
import org.omnaest.react4j.component.treetable.provider.TreeTableQuery;
import org.omnaest.react4j.domain.UIComponent;

/**
 * A server-driven, lazy-loading tree table component (AG Grid server-side-row-model shape): the component holds NO data,
 * only the current query state, and delegates every fetch to a {@link TreeTableDataProvider}.
 * <p>
 * Root render loads a single window of {@link #withWindowSize(int)} rows via the configured
 * {@link #withDataProvider(TreeTableDataProvider)}. Expand/collapse, per-column filter/sort, and load-more are added in
 * later slices; this interface intentionally only exposes what is needed for a static root render.
 *
 * @author omnaest
 */
public interface TreeTable extends UIComponent<TreeTable>
{
    /**
     * Default window size used when {@link #withWindowSize(int)} is never called.
     */
    public static final int DEFAULT_WINDOW_SIZE = 500;

    public TreeTable withColumns(TreeTableColumn... columns);

    public TreeTable withColumns(List<TreeTableColumn> columns);

    public TreeTable withDataProvider(TreeTableDataProvider dataProvider);

    public TreeTable withWindowSize(int windowSize);

    /**
     * Enables or disables the whole per-column filtering feature (default {@code true}). When {@code false}, no
     * funnel filter-visibility toggle, no filter row, and no per-column filter controls are rendered or registered
     * at all &mdash; the feature is entirely absent from the emitted node, not merely hidden.
     *
     * @param enabled
     * @return this {@link TreeTable} for fluent chaining
     */
    public TreeTable withFilterEnabled(boolean enabled);

    /**
     * Keeps the header row - and the filter row with it - in place while the body scrolls, so the columns stay
     * identifiable however far down the data you are.
     * <p>
     * The table does not create a scroll region of its own; it sticks to whichever ancestor scrolls. That is
     * deliberate: a table that scrolled internally would need a height, and any height it could invent would be a
     * fixed number that is wrong at the next window size. Put the table in a region that has a height instead - see
     * {@link org.omnaest.react4j.domain.ScrollbarContainer.VerticalBoxMode#FILL_REMAINING_HEIGHT} - and the header
     * sticks to the top of that region.
     * <p>
     * Purely presentational: the same rows are emitted either way.
     *
     * @param stickyHeader
     *            {@code true} to keep the header visible while the body scrolls
     * @return this
     */
    public TreeTable withStickyHeader(boolean stickyHeader);

    /**
     * An accessible name for the grid as a whole, announced when a screen reader enters the table.
     * <p>
     * Worth setting whenever a page holds more than one table, or the table's meaning comes from a heading beside it
     * rather than from the table itself: "table with 6 columns" tells a listener nothing about which table they have
     * landed in.
     * <p>
     * Note the grid already announces its per-column state without configuration - each header carries
     * {@code aria-sort} reflecting the current ordering, and the filter, sort and expand controls carry their own
     * labels naming the column they act on.
     *
     * @param ariaLabel
     *            the accessible name, or {@code null} to leave the table unnamed
     * @return this
     */
    public TreeTable withAriaLabel(String ariaLabel);

    /**
     * Names this table so a handler elsewhere on the page can drive it - see {@code UIComponents}.
     * <p>
     * <b>Why a name rather than the object.</b> The component is rebuilt on every render, so an application
     * cannot hold on to the instance it configured and expect a later handler to be talking about the same
     * table. A name survives that, and the framework resolves it to whatever {@code Location} the table
     * currently occupies.
     * <p>
     * Unnamed tables stay unaddressable. That is deliberate: being drivable from a distance is a capability a
     * page opts into, not something every table acquires by existing.
     *
     * @param name
     *            unique within the page. Two tables sharing a name is a programming error the framework cannot
     *            detect - the second registration simply wins.
     */
    public TreeTable withName(String name);

    /**
     * Enables or disables per-column sorting entirely (default {@code true}). When {@code false}, no sort toggles
     * are rendered or registered at all.
     *
     * @param enabled
     * @return this {@link TreeTable} for fluent chaining
     */
    public TreeTable withSortEnabled(boolean enabled);

    /**
     * Chooses between single-column and multi-column sort behavior for the per-column sort toggle (default
     * {@code false}, i.e. single-column).
     * <ul>
     * <li>{@code false} (default) &mdash; clicking one column's sort toggle makes the ordered sort list contain
     * ONLY that column (still cycling ASCENDING &rarr; DESCENDING &rarr; none; cycling to none empties the list).
     * Any OTHER column that was previously sorted is cleared. At most one column is ever active at a time.</li>
     * <li>{@code true} &mdash; the sort toggle accumulates: a newly-toggled column is APPENDED to the end of the
     * ordered sort list (click order determines primary/secondary/&hellip;), toggling an already-sorted column's
     * direction keeps its existing position, and cycling a column to none removes only its own entry. In this mode
     * a user can also re-rank an active column's priority &mdash; see the per-column {@code sortPriority} /
     * {@code sortPriorityFieldKey} / {@code sortPriorityReorderTarget} emitted on each column.</li>
     * </ul>
     * Only meaningful when {@link #withSortEnabled(boolean)} is {@code true}.
     *
     * @param enabled
     * @return this {@link TreeTable} for fluent chaining
     */
    public TreeTable withMultiColumnSortEnabled(boolean enabled);

    /**
     * Sets the DEFAULT value of the server-owned {@code filtersVisible} field used on the very first render, before
     * any submitted {@link org.omnaest.react4j.domain.context.data.Data} carries a value for it (default
     * {@code false}, i.e. the filter row starts collapsed). Only meaningful when {@link #withFilterEnabled(boolean)}
     * is {@code true} and at least one configured column is filterable.
     *
     * @param visible
     * @return this {@link TreeTable} for fluent chaining
     */
    public TreeTable withFiltersInitiallyVisible(boolean visible);

    /**
     * Enables or disables the user-facing flat/tree toggle control (plan-80 flatten-the-tree mode). Default
     * {@code false} (opt-in): when {@code false}, no toggle control is rendered or registered at all &mdash; the
     * table stays exactly as configured by {@link #withInitiallyFlat(boolean)} for its whole lifetime, with no way
     * for the user to switch. When {@code true}, a distinct toggle sub-component is rendered/registered (mirrors the
     * collapsible-filter funnel toggle) letting the user flip between the normal hierarchical tree and a flat,
     * non-hierarchical list of ALL nodes.
     *
     * @param enabled
     * @return this {@link TreeTable} for fluent chaining
     */
    public TreeTable withFlatModeToggleEnabled(boolean enabled);

    /**
     * Sets the DEFAULT value of the server-owned {@code flatMode} field used on the very first render, before any
     * submitted {@link org.omnaest.react4j.domain.context.data.Data} carries a value for it (default {@code false},
     * i.e. the table starts as a normal hierarchical tree). Combined with {@link #withFlatModeToggleEnabled(boolean)}:
     * <ul>
     * <li>toggle enabled + {@code false} (default) &mdash; the user can switch, starting from the tree view.</li>
     * <li>toggle disabled + {@code false} &mdash; always tree, no control (today's behavior, backward compatible).</li>
     * <li>toggle disabled + {@code true} &mdash; always flat, no control.</li>
     * <li>toggle enabled + {@code true} &mdash; starts flat, switchable.</li>
     * </ul>
     * In flat mode the table renders ALL nodes (every depth, folders and files &mdash; per the provider's flat
     * semantics, see {@link TreeTableQuery#isFlat()}) as a plain, non-hierarchical, fully sortable/filterable list:
     * every emitted row has {@code depth == 0} and {@code expandable == false}, no expand carets, no per-row expand
     * sub-components, no child fetches, no {@code childLoadMore} &mdash; only the root-level window/load-more pages
     * the flat list.
     *
     * @param flat
     * @return this {@link TreeTable} for fluent chaining
     */
    public TreeTable withInitiallyFlat(boolean flat);


    /**
     * Lifecycle hook for an external data change (plan-76 &sect;2.4 extension point "refresh() (lifecycle hook for
     * external-data-change)"): documents, and reserves the name for, forcing this component's next render to
     * re-invoke {@link TreeTableDataProvider#fetch(TreeTableQuery)} with the CURRENT query state
     * (window/expanded-set/filters/sorts carried in the submitted {@link org.omnaest.react4j.domain.context.data.Data})
     * &mdash; the SAME {@code RerenderingContainer} seam the interactive re-query (expand/filter/sort/load-more) already
     * uses, so the two paths cannot diverge (SPI contract, see {@link TreeTableDataProvider}).
     * <p>
     * <b>What this call actually does today, precisely:</b> this component holds no data and never caches a fetch
     * result (Slices 1-6) &mdash; {@code TreeTableRendererImpl}'s internal {@code RerenderingContainer} content
     * function re-invokes {@code provider.fetch(...)} fresh on EVERY render pass already (any subsequent
     * {@code POST /ui/event} whose target routes through this table's subtree, or a fresh {@code GET /ui}). There is
     * therefore no cache for this method to invalidate, and it returns {@code this} unchanged. It exists as the
     * explicitly-named lifecycle hook so application code has a stable, documented spot to call after mutating data
     * out-of-band, and so the "one seam, cannot diverge" contract has a concrete method to point at.
     * <p>
     * <b>Limit:</b> React4J is a request/response server-driven UI with no server-to-already-open-browser push
     * channel (no WebSocket). Calling {@code refresh()} cannot itself deliver new content to a browser tab that is
     * not making a request right now &mdash; the refreshed data is only observed on the NEXT round trip the client
     * initiates (a click anywhere in this table's subtree, or a page reload). If a genuinely live, unsolicited
     * push is required, compose this component under an {@code IntervalRerenderingContainer} (client-driven polling)
     * instead &mdash; that is a distinct, orthogonal mechanism this method does not attempt to replace.
     *
     * @return this {@link TreeTable} for fluent chaining, mirroring {@link #withColumns(TreeTableColumn...)} /
     *         {@link #withDataProvider(TreeTableDataProvider)} / {@link #withWindowSize(int)}
     */
    public TreeTable refresh();
}
