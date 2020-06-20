package org.omnaest.react.domain;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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
        return new Location()
        {
            @Override
            public List<String> get()
            {
                return Collections.emptyList();
            }
        };
    }

    public static Location of(Location parent, String id)
    {
        return () -> Stream.concat(parent.get()
                                         .stream(),
                                   Stream.of(id))
                           .collect(Collectors.toList());
    }

    public static Location of(String id)
    {
        return () -> Arrays.asList(id);
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

}
