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
import org.omnaest.react4j.domain.support.UIComponentFactoryFunction;
import org.omnaest.react4j.domain.support.UIContentHolder;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

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

        public Row addCellContent(UIComponentFactoryFunction factory);

        public <E> Row addCells(Stream<E> elements, BiConsumer<Cell, E> cellAndElementConsumer);
    }

    public static interface Cell extends UIContentHolder<Cell>
    {

    }

    @Getter
    @RequiredArgsConstructor
    public static enum TableSize
    {
        REGULAR(""), SMALL("sm"), MEDIUM("md"), LARGE("lg"), EXTRA_LARGE("xl"), EXTRA_EXTRA_LARGE("xxl");

        private final String identifier;
    }

    public Table fromCSVResource(String resourcePath);

    public Table fromCSV(String csv);

    /**
     * Chooses the viewport width up to which a responsive table scrolls sideways instead of overflowing: the table is
     * wrapped in a {@code table-responsive-<size>} element. Has an effect only on a table that is
     * {@link #withResponsiveness(boolean) responsive} (the default); {@link TableSize#REGULAR} (the default) and
     * {@code null} choose no size.
     *
     * @param tableSize
     *            the size, or {@code null} to leave it unchanged
     * @return this
     */
    public Table withSize(TableSize tableSize);

    /**
     * Wraps the table in a scrolling element so that it scrolls sideways on a narrow screen instead of overflowing.
     * Default is {@code true}.
     * <p>
     * The two renderers differ in one case. The interactive renderer always wraps a responsive table, with
     * {@code table-responsive} when no {@link #withSize(TableSize) size} is chosen. The static HTML render
     * ({@code NodeRenderType.HTML}) has never wrapped a table without a size, and keeps not doing so, because wrapping
     * the default would change the output of every existing table; it wraps a responsive table that has a size, in
     * {@code table-responsive-<size>}, as the interactive renderer does. So in static output an explicit
     * {@code withResponsiveness(true)} is the same as the default: no wrapper. This is a structural difference of the
     * static render, not a missing field.
     *
     * @param responsive
     *            {@code false} to render the table without the scrolling wrapper
     * @return this
     */
    public Table withResponsiveness(boolean responsive);

    /**
     * @see #withResponsiveness(boolean)
     */
    public Table withDisabledResponsiveness();

    public Table fromDataTable(org.omnaest.utils.table.Table table);

}
