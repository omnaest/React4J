package org.omnaest.react4j.component.treetable.provider;

import org.omnaest.react4j.component.treetable.TreeTable;

/**
 * Persistence-agnostic data seam behind a {@code TreeTable}. Any data source (JDBC, Elasticsearch, an in-memory tree, ...)
 * implements this single method to serve one sibling-group window at a time.
 * <p>
 * <b>SPI contract</b> (see plan-76 section 2.2):
 * <ul>
 * <li>Each {@link #fetch(TreeTableQuery)} is scoped to exactly ONE parent and returns ONE sibling group. A root-level render
 * passes an empty {@link TreeTableQuery#getParentNodeId()}. Returned {@link TreeTableRow}s carry no parent id — the query
 * already carries the parent.</li>
 * <li>{@link TreeTablePage#getTotalChildCount()} is optional (streaming/scroll sources may skip counting). When empty, the
 * component falls back to the rule "a full window ({@code rows.size() == limit}) came back &rArr; offer load-more"; when
 * present it drives "showing N of M" and disables load-more at the end.</li>
 * <li><b>Tree-filter semantics are the provider's responsibility.</b> The component passes {@link ColumnFilter}s down and
 * renders exactly what comes back. Whether filtering prunes non-matching branches or keeps ancestor-paths of matches is
 * decided by the provider implementation, not by the component.</li>
 * <li>Both the interactive re-query (expand/filter/sort/load-more) and an explicit {@link TreeTable#refresh()} funnel through
 * this same {@code fetch} method — one seam that cannot diverge. See {@link TreeTable#refresh()} for the precise, honest
 * contract of what that call does (and its documented limits) given this component caches no fetch result.</li>
 * </ul>
 *
 * @author omnaest
 */
public interface TreeTableDataProvider
{
    /**
     * Fetches exactly one sibling group (either the root level or one parent's direct children) for the given
     * {@link TreeTableQuery}.
     *
     * @param query
     * @return
     */
    public TreeTablePage fetch(TreeTableQuery query);
}
