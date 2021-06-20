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
package org.omnaest.react4j.domain;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.omnaest.react4j.domain.TextAlignmentContainer.HorizontalAlignment;
import org.omnaest.react4j.domain.support.UIComponentFactoryFunction;
import org.omnaest.react4j.domain.support.UIComponentProvider;
import org.omnaest.react4j.domain.support.UIContentHolder;

public interface GridContainer extends UIComponent<GridContainer>
{

    public GridContainer addRow(Consumer<Row> rowConsumer);

    public GridContainer addRowContent(UIComponent<?> component);

    public GridContainer addRowContent(UIComponentFactoryFunction factoryConsumer);

    public GridContainer addRowContent(UIComponentProvider<?> componentProvider);

    public <E> GridContainer addRowsContent(Stream<E> elements, BiFunction<UIComponentFactory, E, UIComponent<?>> factoryConsumer);

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
        /**
         * Defines how many columns are spanned within the 12 columns grid system
         * 
         * @param numberOfColumns
         * @return
         */
        public Cell withColumnSpan(int numberOfColumns);

        public Cell withHorizontalAlignment(HorizontalAlignment horizontalAlignment);
    }

    public GridContainer withLinkLocator(String locator);

    public GridContainer withUnlimitedColumns();

}
