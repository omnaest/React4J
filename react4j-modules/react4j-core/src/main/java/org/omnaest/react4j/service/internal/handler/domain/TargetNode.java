package org.omnaest.react4j.service.internal.handler.domain;

import org.omnaest.react4j.domain.raw.Node;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TargetNode
{
    @JsonProperty
    private Target target;

    @JsonProperty
    private Node node;

    protected TargetNode()
    {
        super();
    }

    public TargetNode(Target target, Node node)
    {
        super();
        this.target = target;
        this.node = node;
    }

    public Target getTarget()
    {
        return this.target;
    }

    public Node getNode()
    {
        return this.node;
    }

    @Override
    public String toString()
    {
        return "TargetNode [target=" + this.target + ", node=" + this.node + "]";
    }

}