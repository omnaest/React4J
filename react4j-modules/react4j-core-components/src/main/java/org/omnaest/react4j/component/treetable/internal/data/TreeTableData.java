package org.omnaest.react4j.component.treetable.internal.data;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

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
     * The application-supplied flat/tree mode, making this a CONTROLLED component.
     * <p>
     * {@code null} - the normal case - leaves the table uncontrolled: the mode comes from the field the toggle
     * flips, and the application need not know about it at all. A non-null value is authoritative for every
     * render, and the application is then responsible for keeping it current via {@link #onFlatModeChange}.
     * <p>
     * Boxed rather than a plain boolean with a default, because a default cannot express "leave it alone".
     */
    private Boolean               flatModeRequest;

    /**
     * Notified with the NEW mode whenever the user presses the toggle - the other half of the controlled contract.
     * <p>
     * Without it, a controlled table would ignore its own toggle: the application would keep supplying the value
     * it last set, and the user's press would be overwritten on the very next render.
     */
    private Consumer<Boolean>     onFlatModeChange;
}
