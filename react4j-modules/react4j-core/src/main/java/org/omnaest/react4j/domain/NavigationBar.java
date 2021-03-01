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

    public static interface NavigationBarEntry
    {
        public NavigationBarEntry withText(String text);

        public NavigationBarEntry withLink(String link);

        public NavigationBarEntry withLinkedLocator(String id);

        public NavigationBarEntry withLinked(UIComponent component);

        public NavigationBarEntry withActiveState(boolean active);

        public NavigationBarEntry withDisabledState(boolean disabled);
    }

    public static interface NavigationBarProvider extends Supplier<NavigationBar>
    {
    }

    public static interface NavigationBarConsumer extends Consumer<NavigationBar>
    {
    }
}
