package org.omnaest.react.internal.nodes;

import java.util.List;

import org.omnaest.react.domain.raw.Node;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CompositeNode extends AbstractNode implements Node
{
    @JsonProperty
    protected String type;

    @JsonProperty
    protected List<Node> elements;

    protected CompositeNode(String type)
    {
        super();
        this.type = type;
    }

    public CompositeNode()
    {
        super();
        this.type = "COMPOSITE";
    }

    @Override
    public String getType()
    {
        return this.type;
    }

    public List<Node> getElements()
    {
        return this.elements;
    }

    public CompositeNode setElements(List<Node> elements)
    {
        this.elements = elements;
        return this;
    }

}
