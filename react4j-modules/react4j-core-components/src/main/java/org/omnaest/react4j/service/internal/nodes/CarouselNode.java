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

import com.fasterxml.jackson.annotation.JsonProperty;

public class CarouselNode extends AbstractNode implements Node
{
    @JsonProperty
    private String                 type = "CAROUSEL";

    @JsonProperty
    private List<CarouselItemNode> items;

    @JsonProperty
    private Integer                interval;

    @JsonProperty
    private boolean                controls;

    @JsonProperty
    private boolean                indicators;

    @JsonProperty
    private boolean                fade;

    @Override
    public String getType()
    {
        return this.type;
    }

    public List<CarouselItemNode> getItems()
    {
        return this.items;
    }

    public CarouselNode setItems(List<CarouselItemNode> items)
    {
        this.items = items;
        return this;
    }

    public Integer getInterval()
    {
        return this.interval;
    }

    public CarouselNode setInterval(Integer interval)
    {
        this.interval = interval;
        return this;
    }

    public boolean isControls()
    {
        return this.controls;
    }

    public CarouselNode setControls(boolean controls)
    {
        this.controls = controls;
        return this;
    }

    public boolean isIndicators()
    {
        return this.indicators;
    }

    public CarouselNode setIndicators(boolean indicators)
    {
        this.indicators = indicators;
        return this;
    }

    public boolean isFade()
    {
        return this.fade;
    }

    public CarouselNode setFade(boolean fade)
    {
        this.fade = fade;
        return this;
    }

}
