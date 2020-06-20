package org.omnaest.react.internal.handler.internal;

import java.util.Map;

import org.omnaest.react.domain.data.Data;
import org.omnaest.utils.JSONHelper;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DataImpl implements Data
{
    @JsonProperty
    private String contextId;

    @JsonProperty
    private final Map<String, Object> map;

    public DataImpl(String contextId, Map<String, Object> map)
    {
        this.contextId = contextId;
        this.map = map;
    }

    @Override
    public Map<String, Object> asMap()
    {
        return this.map;
    }

    @Override
    public String toString()
    {
        return JSONHelper.prettyPrint(this);
    }

}