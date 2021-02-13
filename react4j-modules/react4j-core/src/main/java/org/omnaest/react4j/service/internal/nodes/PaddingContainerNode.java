package org.omnaest.react4j.service.internal.nodes;

import org.omnaest.react4j.domain.raw.Node;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PaddingContainerNode extends AbstractNode implements Node
{
    @JsonProperty
    private String type = "PADDINGCONTAINER";

    @JsonProperty
    private boolean horizontal;

    @JsonProperty
    private boolean vertical;

    @JsonProperty
    private Node content;

    @Override
    public String getType()
    {
        return this.type;
    }

    public boolean isHorizontal()
    {
        return this.horizontal;
    }

    public PaddingContainerNode setHorizontal(boolean horizontal)
    {
        this.horizontal = horizontal;
        return this;
    }

    public boolean isVertical()
    {
        return this.vertical;
    }

    public PaddingContainerNode setVertical(boolean vertical)
    {
        this.vertical = vertical;
        return this;
    }

    public Node getContent()
    {
        return this.content;
    }

    public PaddingContainerNode setContent(Node content)
    {
        this.content = content;
        return this;
    }

}
