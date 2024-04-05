/*******************************************************************************
 * Copyright 2021 Danny Kunz
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package org.omnaest.react4j.service.internal.nodes;

import java.util.List;

import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.service.internal.nodes.i18n.I18nTextValue;

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
