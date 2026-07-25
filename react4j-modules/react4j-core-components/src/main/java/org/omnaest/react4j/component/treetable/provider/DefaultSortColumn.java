package org.omnaest.react4j.component.treetable.provider;

import org.omnaest.react4j.component.treetable.provider.SortColumn.SortDirection;

import lombok.Getter;
import lombok.ToString;

/**
 * @see SortColumn#of(String, SortDirection)
 * @author omnaest
 */
@Getter
@ToString
class DefaultSortColumn implements SortColumn
{
    private final String        columnKey;
    private final SortDirection direction;

    DefaultSortColumn(String columnKey, SortDirection direction)
    {
        this.columnKey = columnKey;
        this.direction = direction;
    }

}
