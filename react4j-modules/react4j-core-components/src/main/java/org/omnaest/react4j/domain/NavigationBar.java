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

    /**
     * One entry of the bar, or one item of a dropdown's menu. The static HTML render and the interactive React renderer treat an entry identically, with
     * these rules:
     * <ul>
     * <li><b>href</b>: a {@link #withLinkedLocator(String) linked locator} wins, rendering {@code href="#id"}; otherwise the {@link #withLink(String) link}
     * is the href; with neither set the anchor carries no {@code href} attribute at all (never {@code href="null"} or an empty one). An empty string counts
     * as not set.</li>
     * <li><b>target</b>: {@code _blank} if and only if the resolved href is the external link, that is a link is set and no linked locator overrides it;
     * otherwise {@code _self}. An in-page anchor therefore never opens a new tab, even when a link is set as well.</li>
     * <li><b>state</b>: {@link #withActiveState(boolean) active} and {@link #withDisabledState(boolean) disabled} render as the {@code active} and
     * {@code disabled} classes on the entry's anchor, a dropdown item's anchor and a dropdown toggle.</li>
     * </ul>
     * <b>Remaining renderer divergences, both structural.</b> A {@link org.omnaest.react4j.domain.rendering.node.NodeRenderType#HTML} static render has no
     * script, so a dropdown's toggle is Bootstrap {@code data-bs-toggle} markup that opens its menu only if the page that embeds the static HTML loads
     * Bootstrap's JavaScript (the interactive renderer opens it itself); and the static toggle is an anchor with {@code href="#"} where the interactive
     * renderer uses a {@code button}.
     */
    public static interface NavigationBarEntry
    {
        public NavigationBarEntry withText(String text);

        /**
         * The external link of the entry, opened in a new tab unless a {@link #withLinkedLocator(String) linked locator} is set too, which wins. See the
         * rules on {@link NavigationBarEntry}.
         */
        public NavigationBarEntry withLink(String link);

        /**
         * Links the entry to the component carrying the given link locator, as {@code href="#id"}, in the same page ({@code _self}). Wins over
         * {@link #withLink(String)}. See the rules on {@link NavigationBarEntry}.
         */
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
