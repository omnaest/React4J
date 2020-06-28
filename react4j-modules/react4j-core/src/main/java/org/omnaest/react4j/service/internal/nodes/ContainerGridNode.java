package org.omnaest.react4j.service.internal.nodes;

import java.util.List;

import org.omnaest.react4j.domain.raw.Node;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ContainerGridNode extends AbstractNode
{
    @JsonProperty
    private String type = "CONTAINER";

    @JsonProperty
    private String locator;

    @JsonProperty
    private List<RowNode> rows;

    @JsonProperty
    private boolean unlimitedColumns;

    public static class RowNode extends AbstractNode
    {
        @JsonProperty
        private String type = "ROW";

        @JsonProperty
        private List<CellNode> cells;

        @Override
        public String getType()
        {
            return this.type;
        }

        public List<CellNode> getCells()
        {
            return this.cells;
        }

        public RowNode setCells(List<CellNode> cells)
        {
            this.cells = cells;
            return this;
        }

    }

    public static class CellNode extends AbstractNode
    {
        @JsonProperty
        private String type = "CELL";

        @JsonProperty
        private int colspan = 1;

        @JsonProperty
        private Node content;

        @Override
        public String getType()
        {
            return this.type;
        }

        public int getColspan()
        {
            return this.colspan;
        }

        public CellNode setColspan(int colSpan)
        {
            this.colspan = colSpan;
            return this;
        }

        public Node getContent()
        {
            return this.content;
        }

        public CellNode setContent(Node content)
        {
            this.content = content;
            return this;
        }

    }

    @Override
    public String getType()
    {
        return this.type;
    }

    public String getLocator()
    {
        return this.locator;
    }

    public ContainerGridNode setLocator(String locator)
    {
        this.locator = locator;
        return this;
    }

    public List<RowNode> getRows()
    {
        return this.rows;
    }

    public ContainerGridNode setRows(List<RowNode> rows)
    {
        this.rows = rows;
        return this;
    }

    public ContainerGridNode setUnlimitedColumns(boolean unlimitedColumns)
    {
        this.unlimitedColumns = unlimitedColumns;
        return this;
    }

    public boolean isUnlimitedColumns()
    {
        return this.unlimitedColumns;
    }

}
