package org.omnaest.react4j.component.treetable.provider;

import java.util.Optional;

import org.omnaest.react4j.component.treetable.provider.SortColumn.SortDirection;

/**
 * A column definition handed to the {@code TreeTable} component by the application (column key + display title).
 *
 * @author omnaest
 */
public interface TreeTableColumn
{
    /**
     * The column key referenced by {@link TreeTableRow#getCells()}, {@link SortColumn#getColumnKey()} and
     * {@link ColumnFilter#getColumnKey()}.
     *
     * @return
     */
    public String getKey();

    /**
     * The display title of the column.
     *
     * @return
     */
    public String getTitle();

    /**
     * Whether this column's filter control (header input + funnel-row participation) should be rendered at all.
     * Defaults to {@code true} via {@link #of(String, String)}.
     *
     * @return
     */
    public boolean isFilterable();

    /**
     * Whether this column's sort toggle should be rendered at all. Defaults to {@code true} via
     * {@link #of(String, String)}.
     *
     * @return
     */
    public boolean isSortable();

    /**
     * The sort direction this column should be sorted by on the very FIRST render &mdash; i.e. before the user has
     * ever clicked a sort toggle for this grid. Empty by default (no initial sort) via {@link #of(String, String)},
     * meaning the table loads unsorted unless a column declares one via {@link #withInitialSortDirection(SortDirection)}.
     * <p>
     * The initial direction is honored ONLY when this column is also {@link #isSortable()}; a non-sortable column's
     * initial direction is silently ignored by the renderer (no seed sort entry, no sort indicator).
     * <p>
     * When MULTIPLE columns each declare an initial sort direction, priority follows COLUMN DECLARATION ORDER: the
     * first such column (index 0 among the columns handed to the component) is the PRIMARY sort key, the next is the
     * SECONDARY, and so on.
     *
     * @return
     */
    public Optional<SortDirection> getInitialSortDirection();

    /**
     * Returns a new {@link TreeTableColumn} with {@link #isFilterable()} set to {@code filterable}; all other
     * properties (key, title, {@link #isSortable()}, {@link #getInitialSortDirection()}) are carried over unchanged
     * (immutable value type &mdash; this instance is not modified).
     *
     * @param filterable
     * @return a new {@link TreeTableColumn} instance
     */
    public TreeTableColumn withFilterable(boolean filterable);

    /**
     * Returns a new {@link TreeTableColumn} with {@link #isSortable()} set to {@code sortable}; all other
     * properties (key, title, {@link #isFilterable()}, {@link #getInitialSortDirection()}) are carried over
     * unchanged (immutable value type &mdash; this instance is not modified).
     *
     * @param sortable
     * @return a new {@link TreeTableColumn} instance
     */
    public TreeTableColumn withSortable(boolean sortable);

    /**
     * Returns a new {@link TreeTableColumn} with {@link #getInitialSortDirection()} set to {@code direction}; all
     * other properties (key, title, {@link #isFilterable()}, {@link #isSortable()}) are carried over unchanged
     * (immutable value type &mdash; this instance is not modified). Passing {@code null} clears the initial sort
     * direction back to empty.
     * <p>
     * Only takes effect when this column is also {@link #isSortable()} &mdash; see {@link #getInitialSortDirection()}.
     *
     * @param direction
     *            may be {@code null} to clear the initial sort direction
     * @return a new {@link TreeTableColumn} instance
     */
    public TreeTableColumn withInitialSortDirection(SortDirection direction);

    /**
     * Creates a new {@link TreeTableColumn} with {@link #isFilterable()} and {@link #isSortable()} both defaulting
     * to {@code true} and {@link #getInitialSortDirection()} defaulting to empty (no initial sort) &mdash; use
     * {@link #withFilterable(boolean)} / {@link #withSortable(boolean)} / {@link #withInitialSortDirection(SortDirection)}
     * to override.
     *
     * @param key
     * @param title
     * @return
     */
    public static TreeTableColumn of(String key, String title)
    {
        return new DefaultTreeTableColumn(key, title, true, true, null);
    }
}
