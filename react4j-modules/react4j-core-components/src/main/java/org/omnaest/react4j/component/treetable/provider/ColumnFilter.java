package org.omnaest.react4j.component.treetable.provider;

/**
 * One per-column predicate of {@link TreeTableQuery#getFilters()}, AND-combined with any other filters in the same query.
 * <p>
 * Tree-filter semantics (whether a filter prunes non-matching branches or keeps ancestor-paths of matches) are the
 * {@link TreeTableDataProvider}'s responsibility — see {@link TreeTableDataProvider}'s javadoc.
 * <p>
 * {@link FilterOperator} is v1-minimal and extensible under the Rule of Three: add a new operator only once at least three
 * concrete provider use cases need it.
 *
 * @author omnaest
 */
public interface ColumnFilter
{
    /**
     * The column key to filter on.
     *
     * @return
     */
    public String getColumnKey();

    /**
     * The predicate operator.
     *
     * @return
     */
    public FilterOperator getOperator();

    /**
     * The value the operator compares each cell against.
     *
     * @return
     */
    public Object getValue();

    public static enum FilterOperator
    {
        EQUALS, CONTAINS, STARTS_WITH, GREATER_THAN, LESS_THAN
    }

    /**
     * Creates a new {@link ColumnFilter}.
     *
     * @param columnKey
     * @param operator
     * @param value
     * @return
     */
    public static ColumnFilter of(String columnKey, FilterOperator operator, Object value)
    {
        return new DefaultColumnFilter(columnKey, operator, value);
    }
}
