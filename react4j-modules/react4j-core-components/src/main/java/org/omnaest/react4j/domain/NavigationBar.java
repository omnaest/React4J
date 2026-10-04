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

import java.util.function.Consumer;
import java.util.function.Supplier;

public interface NavigationBar extends UIComponent<NavigationBar>
{
    public NavigationBar addEntry(Consumer<NavigationBarEntry> navigationEntryConsumer);

    /**
     * Adds a dropdown to the bar: a toggle that opens a menu of entries. Entries and dropdowns appear in the order they were added.
     * <p>
     * The items of the menu are {@link NavigationBarEntry}s and behave exactly like the entries added through {@link #addEntry(Consumer)}: text, link,
     * linked locator, linked component, active and disabled state. A dropdown is one level deep: an item cannot hold items of its own.
     *
     * @param dropdownConsumer
     *            configures the {@link NavigationBarDropdown}
     * @return this
     */
    public NavigationBar addDropdown(Consumer<NavigationBarDropdown> dropdownConsumer);

    public static interface NavigationBarEntry
    {
        public NavigationBarEntry withText(String text);

        public NavigationBarEntry withLink(String link);

        public NavigationBarEntry withLinkedLocator(String id);

        public NavigationBarEntry withLinked(UIComponent component);

        public NavigationBarEntry withActiveState(boolean active);

        public NavigationBarEntry withDisabledState(boolean disabled);
    }

    /**
     * A toggle with a menu of {@link NavigationBarEntry}s, see {@link NavigationBar#addDropdown(Consumer)}. The toggle itself links nowhere: it only opens the
     * menu.
     */
    public static interface NavigationBarDropdown
    {
        /**
         * The text of the toggle
         */
        public NavigationBarDropdown withText(String text);

        /**
         * Marks the toggle as the active one, for example when one of its items is the current page
         */
        public NavigationBarDropdown withActiveState(boolean active);

        public NavigationBarDropdown withDisabledState(boolean disabled);

        /**
         * Adds an item to the menu, below the items added before
         */
        public NavigationBarDropdown addEntry(Consumer<NavigationBarEntry> navigationEntryConsumer);
    }

    public static interface NavigationBarProvider extends Supplier<NavigationBar>
    {
    }

    public static interface NavigationBarConsumer extends Consumer<NavigationBar>
    {
    }
}
