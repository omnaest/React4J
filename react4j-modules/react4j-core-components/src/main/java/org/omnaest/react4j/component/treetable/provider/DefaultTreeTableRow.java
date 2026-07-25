package org.omnaest.react4j.component.treetable.provider;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import lombok.Getter;
import lombok.ToString;

/**
 * @see TreeTableRow#of(String, Map, boolean)
 * @author omnaest
 */
@Getter
@ToString
class DefaultTreeTableRow implements TreeTableRow
{
    private final String              nodeId;
    private final Map<String, Object> cells;
    private final boolean             expandable;

    DefaultTreeTableRow(String nodeId, Map<String, Object> cells, boolean expandable)
    {
        this.nodeId = nodeId;
        this.cells = cells != null ? Collections.unmodifiableMap(new LinkedHashMap<>(cells)) : Collections.emptyMap();
        this.expandable = expandable;
    }

}
