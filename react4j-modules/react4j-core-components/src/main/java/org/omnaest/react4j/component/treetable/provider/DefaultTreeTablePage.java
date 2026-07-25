package org.omnaest.react4j.component.treetable.provider;

import java.util.Collections;
import java.util.List;
import java.util.OptionalLong;

import lombok.Getter;
import lombok.ToString;

/**
 * @see TreeTablePage#of(List, OptionalLong)
 * @author omnaest
 */
@Getter
@ToString
class DefaultTreeTablePage implements TreeTablePage
{
    private final List<TreeTableRow> rows;
    private final OptionalLong       totalChildCount;

    DefaultTreeTablePage(List<TreeTableRow> rows, OptionalLong totalChildCount)
    {
        this.rows = rows != null ? Collections.unmodifiableList(rows) : Collections.emptyList();
        this.totalChildCount = totalChildCount != null ? totalChildCount : OptionalLong.empty();
    }

}
