package org.omnaest.react4j.service.internal.handler.domain;

import java.util.Map;

import org.omnaest.utils.json.AbstractJSONSerializable;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DataWithContext extends AbstractJSONSerializable
{
    @JsonProperty
    private String contextId;

    @JsonProperty
    private Map<String, Object> data;

    public DataWithContext(String contextId, Map<String, Object> data)
    {
        super();
        this.contextId = contextId;
        this.data = data;
    }

    protected DataWithContext()
    {
        super();
    }

    public String getContextId()
    {
        return this.contextId;
    }

    public Map<String, Object> getData()
    {
        return this.data;
    }

    @Override
    public String toString()
    {
        return "DataWithContext [contextId=" + this.contextId + ", data=" + this.data + "]";
    }

}
