package org.omnaest.react4j.service.internal.nodes;

import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.service.internal.nodes.i18n.I18nTextValue;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AnkerButtonNode extends AbstractNode implements Node
{
    @JsonProperty
    private String type = "ANKERBUTTON";

    @JsonProperty
    private I18nTextValue text;

    @JsonProperty
    private String link;

    @JsonProperty
    private String style;

    @Override
    public String getType()
    {
        return this.type;
    }

    public AnkerButtonNode setText(I18nTextValue text)
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

    public AnkerButtonNode setLink(String link)
    {
        this.link = link;
        return this;
    }

    public String getStyle()
    {
        return this.style;
    }

    public AnkerButtonNode setStyle(String style)
    {
        this.style = style;
        return this;
    }

}
