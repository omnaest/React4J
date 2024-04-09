/*******************************************************************************
 * Copyright 2021 Danny Kunz
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package org.omnaest.react4j.component.table.internal.renderer.node;

import java.util.List;

import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.service.internal.nodes.AbstractNode;
import org.omnaest.react4j.service.internal.nodes.i18n.I18nTextValue;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.Getter;

@Getter
@Builder
public class TableNode extends AbstractNode
{
    @JsonProperty
    private final String type = "TABLE";

    @JsonProperty
    private List<I18nTextValue> columnTitles;

    @JsonProperty
    private List<RowNode> rows;

    @JsonProperty
    private OptionsNode options;

    @Data
    @Builder
    public static class OptionsNode
    {
        @JsonProperty
        @Default
        private String size = "";

        @JsonProperty
        @Default
        private boolean responsive = true;
    }

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
