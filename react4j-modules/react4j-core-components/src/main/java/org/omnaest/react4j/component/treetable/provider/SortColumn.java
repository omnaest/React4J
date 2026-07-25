package org.omnaest.react4j.component.treetable.provider;

/**
 * One column of a {@link TreeTableQuery#getSorts()} ordered multi-column sort specification.
 *
 * @author omnaest
 */
public interface SortColumn
{
    /**
     * The column key to sort by.
     *
     * @return
     */
    public String getColumnKey();

    /**
     * The sort direction.
     *
     * @return
     */
    public SortDirection getDirection();

    public static enum SortDirection
    {
        ASCENDING, DESCENDING
    }

    /**
     * Creates a new {@link SortColumn}.
     *
     * @param columnKey
     * @param direction
     * @return
     */
    public static SortColumn of(String columnKey, SortDirection direction)
    {
        return new DefaultSortColumn(columnKey, direction);
    }
}
