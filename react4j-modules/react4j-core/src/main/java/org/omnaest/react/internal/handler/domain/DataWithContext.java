package org.omnaest.react.internal.handler.domain;

import java.util.Map;

import org.omnaest.utils.json.AbstractJSONSerializable;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DataWithContext extends AbstractJSONSerializable
{
    @JsonProperty
    private String contextId;

    @JsonProperty
    private Map<String, Object> data;

    public String getContextId()
    {
        return this.contextId;
    }

    public Map<String, Object> getData()
    {
        return this.data;
    }

}
