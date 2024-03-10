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
package org.omnaest.react4j.service.internal.component;

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

import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.Table;
import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.UIComponentFactory;
import org.omnaest.react4j.domain.context.data.Data;
import org.omnaest.react4j.domain.i18n.I18nText;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer;
import org.omnaest.react4j.domain.rendering.components.LocationSupport;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.domain.rendering.node.NodeRenderType;
import org.omnaest.react4j.domain.rendering.node.NodeRenderer;
import org.omnaest.react4j.domain.rendering.node.NodeRendererRegistry;
import org.omnaest.react4j.domain.rendering.node.NodeRenderingProcessor;
import org.omnaest.react4j.domain.support.UIComponentProvider;
import org.omnaest.react4j.service.internal.nodes.TableNode;
import org.omnaest.react4j.service.internal.nodes.TableNode.CellNode;
import org.omnaest.utils.ClassUtils;
import org.omnaest.utils.ListUtils;
import org.omnaest.utils.MapperUtils;
import org.omnaest.utils.element.bi.BiElement;
import org.omnaest.utils.template.TemplateUtils;

public class TableImpl extends AbstractUIComponentWithSubComponents<Table> implements Table
{
    private List<I18nText> columnTitles = Collections.emptyList();
    private List<RowImpl>  rows         = new ArrayList<>();

    public TableImpl(ComponentContext context)
    {
        super(context);
    }

    public TableImpl(ComponentContext context, List<I18nText> columnTitles, List<RowImpl> rows)
    {
        super(context);
        this.columnTitles = columnTitles;
        this.rows = rows;
    }

    @Override
    public Table withColumnTitles(String... titles)
    {
        return this.withColumnTitles(Arrays.asList(titles));
    }

    @Override
    public Table withColumnTitles(Iterable<String> titles)
    {
        this.columnTitles = ListUtils.toList(titles)
                                     .stream()
                                     .map(title -> this.toI18nText(title))
                                     .collect(Collectors.toList());
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
        this.rows.add(row);
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
        return new UIComponentRenderer()
        {

            @Override
            public Location getLocation(LocationSupport locationSupport)
            {
                return locationSupport.createLocation(TableImpl.this.getId());
            }

            @Override
            public Node render(RenderingProcessor renderingProcessor, Location location, Optional<Data> data)
            {
                return new TableNode().setColumnTitles(TableImpl.this.getTextResolver()
                                                                     .apply(TableImpl.this.columnTitles, location))
                                      .setRows(TableImpl.this.rows.stream()
                                                                  .map(MapperUtils.withIntCounter())
                                                                  .map(rowAndIndex -> new TableNode.RowNode().setCells(rowAndIndex.getFirst()
                                                                                                                                  .getCells()
                                                                                                                                  .stream()
                                                                                                                                  .map(MapperUtils.withIntCounter())
                                                                                                                                  .map(this.createCellRenderer(renderingProcessor,
                                                                                                                                                               location,
                                                                                                                                                               rowAndIndex))
                                                                                                                                  .collect(Collectors.toList())))
                                                                  .collect(Collectors.toList()));
            }

            private Function<BiElement<CellImpl, Integer>, CellNode> createCellRenderer(RenderingProcessor renderingProcessor, Location location,
                                                                                        BiElement<RowImpl, Integer> rowAndIndex)
            {
                return cellAndIndex ->
                {
                    Location cellLocation = this.createCellLocation(location, rowAndIndex, cellAndIndex);
                    CellImpl cell = cellAndIndex.getFirst();
                    return new TableNode.CellNode().setContent(renderingProcessor.process(cell.getContent(), cellLocation)

                    );
                };
            }

            private Location createCellLocation(Location location, BiElement<RowImpl, Integer> rowAndIndex, BiElement<CellImpl, Integer> cellAndIndex)
            {
                return Optional.ofNullable(location)
                               .map(iLocation -> iLocation.and("row" + rowAndIndex.getSecond())
                                                          .and("cell" + cellAndIndex.getSecond()))
                               .orElse(null);
            }

            @Override
            public void manageNodeRenderers(NodeRendererRegistry registry)
            {
                registry.register(TableNode.class, NodeRenderType.HTML, new NodeRenderer<TableNode>()
                {
                    @Override
                    public String render(TableNode node, NodeRenderingProcessor nodeRenderingProcessor)
                    {
                        return TemplateUtils.builder()
                                            .useTemplateClassResource(this.getClass(), "/render/templates/html/table.html")
                                            .add("columns", node.getColumnTitles()
                                                                .stream()
                                                                .map(nodeRenderingProcessor::render)
                                                                .collect(Collectors.toList()))
                                            .add("rows", node.getRows()
                                                             .stream()
                                                             .map(rowNode -> rowNode.getCells()
                                                                                    .stream()
                                                                                    .map(CellNode::getContent)
                                                                                    .map(nodeRenderingProcessor::render)
                                                                                    .collect(Collectors.toList()))
                                                             .collect(Collectors.toList()))
                                            .build()
                                            .get();
                    }

                });
            }

            @Override
            public void manageEventHandler(EventHandlerRegistrationSupport eventHandlerRegistrationSupport)
            {
            }

            @Override
            public Stream<ParentLocationAndComponent> getSubComponents(Location parentLocation)
            {
                return TableImpl.this.rows.stream()
                                          .map(MapperUtils.withIntCounter())
                                          .flatMap(rowAndIndex -> rowAndIndex.getFirst()
                                                                             .getCells()
                                                                             .stream()
                                                                             .map(MapperUtils.withIntCounter())
                                                                             .map(cellAndIndex -> ParentLocationAndComponent.of(this.createCellLocation(parentLocation,
                                                                                                                                                        rowAndIndex,
                                                                                                                                                        cellAndIndex),
                                                                                                                                cellAndIndex.getFirst()
                                                                                                                                            .getContent())));
            }

        };
    }

    private static class RowImpl implements Row
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

    private static class CellImpl extends AbstractUIContentHolder<Cell> implements Cell
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
        return () -> new TableImpl(this.context, this.columnTitles.stream()
                                                                  .collect(Collectors.toList()),
                                   this.rows.stream()
                                            .collect(Collectors.toList()));
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

}
