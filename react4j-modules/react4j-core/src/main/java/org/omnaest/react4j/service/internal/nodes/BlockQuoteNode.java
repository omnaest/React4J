package org.omnaest.react4j.service.internal.nodes;

import java.util.List;

import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.service.internal.nodes.i18n.I18nTextValue;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BlockQuoteNode extends AbstractNode implements Node
{
    @JsonProperty
    private String type = "BLOCKQUOTE";

    @JsonProperty
    private List<I18nTextValue> texts;

    @JsonProperty
    private I18nTextValue footer;

    @Override
    public String getType()
    {
        return this.type;
    }

    public List<I18nTextValue> getTexts()
    {
        return this.texts;
    }

    public BlockQuoteNode setTexts(List<I18nTextValue> texts)
    {
        this.texts = texts;
        return this;
    }

    public I18nTextValue getFooter()
    {
        return this.footer;
    }

    public BlockQuoteNode setFooter(I18nTextValue footer)
    {
        this.footer = footer;
        return this;
    }

}
