package org.omnaest.react.internal.component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.omnaest.react.domain.ContainerGrid;
import org.omnaest.react.domain.Location;
import org.omnaest.react.domain.UIComponent;
import org.omnaest.react.domain.UIComponentFactory;
import org.omnaest.react.domain.raw.Node;
import org.omnaest.react.domain.raw.UIComponentRenderer;
import org.omnaest.react.domain.support.UIComponentFactoryFunction;
import org.omnaest.react.domain.support.UIComponentProvider;
import org.omnaest.react.internal.nodes.ContainerGridNode;
import org.omnaest.utils.ListUtils;
import org.omnaest.utils.MapperUtils;

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
            public Node render(Location parentLocation)
            {
                Location location = Location.of(parentLocation, ContainerGridImpl.this.getId());
                return new ContainerGridNode().setLocator(ContainerGridImpl.this.locator)
                                              .setUnlimitedColumns(ContainerGridImpl.this.unlimitedColumns)
                                              .setRows(ContainerGridImpl.this.rows.stream()
                                                                                  .map(MapperUtils.withIntCounter())
                                                                                  .map(rowAndIndex -> new ContainerGridNode.RowNode().setCells(rowAndIndex.getFirst()
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
                                                                                                                                                                                                     .setContent(cell.getContent()
                                                                                                                                                                                                                     .asRenderer()
                                                                                                                                                                                                                     .render(cellLocation));
                                                                                                                                                          })
                                                                                                                                                          .collect(Collectors.toList())))
                                                                                  .collect(Collectors.toList()));
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