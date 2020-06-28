package org.omnaest.react4j.domain;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.omnaest.react4j.domain.support.UIContentHolder;

public interface Table extends UIComponent<Table>
{
    public Table withColumnTitles(String... titles);

    public Table withColumnTitles(Iterable<String> titles);

    public Table addRow(Consumer<Row> rowConsumer);

    /**
     * Consumes a given element {@link Stream} and creates a new {@link Row} for each element and provides the {@link Row} together with the element in the
     * {@link BiConsumer}
     * 
     * @param elements
     * @param rowAndElementConsumer
     * @return
     */
    public <E> Table addRows(Stream<E> elements, BiConsumer<Row, E> rowAndElementConsumer);

    public static interface Row
    {
        public Row addCell(Consumer<Cell> cellConsumer);
    }

    public static interface Cell extends UIContentHolder<Cell>
    {

    }
}