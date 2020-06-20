package org.omnaest.react.internal.nodes.handler;

import com.fasterxml.jackson.annotation.JsonProperty;

public interface Handler
{
    @JsonProperty
    public String getType();
}
