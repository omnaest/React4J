package org.omnaest.react4j.domain.context.data;

import java.util.List;
import java.util.Optional;

public interface Value
{
    public String asString();

    public String asString(String defaultValue);

    public Optional<String> asStringOptional();

    public List<String> asStringList();

    public boolean asBoolean();

    public boolean asBoolean(boolean defaultValue);

    public Optional<Boolean> asBooleanOptional();

    public Integer asInteger(int defaultValue);

    public int asInteger();

    public Optional<Integer> asIntegerOptional();

    public double asDouble();

    public double asDouble(double defaultValue);

    public Optional<Double> asDoubleOptional();

}