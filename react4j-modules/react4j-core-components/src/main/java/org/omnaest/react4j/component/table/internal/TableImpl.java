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
package org.omnaest.react4j.component.table.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.omnaest.react4j.component.table.Table;
import org.omnaest.react4j.component.table.internal.data.TableData;
import org.omnaest.react4j.component.table.internal.renderer.TableRendererImpl;
import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.UIComponentFactory;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer;
import org.omnaest.react4j.domain.support.UIComponentProvider;
import org.omnaest.react4j.service.internal.component.AbstractUIComponentWithSubComponents;
import org.omnaest.react4j.service.internal.component.AbstractUIContentHolder;
import org.omnaest.react4j.service.internal.component.ComponentContext;
import org.omnaest.utils.ClassUtils;
import org.omnaest.utils.ListUtils;

public class TableImpl extends AbstractUIComponentWithSubComponents<Table> implements Table
{
    private final TableData.TableDataBuilder data;

    public TableImpl(ComponentContext context)
    {
        this(context, TableData.builder());
    }

    public TableImpl(ComponentContext context, TableData.TableDataBuilder data)
    {
        super(context);
        this.data = data;
    }

    @Override
    public Table withColumnTitles(String... titles)
    {
        return this.withColumnTitles(Arrays.asList(titles));
    }

    @Override
    public Table withColumnTitles(Iterable<String> titles)
    {
        this.data.columnTitles(ListUtils.toList(titles)
                                        .stream()
                                        .map(title -> this.toI18nText(title))
                                        .collect(Collectors.toList()));
        return this;
    }

    @Override
    public <E> Table addRows(Stream<E> elements, BiConsumer<Row, E> rowAndElementConsumer)
    {
        if (elements != null)
        {
            elements.forEach(element -> this.addRow(row -> rowAndElementConsumer.accept(row, element)));
        }
        return this;
    }

    @Override
    public Table addRow(Consumer<Row> rowConsumer)
    {
        RowImpl row = new RowImpl(this.getUiComponentFactory());
        rowConsumer.accept(row);
        this.data.addRow(row);
        return this;
    }

    @Override
    public <UIC extends UIComponent<?>> Table addRowContent(List<UIC> components)
    {
        return this.addRow(row -> row.addCells(Optional.ofNullable(components)
                                                       .orElse(Collections.emptyList())
                                                       .stream(),
                                               Cell::withContent));
    }

    @Override
    public Table addRowTextContent(List<String> texts)
    {
        return this.addRowContent(Optional.ofNullable(texts)
                                          .orElse(Collections.emptyList())
                                          .stream()
                                          .map(text -> this.context.getUiComponentFactory()
                                                                   .newText()
                                                                   .addText(text))
                                          .toList());
    }

    @Override
    public <E> Table addRowsTextContent(List<E> elements, Function<E, List<String>> elementToTextContentMapper)
    {
        if (elementToTextContentMapper != null)
        {
            Optional.ofNullable(elements)
                    .orElse(Collections.emptyList())
                    .stream()
                    .map(elementToTextContentMapper)
                    .forEach(this::addRowTextContent);
        }
        return this;
    }

    @Override
    public Table fromCSVResource(String resourcePath)
    {
        return this.fromCSV(ClassUtils.loadResource(this, resourcePath)
                                      .map(resource -> resource.asString())
                                      .orElse(null));
    }

    @Override
    public Table fromCSV(String csv)
    {
        return this.fromDataTable(org.omnaest.utils.table.Table.newInstance()
                                                               .deserialize()
                                                               .fromCsv(csv));
    }

    @Override
    public Table fromDataTable(org.omnaest.utils.table.Table table)
    {
        this.withColumnTitles(table.getColumnTitles());
        this.addRows(table.stream(), (rowFactory, dataRow) -> dataRow.forEach(dataCell ->
        {
            rowFactory.addCell(cell -> cell.withContent(f -> f.newText()
                                                              .addText(dataCell)));
        }));
        return this;
    }

    @Override
    public UIComponentRenderer asRenderer()
    {
        return new TableRendererImpl(this.data.build(), TableImpl.this.getTextResolver(), this::getId);
    }

    public static class RowImpl implements Row
    {
        private UIComponentFactory uiComponentFactory;
        private List<CellImpl>     cells = new ArrayList<>();

        public RowImpl(UIComponentFactory uiComponentFactory)
        {
            this.uiComponentFactory = uiComponentFactory;
        }

        @Override
        public Row addCell(Consumer<Cell> cellConsumer)
        {
            CellImpl cell = new CellImpl(this.uiComponentFactory);
            cellConsumer.accept(cell);
            this.cells.add(cell);
            return this;
        }

        @Override
        public <E> Row addCells(Stream<E> elements, BiConsumer<Cell, E> cellAndElementConsumer)
        {
            Optional.ofNullable(elements)
                    .orElse(Stream.empty())
                    .forEach(element -> this.addCell(cell -> cellAndElementConsumer.accept(cell, element)));
            return this;
        }

        public List<CellImpl> getCells()
        {
            return this.cells;
        }

    }

    public static class CellImpl extends AbstractUIContentHolder<Cell> implements Cell
    {
        private UIComponent content;

        public CellImpl(UIComponentFactory uiComponentFactory)
        {
            super(uiComponentFactory);
        }

        @Override
        public Cell withContent(UIComponent component)
        {
            this.content = component;
            return this;
        }

        public UIComponent getContent()
        {
            return this.content;
        }

    }

    @Override
    public UIComponentProvider<Table> asTemplateProvider()
    {
        return () -> new TableImpl(this.context, this.data.build()
                                                          .toBuilder());
    }

}
