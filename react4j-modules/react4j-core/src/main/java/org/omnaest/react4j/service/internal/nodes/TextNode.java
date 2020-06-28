package org.omnaest.react4j.service.internal.nodes;

import java.util.List;

import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.service.internal.nodes.i18n.I18nTextValue;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TextNode extends AbstractNode implements Node
{
    @JsonProperty
    private String type = "TEXT";

    @JsonProperty
    private List<I18nTextValue> texts;

    @Override
    public String getType()
    {
        return this.type;
    }

    public List<I18nTextValue> getTexts()
    {
        return this.texts;
    }

    public TextNode setTexts(List<I18nTextValue> texts)
    {
        this.texts = texts;
        return this;
    }

}
