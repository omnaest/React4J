package org.omnaest.react.internal.nodes;

import java.util.List;

import org.omnaest.react.domain.raw.Node;
import org.omnaest.react.internal.nodes.i18n.I18nTextValue;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TableNode extends AbstractNode
{
    @JsonProperty
    private String type = "TABLE";

    @JsonProperty
    private List<I18nTextValue> columnTitles;

    @JsonProperty
    private List<RowNode> rows;

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
        private Node content;

        @Override
        public String getType()
        {
            return this.type;
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

    public List<I18nTextValue> getColumnTitles()
    {
        return this.columnTitles;
    }

    public TableNode setColumnTitles(List<I18nTextValue> columnTitles)
    {
        this.columnTitles = columnTitles;
        return this;
    }

    public List<RowNode> getRows()
    {
        return this.rows;
    }

    public TableNode setRows(List<RowNode> rows)
    {
        this.rows = rows;
        return this;
    }

}
