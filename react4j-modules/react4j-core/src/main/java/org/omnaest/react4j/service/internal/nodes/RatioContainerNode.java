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

public class RatioContainerNode extends AbstractNode implements Node
{
    @JsonProperty
    private String type = "RATIOCONTAINER";

    @JsonProperty
    private Node content;

    @JsonProperty
    private Ratio ratio;

    public static enum Ratio
    {
        @JsonProperty("16x9")
        _16x9, @JsonProperty("4x3")
        _4x3, @JsonProperty("1x1")
        _1x1, @JsonProperty("21x9")
        _21x9;
    }

    @Override
    public String getType()
    {
        return this.type;
    }

    public Node getContent()
    {
        return this.content;
    }

    public RatioContainerNode setContent(Node content)
    {
        this.content = content;
        return this;
    }

    public Ratio getRatio()
    {
        return this.ratio;
    }

    public RatioContainerNode setRatio(Ratio ratio)
    {
        this.ratio = ratio;
        return this;
    }

    @Override
    public String toString()
    {
        return "RatioContainerNode [type=" + this.type + ", content=" + this.content + ", ratio=" + this.ratio + "]";
    }

}
