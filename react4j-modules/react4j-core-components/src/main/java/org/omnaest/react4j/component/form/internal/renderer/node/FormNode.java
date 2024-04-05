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
package org.omnaest.react4j.component.form.internal.renderer.node;

import java.util.ArrayList;
import java.util.List;

import org.omnaest.react4j.component.form.internal.renderer.node.element.FormElementNode;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.service.internal.nodes.AbstractNode;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FormNode extends AbstractNode implements Node
{
    @JsonProperty
    private String type = "FORM";

    @JsonProperty
    private List<FormElementNode> elements = new ArrayList<>();

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

    public static class FormRangeNode
    {
        @JsonProperty
        private String min;

        @JsonProperty
        private String max;

        @JsonProperty
        private String step;

        public String getMin()
        {
            return this.min;
        }

        public FormRangeNode setMin(String min)
        {
            this.min = min;
            return this;
        }

        public String getMax()
        {
            return this.max;
        }

        public FormRangeNode setMax(String max)
        {
            this.max = max;
            return this;
        }

        public String getStep()
        {
            return this.step;
        }

        public FormRangeNode setStep(String step)
        {
            this.step = step;
            return this;
        }

        @Override
        public String toString()
        {
            StringBuilder builder = new StringBuilder();
            builder.append("FormRangeNode [min=")
                   .append(this.min)
                   .append(", max=")
                   .append(this.max)
                   .append(", step=")
                   .append(this.step)
                   .append("]");
            return builder.toString();
        }

    }
}
