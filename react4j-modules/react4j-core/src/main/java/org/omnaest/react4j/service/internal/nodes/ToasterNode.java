package org.omnaest.react4j.service.internal.nodes;

import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.service.internal.nodes.i18n.I18nTextValue;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ToasterNode extends AbstractNode implements Node
{
    @JsonProperty
    private String type = "TOASTER";

    @JsonProperty
    private Node content;

    @JsonProperty
    private I18nTextValue title;

    @Override
    public String getType()
    {
        return this.type;
    }

    public Node getContent()
    {
        return this.content;
    }

    public ToasterNode setContent(Node content)
    {
        this.content = content;
        return this;
    }

    public I18nTextValue getTitle()
    {
        return this.title;
    }

    public ToasterNode setTitle(I18nTextValue title)
    {
        this.title = title;
        return this;
    }

}
