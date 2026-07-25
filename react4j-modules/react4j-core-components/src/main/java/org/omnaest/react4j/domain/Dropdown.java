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

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.omnaest.react4j.service.internal.handler.domain.EventHandler;

public interface Dropdown extends UIComponent<Dropdown>
{
    public Dropdown withTitle(String title);

    public Dropdown withStyle(Style style);

    public Dropdown withPresentation(Presentation presentation);

    public Dropdown withDrop(Drop drop);

    public Dropdown addItem(Consumer<DropdownItem> dropdownItemConsumer);

    public Dropdown addDivider();

    public Dropdown addHeader(String text);

    public static interface DropdownItem extends UIComponent<DropdownItem>
    {
        public DropdownItem withText(String text);

        public DropdownItem withLink(String link);

        public DropdownItem withActiveState(boolean active);

        public DropdownItem withDisabledState(boolean disabled);

        public DropdownItem onClick(EventHandler eventHandler);
    }

    public static enum Style
    {
        PRIMARY, SECONDARY, SUCCESS, DANGER, WARNING, INFO, LIGHT, DARK;

        public static Optional<Style> of(String value)
        {
            return Optional.ofNullable(value)
                           .filter(Arrays.asList(values())
                                         .stream()
                                         .map(Style::name)
                                         .collect(Collectors.toSet())::contains)
                           .map(Style::valueOf);
        }
    }

    public static enum Presentation
    {
        BUTTON, NAV;

        public static Optional<Presentation> of(String value)
        {
            return Optional.ofNullable(value)
                           .filter(Arrays.asList(values())
                                         .stream()
                                         .map(Presentation::name)
                                         .collect(Collectors.toSet())::contains)
                           .map(Presentation::valueOf);
        }
    }

    public static enum Drop
    {
        UP, DOWN, START, END;

        public static Optional<Drop> of(String value)
        {
            return Optional.ofNullable(value)
                           .filter(Arrays.asList(values())
                                         .stream()
                                         .map(Drop::name)
                                         .collect(Collectors.toSet())::contains)
                           .map(Drop::valueOf);
        }
    }
}
