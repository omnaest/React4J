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
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.omnaest.react4j.domain.GridContainer;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.TextAlignmentContainer.HorizontalAlignment;
import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.UIComponentFactory;
import org.omnaest.react4j.domain.context.data.Data;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer;
import org.omnaest.react4j.domain.rendering.components.LocationSupport;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.domain.rendering.node.NodeRenderType;
import org.omnaest.react4j.domain.rendering.node.NodeRenderer;
import org.omnaest.react4j.domain.rendering.node.NodeRendererRegistry;
import org.omnaest.react4j.domain.rendering.node.NodeRenderingProcessor;
import org.omnaest.react4j.domain.support.UIComponentFactoryFunction;
import org.omnaest.react4j.domain.support.UIComponentProvider;
import org.omnaest.react4j.service.internal.nodes.GridContainerNode;
import org.omnaest.react4j.service.internal.nodes.GridContainerNode.CellNode;
import org.omnaest.react4j.service.internal.nodes.GridContainerNode.RowNode;
import org.omnaest.utils.ListUtils;
import org.omnaest.utils.MapperUtils;
import org.omnaest.utils.PredicateUtils;
import org.omnaest.utils.element.bi.BiElement;
import org.omnaest.utils.template.TemplateUtils;

public class GridContainerImpl extends AbstractUIComponentWithSubComponents<GridContainer> implements GridContainer
{
    private List<RowImpl> rows             = new ArrayList<>();
    private String        locator;
    private boolean       unlimitedColumns = false;

    public GridContainerImpl(ComponentContext context)
    {
        super(context);
    }

    public GridContainerImpl(ComponentContext context, List<RowImpl> rows, String locator, boolean unlimitedColumns)
    {
        super(context);
        this.rows = rows;
        this.locator = locator;
        this.unlimitedColumns = unlimitedColumns;
    }

    @Override
    public GridContainer addRow(Consumer<Row> rowConsumer)
    {
        RowImpl row = new RowImpl(this.getUiComponentFactory());
        rowConsumer.accept(row);
        this.rows.add(row);
        return this;
    }

    @Override
    public <E> GridContainer addRows(Stream<E> elements, BiConsumer<Row, E> rowConsumer)
    {
        Optional.ofNullable(elements)
                .orElse(Stream.empty())
                .forEach(element -> this.addRow(row -> rowConsumer.accept(row, element)));
        return this;
    }

    @Override
    public GridContainer addRowContent(UIComponent<?> component)
    {
        return this.addRow(row -> row.withContent(component));
    }

    @Override
    public GridContainer addRowContent(UIComponentFactoryFunction factoryConsumer)
    {
        return this.addRow(row -> row.withContent(factoryConsumer));
    }

    @Override
    public GridContainer addRowContent(UIComponentProvider<?> componentProvider)
    {
        return this.addRow(row -> row.withContent(componentProvider));
    }

    @Override
    public <E> GridContainer addRowsContent(Stream<E> elements, BiFunction<UIComponentFactory, E, UIComponent<?>> factoryConsumer)
    {
        if (elements != null)
        {
            elements.forEach(element -> this.addRowContent(factory -> factoryConsumer.apply(factory, element)));
        }
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
                return locationSupport.createLocation(GridContainerImpl.this.getId());
            }

            @Override
            public Node render(RenderingProcessor renderingProcessor, Location location, Optional<Data> data)
            {
                return new GridContainerNode().setLocator(GridContainerImpl.this.locator)
                                              .setUnlimitedColumns(GridContainerImpl.this.unlimitedColumns)
                                              .setRows(GridContainerImpl.this.rows.stream()
                                                                                  .map(MapperUtils.withIntCounter())
                                                                                  .map(this.createRowMapper(renderingProcessor, location))
                                                                                  .collect(Collectors.toList()));
            }

            private Function<BiElement<RowImpl, Integer>, RowNode> createRowMapper(RenderingProcessor renderingProcessor, Location location)
            {
                return rowAndIndex -> new GridContainerNode.RowNode().setCells(rowAndIndex.getFirst()
                                                                                          .getCells()
                                                                                          .stream()
                                                                                          .map(MapperUtils.withIntCounter())
                                                                                          .map(cellAndIndex ->
                                                                                          {
                                                                                              Location cellLocation = this.createCellLocation(location,
                                                                                                                                              rowAndIndex,
                                                                                                                                              cellAndIndex);
                                                                                              CellImpl cell = cellAndIndex.getFirst();
                                                                                              return new GridContainerNode.CellNode().setColspan(cell.getColumnSpan()
                                                                                                                                                     .orElse(12
                                                                                                                                                             / rowAndIndex.getFirst()
                                                                                                                                                                          .getCells()
                                                                                                                                                                          .size()))
                                                                                                                                     .setContent(Optional.ofNullable(cell)
                                                                                                                                                         .map(CellImpl::getContent)
                                                                                                                                                         .map(component -> renderingProcessor.process(component,
                                                                                                                                                                                                      cellLocation))

                                                                                                                                                         .orElse(null));
                                                                                          })
                                                                                          .collect(Collectors.toList()));
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
                registry.register(GridContainerNode.class, NodeRenderType.HTML, new NodeRenderer<GridContainerNode>()
                {
                    @Override
                    public String render(GridContainerNode node, NodeRenderingProcessor nodeRenderingProcessor)
                    {
                        return TemplateUtils.builder()
                                            .useTemplateClassResource(this.getClass(), "/render/templates/html/container_grid.html")
                                            .add("locator", node.getLocator())
                                            .add("rows", node.getRows()
                                                             .stream()
                                                             .map(row -> row.getCells()
                                                                            .stream()
                                                                            .map(CellNode::getContent)
                                                                            .map(nodeRenderingProcessor::render)
                                                                            .filter(PredicateUtils.notNull())
                                                                            .collect(Collectors.toList()))
                                                             .filter(PredicateUtils.notNull())
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
                return GridContainerImpl.this.rows.stream()
                                                  .map(MapperUtils.withIntCounter())
                                                  .flatMap(rowAndIndex -> rowAndIndex.getFirst()
                                                                                     .getCells()
                                                                                     .stream()
                                                                                     .map(MapperUtils.withIntCounter())
                                                                                     .map(cellAndIndex -> cellAndIndex.applyToFirstArgument(cell -> cell.getContent())
                                                                                                                      .applyToSecondArgument(cellIndex -> this.createCellLocation(parentLocation,
                                                                                                                                                                                  rowAndIndex,
                                                                                                                                                                                  cellAndIndex))
                                                                                                                      .reverse()))
                                                  .map(bi -> ParentLocationAndComponent.of(bi.getFirst(), bi.getSecond()));
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
        public <E> Row addCells(Stream<E> elements, BiConsumer<Cell, E> cellConsumer)
        {
            ListUtils.of(elements)
                     .forEach(element -> this.addCell(cell -> cellConsumer.accept(cell, element)));
            return this;
        }

        public List<CellImpl> getCells()
        {
            return this.cells;
        }

        @Override
        public Row withContent(UIComponent<?> component)
        {
            return this.addCell(cell -> cell.withContent(component));
        }

        @Override
        public Row withContent(List<UIComponent<?>> components)
        {
            return this.withContent(this.uiComponentFactory.newComposite()
                                                           .addComponents(components));
        }

        @Override
        public Row withContent(UIComponentFactoryFunction factoryConsumer)
        {
            return this.addCell(cell -> cell.withContent(factoryConsumer));
        }

        @Override
        public Row withContent(UIComponentProvider<?> componentProvider)
        {
            return this.addCell(cell -> cell.withContent(componentProvider));
        }

        @Override
        public Row withContent(LayoutProvider layout, UIComponent<?> component)
        {
            return this.addCell(cell -> cell.withContent(component));
        }

        @Override
        public Row withContent(LayoutProvider layout, UIComponentProvider<?> componentProvider)
        {
            return this.addCell(cell -> cell.withContent(componentProvider));
        }

        @Override
        public Row withContent(LayoutProvider layout, UIComponentFactoryFunction factoryConsumer)
        {
            return this.addCell(cell -> cell.withContent(factoryConsumer));
        }

    }

    private static class CellImpl extends AbstractUIContentHolder<Cell> implements Cell
    {
        private UIComponentProvider<?> content = UIComponentProvider.empty();
        private UIComponent<?>         layoutWrapper;
        private Integer                columnSpan;

        public CellImpl(UIComponentFactory uiComponentFactory)
        {
            super(uiComponentFactory);
        }

        @Override
        public Cell withContent(UIComponent<?> component)
        {
            return this.withContent((UIComponentProvider<UIComponent<?>>) () -> component);
        }

        @Override
        public Cell withContent(UIComponentProvider<?> componentProvider)
        {
            this.content = componentProvider;
            return this;
        }

        @Override
        public Cell withColumnSpan(int numberOfColumns)
        {
            this.columnSpan = numberOfColumns;
            return this;
        }

        @SuppressWarnings({ "rawtypes" })
        @Override
        public Cell withHorizontalAlignment(HorizontalAlignment horizontalAlignment)
        {
            UIComponent<?> currentLayoutWrapper = this.layoutWrapper;
            this.layoutWrapper = this.getUiComponentFactory()
                                     .newTextAlignmentContainer()
                                     .withHorizontalAlignment(horizontalAlignment)
                                     .withContent((UIComponentProvider) () -> Optional.ofNullable((UIComponent) currentLayoutWrapper)
                                                                                      .orElseGet(this.content));
            return this;
        }

        public UIComponent<?> getContent()
        {
            return Optional.<UIComponent<?>>ofNullable(this.layoutWrapper)
                           .orElseGet(this.content);
        }

        public Optional<Integer> getColumnSpan()
        {
            return Optional.ofNullable(this.columnSpan);
        }

    }

    @Override
    public GridContainer withLinkLocator(String locator)
    {
        this.locator = locator;
        return this;
    }

    @Override
    public GridContainer withUnlimitedColumns()
    {
        this.unlimitedColumns = true;
        return this;
    }

    @Override
    public UIComponentProvider<GridContainer> asTemplateProvider()
    {
        return () -> new GridContainerImpl(this.context, this.rows.stream()
                                                                  .collect(Collectors.toList()),
                                           this.locator, this.unlimitedColumns);
    }

}
