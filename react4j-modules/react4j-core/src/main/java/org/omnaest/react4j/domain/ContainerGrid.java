package org.omnaest.react4j.domain;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.omnaest.react4j.domain.support.UIComponentFactoryFunction;
import org.omnaest.react4j.domain.support.UIComponentProvider;
import org.omnaest.react4j.domain.support.UIContentHolder;

public interface ContainerGrid extends UIComponent<ContainerGrid>
{

    public ContainerGrid addRow(Consumer<Row> rowConsumer);

    public ContainerGrid addRowContent(UIComponent<?> component);

    public ContainerGrid addRowContent(UIComponentFactoryFunction factoryConsumer);

    public ContainerGrid addRowContent(UIComponentProvider<?> componentProvider);

    public <E> ContainerGrid addRowsContent(Stream<E> elements, BiFunction<UIComponentFactory, E, UIComponent<?>> factoryConsumer);

    public static interface Row extends UIContentHolder<Row>
    {
        public Row addCell(Consumer<Cell> cellConsumer);

        /**
         * Adds a new {@link Cell} for each element in the given {@link Stream} and provides the element together with the {@link Cell} to the
         * {@link BiConsumer}
         * 
         * @param elements
         * @param cellConsumer
         * @return
         */
        public <E> Row addCells(Stream<E> elements, BiConsumer<Cell, E> cellConsumer);

        public default <E> Row addCells(Collection<E> elements, BiConsumer<Cell, E> cellConsumer)
        {
            return this.addCells(Optional.ofNullable(elements)
                                         .orElse(Collections.emptyList())
                                         .stream(),
                                 cellConsumer);
        }

        /**
         * Adds a {@link Cell} spanning the whole row with the given content
         * 
         * @param cellConsumer
         * @return
         */
        @Override
        public Row withContent(UIComponent<?> component);

        /**
         * Similar to {@link #withContent(UIComponent)}
         */
        @Override
        public Row withContent(UIComponentFactoryFunction factoryConsumer);

        /**
         * Similar to {@link #withContent(UIComponent)}
         */
        @Override
        public Row withContent(UIComponentProvider<?> componentProvider);

    }

    public static interface Cell extends UIContentHolder<Cell>
    {
        public Cell withColumnSpan(int numberOfColumns);
    }

    public ContainerGrid withLinkLocator(String locator);

    public ContainerGrid withUnlimitedColumns();

}