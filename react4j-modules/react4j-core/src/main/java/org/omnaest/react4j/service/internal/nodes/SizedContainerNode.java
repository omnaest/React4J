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

public class SizedContainerNode extends AbstractNode implements Node
{
    @JsonProperty
    private String type = "SIZEDCONTAINER";

    @JsonProperty
    private Node content;

    @JsonProperty
    private String height;

    @JsonProperty
    private String width;

    @Override
    public String getType()
    {
        return this.type;
    }

    public Node getContent()
    {
        return this.content;
    }

    public SizedContainerNode setContent(Node content)
    {
        this.content = content;
        return this;
    }

    public String getHeight()
    {
        return this.height;
    }

    public SizedContainerNode setHeight(String height)
    {
        this.height = height;
        return this;
    }

    public String getWidth()
    {
        return this.width;
    }

    public SizedContainerNode setWidth(String width)
    {
        this.width = width;
        return this;
    }

    @Override
    public String toString()
    {
        return "SizedContainerNode [type=" + this.type + ", content=" + this.content + ", height=" + this.height + ", width=" + this.width + "]";
    }

}
