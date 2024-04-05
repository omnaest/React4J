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

public class ProgressBarNode extends AbstractNode implements Node
{
    @JsonProperty
    private String type = "PROGRESSBAR";

    @JsonProperty
    private I18nTextValue text;

    @JsonProperty
    private double value;

    @JsonProperty
    private double min;

    @JsonProperty
    private double max;

    public I18nTextValue getText()
    {
        return this.text;
    }

    public double getValue()
    {
        return this.value;
    }

    public double getMin()
    {
        return this.min;
    }

    public double getMax()
    {
        return this.max;
    }

    public ProgressBarNode setText(I18nTextValue text)
    {
        this.text = text;
        return this;
    }

    public ProgressBarNode setValue(double value)
    {
        this.value = value;
        return this;
    }

    public ProgressBarNode setMin(double min)
    {
        this.min = min;
        return this;
    }

    public ProgressBarNode setMax(double max)
    {
        this.max = max;
        return this;
    }

    @Override
    public String getType()
    {
        return this.type;
    }

    @Override
    public String toString()
    {
        return "ProgressBarNode [type=" + this.type + ", text=" + this.text + ", value=" + this.value + ", min=" + this.min + ", max=" + this.max + "]";
    }

}
