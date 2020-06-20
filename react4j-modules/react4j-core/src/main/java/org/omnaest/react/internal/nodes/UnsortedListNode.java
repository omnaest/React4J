package org.omnaest.react.internal.nodes;

import java.util.List;

import org.omnaest.react.domain.raw.Node;
import org.omnaest.react.internal.nodes.i18n.I18nTextValue;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UnsortedListNode extends AbstractNode implements Node
{
    @JsonProperty
    private String type = "LIST";

    @JsonProperty
    private List<ULElement> elements;

    @Override
    public String getType()
    {
        return this.type;
    }

    public List<ULElement> getElements()
    {
        return this.elements;
    }

    public UnsortedListNode setElements(List<ULElement> elements)
    {
        this.elements = elements;
        return this;
    }

    public static class ULElement
    {
        private I18nTextValue text;
        private String        icon;

        public I18nTextValue getText()
        {
            return this.text;
        }

        public ULElement setText(I18nTextValue text)
        {
            this.text = text;
            return this;
        }

        public String getIcon()
        {
            return this.icon;
        }

        public ULElement setIcon(String icon)
        {
            this.icon = icon;
            return this;
        }

    }

}
