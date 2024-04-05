package org.omnaest.react4j.service.internal.nodes.context;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UIContextNode
{
    @JsonProperty
    private String contextId;

    @JsonProperty
    private Map<String, String> data;

    public String getContextId()
    {
        return this.contextId;
    }

    public UIContextNode setContextId(String contextId)
    {
        this.contextId = contextId;
        return this;
    }

    public Map<String, String> getData()
    {
        return this.data;
    }

    public UIContextNode setData(Map<String, String> data)
    {
        this.data = data;
        return this;
    }

    @Override
    public String toString()
    {
        return "UIContextNode [contextId=" + this.contextId + ", data=" + this.data + "]";
    }

}
