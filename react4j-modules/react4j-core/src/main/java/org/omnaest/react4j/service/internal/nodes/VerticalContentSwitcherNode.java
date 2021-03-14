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
import org.omnaest.react4j.service.internal.handler.domain.Target;
import org.omnaest.react4j.service.internal.nodes.i18n.I18nTextValue;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class VerticalContentSwitcherNode extends AbstractNode implements Node
{
    @JsonProperty
    private String type = "VERTICALCONTENTSWITCHER";

    @JsonProperty
    private List<ContentElement> elements;

    @Override
    public String getType()
    {
        return this.type;
    }

    public List<ContentElement> getElements()
    {
        return this.elements;
    }

    public VerticalContentSwitcherNode setElements(List<ContentElement> elements)
    {
        this.elements = elements;
        return this;
    }

    public static class ContentElement implements Node
    {
        @JsonProperty
        private I18nTextValue title;

        @JsonProperty
        private boolean disabled;

        @JsonProperty
        private boolean active;

        @JsonProperty
        private Node content;

        public I18nTextValue getTitle()
        {
            return this.title;
        }

        public ContentElement setTitle(I18nTextValue title)
        {
            this.title = title;
            return this;
        }

        public boolean isDisabled()
        {
            return this.disabled;
        }

        public ContentElement setDisabled(boolean disabled)
        {
            this.disabled = disabled;
            return this;
        }

        public boolean isActive()
        {
            return this.active;
        }

        public ContentElement setActive(boolean active)
        {
            this.active = active;
            return this;
        }

        public Node getContent()
        {
            return this.content;
        }

        public ContentElement setContent(Node content)
        {
            this.content = content;
            return this;
        }

        @JsonIgnore
        @Override
        public String getType()
        {
            return "";
        }

        @Override
        public Target getTarget()
        {
            return Target.empty();
        }

    }

}
