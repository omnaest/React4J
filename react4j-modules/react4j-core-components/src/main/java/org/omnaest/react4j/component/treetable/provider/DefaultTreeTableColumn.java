package org.omnaest.react4j.component.treetable.provider;

import java.util.Optional;

import org.omnaest.react4j.component.treetable.provider.SortColumn.SortDirection;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.ToString;

/**
 * @see TreeTableColumn#of(String, String)
 * @author omnaest
 */
@Getter
@ToString
class DefaultTreeTableColumn implements TreeTableColumn
{
    private final String        key;
    private final String        title;
    private final boolean       filterable;
    private final boolean       sortable;

    @Getter(AccessLevel.NONE)
    private final SortDirection initialSortDirection;

    DefaultTreeTableColumn(String key, String title, boolean filterable, boolean sortable, SortDirection initialSortDirection)
    {
        this.key = key;
        this.title = title;
        this.filterable = filterable;
        this.sortable = sortable;
        this.initialSortDirection = initialSortDirection;
    }

    @Override
    public Optional<SortDirection> getInitialSortDirection()
    {
        return Optional.ofNullable(this.initialSortDirection);
    }

    @Override
    public TreeTableColumn withFilterable(boolean filterable)
    {
        return new DefaultTreeTableColumn(this.key, this.title, filterable, this.sortable, this.initialSortDirection);
    }

    @Override
    public TreeTableColumn withSortable(boolean sortable)
    {
        return new DefaultTreeTableColumn(this.key, this.title, this.filterable, sortable, this.initialSortDirection);
    }

    @Override
    public TreeTableColumn withInitialSortDirection(SortDirection direction)
    {
        return new DefaultTreeTableColumn(this.key, this.title, this.filterable, this.sortable, direction);
    }

}
