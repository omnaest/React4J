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

import org.omnaest.react4j.domain.ContainerGrid;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.UIComponentFactory;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer;
import org.omnaest.react4j.domain.rendering.components.LocationSupport;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.domain.rendering.node.NodeRendererRegistry;
import org.omnaest.react4j.domain.support.UIComponentFactoryFunction;
import org.omnaest.react4j.domain.support.UIComponentProvider;
import org.omnaest.react4j.service.internal.nodes.ContainerGridNode;
import org.omnaest.react4j.service.internal.nodes.ContainerGridNode.RowNode;
import org.omnaest.utils.ListUtils;
import org.omnaest.utils.MapperUtils;
import org.omnaest.utils.element.bi.BiElement;

public class ContainerGridImpl extends AbstractUIComponentWithSubComponents<ContainerGrid> implements ContainerGrid
{
    private List<RowImpl> rows             = new ArrayList<>();
    private String        locator;
    private boolean       unlimitedColumns = false;

    public ContainerGridImpl(ComponentContext context)
    {
        super(context);
    }

    @Override
    public ContainerGrid addRow(Consumer<Row> rowConsumer)
    {
        RowImpl row = new RowImpl(this.getUiComponentFactory());
        rowConsumer.accept(row);
        this.rows.add(row);
        return this;
    }

    @Override
    public ContainerGrid addRowContent(UIComponent<?> component)
    {
        return this.addRow(row -> row.withContent(component));
    }

    @Override
    public ContainerGrid addRowContent(UIComponentFactoryFunction factoryConsumer)
    {
        return this.addRow(row -> row.withContent(factoryConsumer));
    }

    @Override
    public ContainerGrid addRowContent(UIComponentProvider<?> componentProvider)
    {
        return this.addRow(row -> row.withContent(componentProvider));
    }

    @Override
    public <E> ContainerGrid addRowsContent(Stream<E> elements, BiFunction<UIComponentFactory, E, UIComponent<?>> factoryConsumer)
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
                return locationSupport.createLocation(ContainerGridImpl.this.getId());
            }

            @Override
            public Node render(RenderingProcessor renderingProcessor, Location location)
            {
                return new ContainerGridNode().setLocator(ContainerGridImpl.this.locator)
                                              .setUnlimitedColumns(ContainerGridImpl.this.unlimitedColumns)
                                              .setRows(ContainerGridImpl.this.rows.stream()
                                                                                  .map(MapperUtils.withIntCounter())
                                                                                  .map(this.createRowMapper(renderingProcessor, location))
                                                                                  .collect(Collectors.toList()));
            }

            private Function<BiElement<RowImpl, Integer>, RowNode> createRowMapper(RenderingProcessor renderingProcessor, Location location)
            {
                return rowAndIndex -> new ContainerGridNode.RowNode().setCells(rowAndIndex.getFirst()
                                                                                          .getCells()
                                                                                          .stream()
                                                                                          .map(MapperUtils.withIntCounter())
                                                                                          .map(cellAndIndex ->
                                                                                          {
                                                                                              Location cellLocation = location.and("row"
                                                                                                      + rowAndIndex.getSecond())
                                                                                                                              .and("cell"
                                                                                                                                      + cellAndIndex.getSecond());
                                                                                              CellImpl cell = cellAndIndex.getFirst();
                                                                                              return new ContainerGridNode.CellNode().setColspan(cell.getColumnSpan()
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

            @Override
            public void manageNodeRenderers(NodeRendererRegistry registry)
            {
                registry.registerChildrenMapper(ContainerGridNode.class, ContainerGridNode::getRows);
                registry.registerChildrenMapper(ContainerGridNode.RowNode.class, RowNode::getCells);
                registry.registerChildMapper(ContainerGridNode.CellNode.class, ContainerGridNode.CellNode::getContent);
            }

            @Override
            public Stream<UIComponent<?>> getSubComponents()
            {
                return ContainerGridImpl.this.rows.stream()
                                                  .map(RowImpl::getCells)
                                                  .flatMap(List::stream)
                                                  .map(CellImpl::getContent);
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
        private UIComponent<?> content;
        private Integer        columnSpan;

        public CellImpl(UIComponentFactory uiComponentFactory)
        {
            super(uiComponentFactory);
        }

        @Override
        public Cell withContent(UIComponent<?> component)
        {
            this.content = component;
            return this;
        }

        @Override
        public Cell withColumnSpan(int numberOfColumns)
        {
            this.columnSpan = numberOfColumns;
            return this;
        }

        public UIComponent<?> getContent()
        {
            return this.content;
        }

        public Optional<Integer> getColumnSpan()
        {
            return Optional.ofNullable(this.columnSpan);
        }

    }

    @Override
    public ContainerGrid withLinkLocator(String locator)
    {
        this.locator = locator;
        return this;
    }

    @Override
    public ContainerGrid withUnlimitedColumns()
    {
        this.unlimitedColumns = true;
        return this;
    }

}