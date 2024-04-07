package org.omnaest.react4j.service.internal.nodes.context;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UIContextDataNode
{
    @JsonProperty
    private String contextId;

    @JsonProperty
    private Map<String, Object> data;

    @JsonProperty
    private Map<String, Object> internalData;

    public UIContextDataNode setContextId(String contextId)
    {
        this.contextId = contextId;
        return this;
    }

    public UIContextDataNode setData(Map<String, Object> data)
    {
        this.data = data;
        return this;
    }

    public UIContextDataNode setInternalData(Map<String, Object> internalData)
    {
        this.internalData = internalData;
        return this;
    }

}
