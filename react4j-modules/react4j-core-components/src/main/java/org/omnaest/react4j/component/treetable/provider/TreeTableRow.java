package org.omnaest.react4j.component.treetable.provider;

import java.util.Map;

/**
 * A single row within a fetched sibling group. Carries no parent id — the {@link TreeTableQuery} that produced its
 * {@link TreeTablePage} already carries the parent.
 *
 * @author omnaest
 */
public interface TreeTableRow
{
    /**
     * The unique row identity within the whole tree.
     *
     * @return
     */
    public String getNodeId();

    /**
     * Column key -&gt; cell value; stringified by the component for display.
     *
     * @return
     */
    public Map<String, Object> getCells();

    /**
     * Whether this row should draw an expand caret and allow a lazy child fetch.
     *
     * @return
     */
    public boolean isExpandable();

    /**
     * Framework factory used by {@link TreeTableDataProvider} implementations to build rows.
     *
     * @param nodeId
     * @param cells
     * @param expandable
     * @return
     */
    public static TreeTableRow of(String nodeId, Map<String, Object> cells, boolean expandable)
    {
        return new DefaultTreeTableRow(nodeId, cells, expandable);
    }
}
