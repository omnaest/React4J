package org.omnaest.react4j.component.treetable.provider;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import lombok.Getter;
import lombok.ToString;

/**
 * @see TreeTableQuery#of(String, long, int, List, List)
 * @see TreeTableQuery#of(String, long, int, List, List, boolean)
 * @author omnaest
 */
@Getter
@ToString
class DefaultTreeTableQuery implements TreeTableQuery
{
    private final Optional<String>   parentNodeId;
    private final long               offset;
    private final int                limit;
    private final List<SortColumn>   sorts;
    private final List<ColumnFilter> filters;
    private final boolean            flat;

    DefaultTreeTableQuery(String parentNodeId, long offset, int limit, List<SortColumn> sorts, List<ColumnFilter> filters, boolean flat)
    {
        this.parentNodeId = Optional.ofNullable(parentNodeId);
        this.offset = offset;
        this.limit = limit;
        this.sorts = sorts != null ? Collections.unmodifiableList(sorts) : Collections.emptyList();
        this.filters = filters != null ? Collections.unmodifiableList(filters) : Collections.emptyList();
        this.flat = flat;
    }

}
