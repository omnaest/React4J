package org.omnaest.react4j.service.internal.nodes.handler;

import org.omnaest.react4j.service.internal.handler.domain.Target;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ServerHandler implements Handler
{
    @JsonProperty
    private String type = "SERVER";

    @JsonProperty
    private Target target;

    @JsonProperty
    private String contextId;

    @JsonCreator
    public ServerHandler(Target target)
    {
        super();
        this.target = target;
    }

    @Override
    public String getType()
    {
        return this.type;
    }

    public Target getTarget()
    {
        return this.target;
    }

    public String getContextId()
    {
        return this.contextId;
    }

    public ServerHandler setContextId(String contextId)
    {
        this.contextId = contextId;
        return this;
    }

}
