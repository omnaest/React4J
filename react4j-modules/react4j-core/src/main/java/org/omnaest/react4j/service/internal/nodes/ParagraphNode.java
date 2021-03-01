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

import org.omnaest.react4j.domain.raw.Node;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ParagraphNode extends AbstractNode implements Node
{
    @JsonProperty
    private String id;

    @JsonProperty
    private String type = "PARAGRAPH";

    @JsonProperty
    private List<Node> elements = new ArrayList<>();

    @JsonProperty
    private boolean bold;

    public String getId()
    {
        return this.id;
    }

    @Override
    public String getType()
    {
        return this.type;
    }

    public ParagraphNode setId(String id)
    {
        this.id = id;
        return this;
    }

    public List<Node> getElements()
    {
        return this.elements;
    }

    public ParagraphNode setElements(List<Node> elements)
    {
        this.elements = elements;
        return this;
    }

    public boolean isBold()
    {
        return this.bold;
    }

    public ParagraphNode setBold(boolean bold)
    {
        this.bold = bold;
        return this;
    }

}
