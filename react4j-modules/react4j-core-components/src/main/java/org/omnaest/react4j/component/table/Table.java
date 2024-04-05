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
package org.omnaest.react4j.component.table;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.support.UIContentHolder;

public interface Table extends UIComponent<Table>
{
    public Table withColumnTitles(String... titles);

    public Table withColumnTitles(Iterable<String> titles);

    public Table addRow(Consumer<Row> rowConsumer);

    public <UIC extends UIComponent<?>> Table addRowContent(List<UIC> components);

    public Table addRowTextContent(List<String> texts);

    /**
     * Consumes a given element {@link Stream} and creates a new {@link Row} for each element and provides the {@link Row} together with the element in the
     * {@link BiConsumer}
     * 
     * @param elements
     * @param rowAndElementConsumer
     * @return
     */
    public <E> Table addRows(Stream<E> elements, BiConsumer<Row, E> rowAndElementConsumer);

    public <E> Table addRowsTextContent(List<E> elements, Function<E, List<String>> element);

    public static interface Row
    {
        public Row addCell(Consumer<Cell> cellConsumer);

        public <E> Row addCells(Stream<E> elements, BiConsumer<Cell, E> cellAndElementConsumer);
    }

    public static interface Cell extends UIContentHolder<Cell>
    {

    }

    public Table fromCSVResource(String resourcePath);

    public Table fromCSV(String csv);

    public Table fromDataTable(org.omnaest.utils.table.Table table);

}
