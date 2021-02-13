package org.omnaest.react4j.service.internal.nodes;

import org.omnaest.react4j.domain.raw.Node;

import com.fasterxml.jackson.annotation.JsonProperty;

public class IconNode extends AbstractNode implements Node
{
    @JsonProperty
    private String type = "ICON";

    @JsonProperty
    private String icon;

    @Override
    public String getType()
    {
        return this.type;
    }

    public String getIcon()
    {
        return this.icon;
    }

    public IconNode setIcon(String icon)
    {
        this.icon = icon;
        return this;
    }

}
