package org.omnaest.react4j.component.treetable.internal.data;

import java.util.Collections;
import java.util.List;

import org.omnaest.react4j.component.treetable.TreeTable;
import org.omnaest.react4j.component.treetable.provider.TreeTableColumn;
import org.omnaest.react4j.component.treetable.provider.TreeTableDataProvider;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;

@Data
@Builder(toBuilder = true)
public class TreeTableData
{
    @Default
    private List<TreeTableColumn> columns                 = Collections.emptyList();

    private TreeTableDataProvider dataProvider;

    @Default
    private int                   windowSize              = TreeTable.DEFAULT_WINDOW_SIZE;

    @Default
    private boolean               filterEnabled           = true;

    @Default
    private boolean               sortEnabled             = true;

    @Default
    private boolean               multiColumnSortEnabled  = false;

    @Default
    private boolean               filtersInitiallyVisible = false;

    @Default
    private boolean               flatModeToggleEnabled   = false;

    @Default
    private boolean               stickyHeader            = false;

    private String                ariaLabel;

    @Default
    private boolean               initiallyFlat           = false;

}