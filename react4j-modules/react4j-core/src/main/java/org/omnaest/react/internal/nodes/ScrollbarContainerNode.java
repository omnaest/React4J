package org.omnaest.react.internal.nodes;

import org.omnaest.react.domain.raw.Node;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ScrollbarContainerNode extends AbstractNode implements Node
{
    @JsonProperty
    private String type = "SCROLLBARCONTAINER";

    @JsonProperty
    private Node content;

    @JsonProperty
    private String verticalBoxMode;

    @JsonProperty
    private String horizontalBoxMode;

    @Override
    public String getType()
    {
        return this.type;
    }

    public Node getContent()
    {
        return this.content;
    }

    public ScrollbarContainerNode setContent(Node content)
    {
        this.content = content;
        return this;
    }

    public ScrollbarContainerNode setVerticalBoxMode(String verticalBoxMode)
    {
        this.verticalBoxMode = verticalBoxMode;
        return this;
    }

    public String getHorizontalBoxMode()
    {
        return this.horizontalBoxMode;
    }

    public ScrollbarContainerNode setHorizontalBoxMode(String horizontalBoxMode)
    {
        this.horizontalBoxMode = horizontalBoxMode;
        return this;
    }

    public String getVerticalBoxMode()
    {
        return this.verticalBoxMode;
    }

}
