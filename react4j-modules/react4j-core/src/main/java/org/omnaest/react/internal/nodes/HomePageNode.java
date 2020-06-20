package org.omnaest.react.internal.nodes;

import org.omnaest.react.domain.raw.Node;

import com.fasterxml.jackson.annotation.JsonProperty;

public class HomePageNode extends AbstractNode
{
    @JsonProperty
    private String type = "HOMEPAGE";

    @JsonProperty
    private Node navigation;

    @JsonProperty
    private Node body;

    @Override
    public String getType()
    {
        return this.type;
    }

    public Node getNavigation()
    {
        return this.navigation;
    }

    public HomePageNode setNavigation(Node navigation)
    {
        this.navigation = navigation;
        return this;
    }

    public Node getBody()
    {
        return this.body;
    }

    public HomePageNode setBody(Node body)
    {
        this.body = body;
        return this;
    }

}
