package org.omnaest.react4j.component.treetable.provider;

import java.util.List;
import java.util.OptionalLong;

/**
 * The result of one {@link TreeTableDataProvider#fetch(TreeTableQuery)} call: the window of {@link TreeTableRow}s for the
 * requested sibling group, plus an optional total count of that group.
 *
 * @author omnaest
 */
public interface TreeTablePage
{
    /**
     * The rows of the requested window.
     *
     * @return
     */
    public List<TreeTableRow> getRows();

    /**
     * The total number of children of the requested parent (or root), if the provider can cheaply compute it. Empty when
     * counting is too expensive (e.g. streaming/scroll sources) — the component then falls back to offering load-more
     * whenever a full window ({@code getRows().size() == query.getLimit()}) came back.
     *
     * @return
     */
    public OptionalLong getTotalChildCount();

    /**
     * Framework factory used by {@link TreeTableDataProvider} implementations to build the return value of
     * {@link TreeTableDataProvider#fetch(TreeTableQuery)}.
     *
     * @param rows
     * @param totalChildCount
     * @return
     */
    public static TreeTablePage of(List<TreeTableRow> rows, OptionalLong totalChildCount)
    {
        return new DefaultTreeTablePage(rows, totalChildCount);
    }
}
