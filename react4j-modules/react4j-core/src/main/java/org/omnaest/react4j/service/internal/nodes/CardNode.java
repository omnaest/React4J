package org.omnaest.react4j.service.internal.nodes;

import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.service.internal.nodes.i18n.I18nTextValue;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CardNode extends AbstractNode implements Node
{
    @JsonProperty
    private String type = "CARD";

    @JsonProperty
    private I18nTextValue title;

    @JsonProperty
    private String locator;

    @JsonProperty
    private Node content;

    @JsonProperty
    private boolean adjust;

    @Override
    public String getType()
    {
        return this.type;
    }

    public I18nTextValue getTitle()
    {
        return this.title;
    }

    public CardNode setTitle(I18nTextValue title)
    {
        this.title = title;
        return this;
    }

    public String getLocator()
    {
        return this.locator;
    }

    public CardNode setLocator(String locator)
    {
        this.locator = locator;
        return this;
    }

    public Node getContent()
    {
        return this.content;
    }

    public CardNode setContent(Node content)
    {
        this.content = content;
        return this;
    }

    public boolean isAdjust()
    {
        return this.adjust;
    }

    public CardNode setAdjust(boolean value)
    {
        this.adjust = value;
        return this;
    }

}
