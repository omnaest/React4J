package org.omnaest.react4j.component.treetable.provider;

import java.util.List;
import java.util.Optional;

/**
 * A single fetch request scoped to exactly ONE parent's sibling group.
 * <p>
 * An empty {@link #getParentNodeId()} means "root level"; a present value means "lazy-load this node's children".
 * {@link #getOffset()}/{@link #getLimit()} window WITHIN that parent's child set (never a global row count).
 * {@link #getSorts()} is ordered (index 0 = primary sort column). {@link #getFilters()} are AND-combined per-column
 * predicates whose tree semantics (prune vs. keep-ancestor-paths) are the {@link TreeTableDataProvider}'s responsibility.
 *
 * @author omnaest
 */
public interface TreeTableQuery
{
    /**
     * Empty for the root level; present to lazy-load the children of the node with this id.
     *
     * @return
     */
    public Optional<String> getParentNodeId();

    /**
     * The window start within the addressed parent's child set.
     *
     * @return
     */
    public long getOffset();

    /**
     * The window size; grows/advances on "load more".
     *
     * @return
     */
    public int getLimit();

    /**
     * Ordered multi-column sort specification; index 0 is the primary sort column.
     *
     * @return
     */
    public List<SortColumn> getSorts();

    /**
     * Per-column filter predicates, AND-combined.
     *
     * @return
     */
    public List<ColumnFilter> getFilters();

    /**
     * Whether this fetch requests the FLATTENED list of ALL nodes (every node at every depth, folders and files)
     * instead of one parent's sibling group (plan-80 flat-mode toggle). Defaults to {@code false} via
     * {@link #of(String, long, int, List, List)} &mdash; the ordinary tree-fetch shape is unchanged.
     * <p>
     * <b>Flat semantics &mdash; which nodes are included &mdash; are the PROVIDER'S responsibility</b>, mirroring the
     * existing tree-filter-semantics contract on {@link TreeTableDataProvider}: the component passes {@code isFlat}
     * down and renders exactly what comes back. The intended default for a provider honoring {@code isFlat} is
     * "all nodes across the whole tree" (folders and files alike), but a provider MAY apply its own scoping (e.g.
     * excluding non-leaf nodes) &mdash; that decision belongs to the provider, not the component. When {@code true}
     * the framework always issues the query with an EMPTY {@link #getParentNodeId()} &mdash; {@link #getOffset()},
     * {@link #getLimit()}, {@link #getSorts()} and {@link #getFilters()} still apply, but to the flat list rather
     * than to one parent's sibling group.
     *
     * @return
     */
    public boolean isFlat();

    /**
     * Creates a new {@link TreeTableQuery} with {@link #isFlat()} defaulting to {@code false} &mdash; the ordinary
     * tree-fetch shape.
     *
     * @param parentNodeId
     *            {@code null} for the root level, or the id of the parent node whose children are being fetched
     * @param offset
     * @param limit
     * @param sorts
     * @param filters
     * @return
     */
    public static TreeTableQuery of(String parentNodeId, long offset, int limit, List<SortColumn> sorts, List<ColumnFilter> filters)
    {
        return new DefaultTreeTableQuery(parentNodeId, offset, limit, sorts, filters, false);
    }

    /**
     * Creates a new {@link TreeTableQuery} with an explicit {@link #isFlat()} value (plan-80 flat-mode toggle). When
     * {@code flat} is {@code true}, {@code parentNodeId} is expected to be {@code null} (empty) &mdash; the framework
     * always issues a flat query at the root, never scoped to a parent.
     *
     * @param parentNodeId
     *            {@code null} for the root level, or the id of the parent node whose children are being fetched
     * @param offset
     * @param limit
     * @param sorts
     * @param filters
     * @param flat
     * @return
     */
    public static TreeTableQuery of(String parentNodeId, long offset, int limit, List<SortColumn> sorts, List<ColumnFilter> filters, boolean flat)
    {
        return new DefaultTreeTableQuery(parentNodeId, offset, limit, sorts, filters, flat);
    }
}
