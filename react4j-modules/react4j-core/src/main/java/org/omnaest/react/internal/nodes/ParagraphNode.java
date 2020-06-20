package org.omnaest.react.internal.nodes;

import java.util.ArrayList;
import java.util.List;

import org.omnaest.react.domain.raw.Node;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ParagraphNode extends AbstractNode implements Node
{
    @JsonProperty
    private String id;

    @JsonProperty
    private String type = "PARAGRAPH";

    @JsonProperty
    private List<Node> elements = new ArrayList<>();

    public String getId()
    {
        return this.id;
    }

    @Override
    public String getType()
    {
        return this.type;
    }

    public ParagraphNode setId(String id)
    {
        this.id = id;
        return this;
    }

    public List<Node> getElements()
    {
        return this.elements;
    }

    public ParagraphNode setElements(List<Node> elements)
    {
        this.elements = elements;
        return this;
    }

}
