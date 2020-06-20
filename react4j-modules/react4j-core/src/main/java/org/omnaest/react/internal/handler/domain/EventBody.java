package org.omnaest.react.internal.handler.domain;

import org.omnaest.utils.json.AbstractJSONSerializable;

import com.fasterxml.jackson.annotation.JsonProperty;

public class EventBody extends AbstractJSONSerializable
{
    @JsonProperty
    protected Target target;

    @JsonProperty
    protected DataWithContext dataWithContext;

    public Target getTarget()
    {
        return this.target;
    }

    public DataWithContext getDataWithContext()
    {
        return this.dataWithContext;
    }

    protected EventBody setTarget(Target target)
    {
        this.target = target;
        return this;
    }

    protected EventBody setDataWithContext(DataWithContext dataWithContext)
    {
        this.dataWithContext = dataWithContext;
        return this;
    }

}
