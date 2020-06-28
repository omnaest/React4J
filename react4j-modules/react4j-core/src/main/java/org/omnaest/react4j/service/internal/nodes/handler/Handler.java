package org.omnaest.react4j.service.internal.nodes.handler;

import com.fasterxml.jackson.annotation.JsonProperty;

public interface Handler
{
    @JsonProperty
    public String getType();
}
