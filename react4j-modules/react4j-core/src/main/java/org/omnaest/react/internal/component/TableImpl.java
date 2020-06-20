package org.omnaest.react.internal.component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.omnaest.react.domain.Location;
import org.omnaest.react.domain.Table;
import org.omnaest.react.domain.UIComponent;
import org.omnaest.react.domain.UIComponentFactory;
import org.omnaest.react.domain.i18n.I18nText;
import org.omnaest.react.domain.raw.Node;
import org.omnaest.react.domain.raw.UIComponentRenderer;
import org.omnaest.react.internal.nodes.TableNode;
import org.omnaest.utils.ListUtils;
import org.omnaest.utils.MapperUtils;

public class TableImpl extends AbstractUIComponentWithSubComponents<Table> implements Table
{
    private List<I18nText> columnTitles = Collections.emptyList();
    private List<RowImpl>  rows         = new ArrayList<>();

    public TableImpl(ComponentContext context)
    {
        super(context);
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
    public UIComponentRenderer asRenderer()
    {
        return new UIComponentRenderer()
        {
            @Override
            public Node render(Location parentLocation)
            {
                Location location = Location.of(parentLocation, TableImpl.this.getId());
                return new TableNode().setColumnTitles(TableImpl.this.getTextResolver()
                                                                     .apply(TableImpl.this.columnTitles, location))
                                      .setRows(TableImpl.this.rows.stream()
                                                                  .map(MapperUtils.withIntCounter())
                                                                  .map(rowAndIndex -> new TableNode.RowNode().setCells(rowAndIndex.getFirst()
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
                                                                                                                                      return new TableNode.CellNode().setContent(cell.getContent()
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

}