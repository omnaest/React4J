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

import java.util.List;

/**
 * A numbered list, rendered as an {@code <ol>} with one {@code <li>} per entry. The counterpart of {@link UnsortedList} for entries whose order matters.
 *
 * @see UIComponentFactory#newOrderedList()
 */
public interface OrderedList extends UIComponent<OrderedList>
{
    /**
     * Adds an entry that consists of the given text.
     *
     * @param text
     * @return this
     */
    public OrderedList addText(String text);

    /**
     * Adds the given component as the next entry.
     *
     * @param component
     * @return this
     */
    public OrderedList addEntry(UIComponent<?> component);

    /**
     * Adds the given components as the next entries, in the given order. A {@code null} list adds nothing.
     *
     * @param components
     * @return this
     */
    public OrderedList addEntries(List<UIComponent<?>> components);

    /**
     * The number the first entry is counted as. Default is 1, which is the only value that renders no {@code start} attribute.
     *
     * @param startNumber
     * @return this
     */
    public OrderedList withStartNumber(int startNumber);
}
