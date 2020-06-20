package org.omnaest.react.domain.data;

import java.util.function.Function;

import org.omnaest.react.domain.Location;
import org.omnaest.react.domain.Locations;
import org.omnaest.utils.EncoderUtils.TextEncoderAndDecoderFactory;
import org.omnaest.utils.StringUtils;
import org.omnaest.utils.element.cached.CachedElement;

public class DataContextImpl implements SingletonDataContext
{
    private Function<Location, String> idFunction = location -> this.encodeLocation(location);
    private Locations                  locations;

    public DataContextImpl(Locations locations)
    {
        this.locations = locations;
    }

    @Override
    public String getId(Location location)
    {
        return this.idFunction.apply(location);
    }

    @Override
    public SingletonDataContext enableSingleton()
    {
        CachedElement<String> firstId = CachedElement.of(() -> null);
        this.idFunction = location -> firstId.setSuppliedValueLazy(this.encodeLocation(location))
                                             .get();
        return this;
    }

    private String encodeLocation(Location location)
    {
        return StringUtils.encoder()
                          .with(TextEncoderAndDecoderFactory::forAlphaNumericText)
                          .encodeList(this.locations.findParentOf(location)
                                                    .get(),
                                      ".");
    }

    @Override
    public SingletonDataContext withId(String id)
    {
        this.idFunction = location -> id;
        return this;
    }
}