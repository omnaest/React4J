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
import org.omnaest.react4j.service.internal.nodes.context.UIContextDataNode;
import org.omnaest.react4j.service.internal.nodes.i18n.I18nTextValue;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class AccordionNode extends AbstractNode implements Node
{
    @JsonProperty
    private String      type = "ACCORDION";

    @JsonProperty
    private boolean     alwaysOpen;

    @JsonProperty
    private List<Panel> panels;

    @Override
    public String getType()
    {
        return this.type;
    }

    public boolean isAlwaysOpen()
    {
        return this.alwaysOpen;
    }

    public AccordionNode setAlwaysOpen(boolean alwaysOpen)
    {
        this.alwaysOpen = alwaysOpen;
        return this;
    }

    public List<Panel> getPanels()
    {
        return this.panels;
    }

    public AccordionNode setPanels(List<Panel> panels)
    {
        this.panels = panels;
        return this;
    }

    public static class Panel implements Node
    {
        @JsonProperty
        private I18nTextValue title;

        @JsonProperty
        private boolean       expanded;

        @JsonProperty
        private Node          content;

        public I18nTextValue getTitle()
        {
            return this.title;
        }

        public Panel setTitle(I18nTextValue title)
        {
            this.title = title;
            return this;
        }

        public boolean isExpanded()
        {
            return this.expanded;
        }

        public Panel setExpanded(boolean expanded)
        {
            this.expanded = expanded;
            return this;
        }

        public Node getContent()
        {
            return this.content;
        }

        public Panel setContent(Node content)
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

        @Override
        public Node setUiContextData(UIContextDataNode uiContext)
        {
            // does not support this feature
            return this;
        }

    }

}
