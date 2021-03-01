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

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.omnaest.utils.ListUtils;

/**
 * Representation of the {@link Location} of a {@link UIComponent} within the component hierarchy.
 * 
 * @author omnaest
 */
public interface Location extends Supplier<List<String>>
{

    public static Location empty()
    {
        return new DefaultLocation(() -> Collections.emptyList());
    }

    public static Location of(Location parent, String id)
    {
        return new DefaultLocation(() -> Stream.concat(Optional.ofNullable(parent)
                                                               .map(Location::get)
                                                               .map(List::stream)
                                                               .orElse(Stream.empty()),
                                                       Stream.of(id))
                                               .collect(Collectors.toList()));
    }

    public static Location of(String id)
    {
        Location parent = null;
        return of(parent, id);
    }

    public default Location and(String id)
    {
        return Location.of(this, id);
    }

    public default boolean isParentOf(Location location)
    {
        return ListUtils.sublistsFromStart(location.get())
                        .anyMatch(sublist -> sublist.equals(location.get()));
    }

    public static Location of(List<String> ids)
    {
        return Optional.ofNullable(ids)
                       .filter(idList -> !idList.isEmpty())
                       .map(idList -> Location.of(ListUtils.sublistFromEnd(idList, 1)))
                       .map(parent -> Location.of(parent, ListUtils.last(ids)))
                       .orElse(Location.of(ListUtils.last(ids)));
    }

    public static class DefaultLocation implements Location
    {
        private final Supplier<List<String>> ids;

        protected DefaultLocation(Supplier<List<String>> ids)
        {
            this.ids = ids;
        }

        @Override
        public List<String> get()
        {
            return this.ids.get();
        }

        @Override
        public String toString()
        {
            return "DefaultLocation [ids=" + this.get() + "]";
        }

    }

}
