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

import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.service.internal.nodes.i18n.I18nTextValue;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RangeNode extends AbstractNode implements Node
{
    @JsonProperty
    private String type = "RANGE";

    @JsonProperty
    private I18nTextValue label;

    @JsonProperty
    private String min;

    @JsonProperty
    private String max;

    @JsonProperty
    private String step;

    @JsonProperty
    private boolean disabled;

    public I18nTextValue getLabel()
    {
        return this.label;
    }

    public RangeNode setLabel(I18nTextValue label)
    {
        this.label = label;
        return this;
    }

    public String getMin()
    {
        return this.min;
    }

    public RangeNode setMin(String min)
    {
        this.min = min;
        return this;
    }

    public String getMax()
    {
        return this.max;
    }

    public RangeNode setMax(String max)
    {
        this.max = max;
        return this;
    }

    public String getStep()
    {
        return this.step;
    }

    public RangeNode setStep(String step)
    {
        this.step = step;
        return this;
    }

    public boolean isDisabled()
    {
        return this.disabled;
    }

    public RangeNode setDisabled(boolean disabled)
    {
        this.disabled = disabled;
        return this;
    }

    @Override
    public String getType()
    {
        return this.type;
    }

}
