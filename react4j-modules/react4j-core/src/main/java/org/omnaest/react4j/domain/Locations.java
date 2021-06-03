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
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Represents {@link Locations} of a {@link UIComponent} in the hierarchy
 * 
 * @author omnaest
 */
public interface Locations extends Supplier<List<Location>>
{
    public default <R> Optional<R> map(Function<List<Location>, R> mapper)
    {
        return Optional.ofNullable(this.get())
                       .map(mapper);
    }

    public static Locations of(Location location)
    {
        return new Locations()
        {
            @Override
            public List<Location> get()
            {
                return Arrays.asList(location);
            }
        };
    }

    public static Locations of(Locations parentLocations, String id)
    {
        return () -> parentLocations.map(list -> list.stream())
                                    .orElse(Stream.of(Location.empty()))
                                    .map(parentLocation -> Location.of(parentLocation, id))
                                    .collect(Collectors.toList());
    }

    public default Location findParentOf(Location location)
    {
        return this.get()
                   .stream()
                   .filter(parentLocation -> parentLocation.equals(location) || parentLocation.isParentOf(location))
                   .findFirst()
                   .orElse(Location.empty());
    }
}
