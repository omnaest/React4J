package org.omnaest.react4j.domain;

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
