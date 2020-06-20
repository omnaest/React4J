package org.omnaest.react.internal.nodes;

import org.omnaest.react.domain.raw.Node;
import org.omnaest.react.internal.nodes.i18n.I18nTextValue;

import com.fasterxml.jackson.annotation.JsonProperty;

public class HeadingNode extends AbstractNode implements Node
{
    @JsonProperty
    private String type = "HEADING";

    @JsonProperty
    private I18nTextValue text;

    @JsonProperty
    private int level;

    @Override
    public String getType()
    {
        return this.type;
    }

    public HeadingNode setText(I18nTextValue text)
    {
        this.text = text;
        return this;
    }

    public I18nTextValue getText()
    {
        return this.text;
    }

    public int getLevel()
    {
        return this.level;
    }

    public HeadingNode setLevel(int level)
    {
        this.level = level;
        return this;
    }

}
