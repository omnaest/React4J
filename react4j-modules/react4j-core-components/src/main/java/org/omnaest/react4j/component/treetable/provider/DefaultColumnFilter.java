package org.omnaest.react4j.component.treetable.provider;

import org.omnaest.react4j.component.treetable.provider.ColumnFilter.FilterOperator;

import lombok.Getter;
import lombok.ToString;

/**
 * @see ColumnFilter#of(String, FilterOperator, Object)
 * @author omnaest
 */
@Getter
@ToString
class DefaultColumnFilter implements ColumnFilter
{
    private final String         columnKey;
    private final FilterOperator operator;
    private final Object         value;

    DefaultColumnFilter(String columnKey, FilterOperator operator, Object value)
    {
        this.columnKey = columnKey;
        this.operator = operator;
        this.value = value;
    }

}
