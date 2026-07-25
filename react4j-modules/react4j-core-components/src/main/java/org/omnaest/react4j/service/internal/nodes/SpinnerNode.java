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

import com.fasterxml.jackson.annotation.JsonProperty;

public class SpinnerNode extends AbstractNode implements Node
{
    @JsonProperty
    private String type = "SPINNER";

    @JsonProperty
    private String style;

    @JsonProperty
    private String spinnerType;

    @Override
    public String getType()
    {
        return this.type;
    }

    public String getStyle()
    {
        return this.style;
    }

    public SpinnerNode setStyle(String style)
    {
        this.style = style;
        return this;
    }

    public String getSpinnerType()
    {
        return this.spinnerType;
    }

    public SpinnerNode setSpinnerType(String spinnerType)
    {
        this.spinnerType = spinnerType;
        return this;
    }

}
