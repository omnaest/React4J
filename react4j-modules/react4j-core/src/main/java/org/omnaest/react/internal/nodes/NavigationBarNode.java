package org.omnaest.react.internal.nodes;

import java.util.List;

import org.omnaest.react.domain.raw.Node;
import org.omnaest.react.internal.nodes.i18n.I18nTextValue;

import com.fasterxml.jackson.annotation.JsonProperty;

public class NavigationBarNode extends AbstractNode implements Node
{
    @JsonProperty
    private String type = "NAVIGATIONBAR";

    @JsonProperty
    private List<Entry> entries;

    @Override
    public String getType()
    {
        return this.type;
    }

    public List<Entry> getEntries()
    {
        return this.entries;
    }

    public NavigationBarNode setEntries(List<Entry> entries)
    {
        this.entries = entries;
        return this;
    }

    public static class Entry
    {
        @JsonProperty
        private I18nTextValue text;

        @JsonProperty
        private String link;

        @JsonProperty
        private String linkedId;

        @JsonProperty
        private boolean active;

        @JsonProperty
        private boolean disabled;

        public I18nTextValue getText()
        {
            return this.text;
        }

        public Entry setText(I18nTextValue text)
        {
            this.text = text;
            return this;
        }

        public String getLink()
        {
            return this.link;
        }

        public Entry setLink(String link)
        {
            this.link = link;
            return this;
        }

        public String getLinkedId()
        {
            return this.linkedId;
        }

        public Entry setLinkedId(String linkedId)
        {
            this.linkedId = linkedId;
            return this;
        }

        public boolean isActive()
        {
            return this.active;
        }

        public Entry setActive(boolean active)
        {
            this.active = active;
            return this;
        }

        public boolean isDisabled()
        {
            return this.disabled;
        }

        public Entry setDisabled(boolean disabled)
        {
            this.disabled = disabled;
            return this;
        }
    }
}
