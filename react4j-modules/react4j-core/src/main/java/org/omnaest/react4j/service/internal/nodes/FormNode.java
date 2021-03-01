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

import java.util.ArrayList;
import java.util.List;

import org.omnaest.react4j.domain.raw.FormElementNode;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.service.internal.nodes.handler.Handler;
import org.omnaest.react4j.service.internal.nodes.i18n.I18nTextValue;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FormNode extends AbstractNode implements Node
{
    @JsonProperty
    private String type = "FORM";

    @JsonProperty
    private List<FormElementNode> elements = new ArrayList<>();

    public static class FormElementNodeImpl implements FormElementNode
    {
        @JsonProperty
        private String field;

        @JsonProperty
        private String contextId;

        @JsonProperty
        private String type;

        @JsonProperty
        private I18nTextValue label;

        @JsonProperty
        private I18nTextValue placeholder;

        @JsonProperty
        private I18nTextValue description;

        @JsonProperty
        private I18nTextValue text;

        @JsonProperty
        private Handler onClick;

        public String getType()
        {
            return this.type;
        }

        public FormElementNodeImpl setType(String type)
        {
            this.type = type;
            return this;
        }

        public I18nTextValue getLabel()
        {
            return this.label;
        }

        public FormElementNodeImpl setLabel(I18nTextValue label)
        {
            this.label = label;
            return this;
        }

        public I18nTextValue getPlaceholder()
        {
            return this.placeholder;
        }

        public FormElementNodeImpl setPlaceholder(I18nTextValue placeholder)
        {
            this.placeholder = placeholder;
            return this;
        }

        public String getField()
        {
            return this.field;
        }

        public FormElementNodeImpl setField(String field)
        {
            this.field = field;
            return this;
        }

        public I18nTextValue getDescription()
        {
            return this.description;
        }

        public FormElementNodeImpl setDescription(I18nTextValue description)
        {
            this.description = description;
            return this;
        }

        public FormElementNodeImpl setText(I18nTextValue text)
        {
            this.text = text;
            return this;
        }

        public I18nTextValue getText()
        {
            return this.text;
        }

        public Handler getOnClick()
        {
            return this.onClick;
        }

        public FormElementNodeImpl setOnClick(Handler onClick)
        {
            this.onClick = onClick;
            return this;
        }

        public String getContextId()
        {
            return this.contextId;
        }

        public FormElementNodeImpl setContextId(String contextId)
        {
            this.contextId = contextId;
            return this;
        }

    }

    @Override
    public String getType()
    {
        return this.type;
    }

    public List<FormElementNode> getElements()
    {
        return this.elements;
    }

    public FormNode setElements(List<FormElementNode> elements)
    {
        this.elements = elements;
        return this;
    }

}
