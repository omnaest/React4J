package org.omnaest.react.internal.nodes;

import org.omnaest.react.domain.raw.Node;
import org.omnaest.react.internal.nodes.i18n.I18nTextValue;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AnkerNode extends AbstractNode implements Node
{
    @JsonProperty
    private String type = "ANKER";

    @JsonProperty
    private I18nTextValue text;

    @JsonProperty
    private String link;

    @Override
    public String getType()
    {
        return this.type;
    }

    public AnkerNode setText(I18nTextValue text)
    {
        this.text = text;
        return this;
    }

    public I18nTextValue getText()
    {
        return this.text;
    }

    public String getLink()
    {
        return this.link;
    }

    public AnkerNode setLink(String link)
    {
        this.link = link;
        return this;
    }

}
