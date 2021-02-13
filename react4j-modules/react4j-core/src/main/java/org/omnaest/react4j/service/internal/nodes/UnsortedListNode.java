package org.omnaest.react4j.service.internal.nodes;

import java.util.List;

import org.omnaest.react4j.domain.raw.Node;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UnsortedListNode extends AbstractNode implements Node
{
    @JsonProperty
    private String type = "UNORDEREDLIST";

    @JsonProperty
    private List<Node> elements;

    @Override
    public String getType()
    {
        return this.type;
    }

    public List<Node> getElements()
    {
        return this.elements;
    }

    public UnsortedListNode setElements(List<Node> elements)
    {
        this.elements = elements;
        return this;
    }

}
