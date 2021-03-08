package org.omnaest.react4j.service.internal.service.internal.context;

import org.omnaest.react4j.domain.context.data.Value;

public class ValueImpl implements Value
{
    private final String value;

    public ValueImpl(String value)
    {
        this.value = value;
    }

    @Override
    public String asString()
    {
        return String.valueOf(this.value);
    }

    @Override
    public int asInteger()
    {
        return Integer.valueOf(this.value);
    }

    @Override
    public boolean asBoolean()
    {
        return Boolean.valueOf(this.value);
    }
}