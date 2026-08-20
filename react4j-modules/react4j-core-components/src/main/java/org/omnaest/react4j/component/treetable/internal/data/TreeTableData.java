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

    /**
     * A server-side request to SET the flat/tree mode on this render, rather than merely default it.
     * <p>
     * {@code null} means "no request" and is the normal case - the mode then comes from the submitted data, i.e.
     * from whatever the user last chose. A non-null value is applied and WRITTEN BACK into the submitted data, so
     * it behaves exactly as though the toggle had been pressed: it persists, and the user's next click flips from
     * it rather than from a stale value.
     * <p>
     * Deliberately not a plain boolean with a default. A default cannot express "leave it alone", and a table
     * rebuilt on every render - which is the ordinary React4J shape - would then re-assert the same value forever
     * and silently undo the user's toggle on the very next round trip.
     */
    private Boolean               flatModeRequest;
}
