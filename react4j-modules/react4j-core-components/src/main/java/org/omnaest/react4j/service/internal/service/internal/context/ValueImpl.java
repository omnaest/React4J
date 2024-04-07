package org.omnaest.react4j.service.internal.service.internal.context;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.omnaest.react4j.domain.context.data.Value;
import org.omnaest.utils.MapperUtils;

public class ValueImpl implements Value
{
    private final Object value;

    public ValueImpl(Object value)
    {
        this.value = value;
    }

    @Override
    public String asString()
    {
        return this.asString(null);
    }

    @Override
    public String asString(String defaultValue)
    {
        return this.asStringOptional()
                   .orElse(defaultValue);
    }

    @Override
    public Optional<String> asStringOptional()
    {
        return Optional.ofNullable(this.value)
                       .map(String::valueOf);
    }

    @Override
    public List<String> asStringList()
    {
        return Optional.ofNullable(this.value)
                       .map(MapperUtils.castToListTypeSilently(String.class))
                       .orElse(Collections.emptyList());
    }

    @Override
    public int asInteger()
    {
        return this.asInteger(0);
    }

    @Override
    public Integer asInteger(int defaultValue)
    {
        return this.asIntegerOptional()
                   .orElse(defaultValue);
    }

    @Override
    public Optional<Integer> asIntegerOptional()
    {
        return this.asStringOptional()
                   .map(Integer::valueOf);
    }

    @Override
    public double asDouble()
    {
        return this.asDouble(0.0);
    }

    @Override
    public double asDouble(double defaultValue)
    {
        return this.asDoubleOptional()
                   .orElse(defaultValue);
    }

    @Override
    public Optional<Double> asDoubleOptional()
    {
        return this.asStringOptional()
                   .map(Double::valueOf);
    }

    @Override
    public boolean asBoolean()
    {
        return this.asBoolean(false);
    }

    @Override
    public boolean asBoolean(boolean defaultValue)
    {
        return this.asBooleanOptional()
                   .orElse(defaultValue);
    }

    @Override
    public Optional<Boolean> asBooleanOptional()
    {
        return this.asStringOptional()
                   .map(Boolean::valueOf);
    }
}