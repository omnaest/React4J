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

/**
 * Serialized as {@code {"type":"ORDEREDLIST","elements":[...],"startNumber":N}}. Has the implicit no-arg constructor the static renderer registry needs to
 * read the {@link #getType() type} it registers under.
 */
public class OrderedListNode extends AbstractNode implements Node
{
    @JsonProperty
    private String     type        = "ORDEREDLIST";

    @JsonProperty
    private List<Node> elements;

    @JsonProperty
    private int        startNumber = 1;

    @Override
    public String getType()
    {
        return this.type;
    }

    public List<Node> getElements()
    {
        return this.elements;
    }

    public OrderedListNode setElements(List<Node> elements)
    {
        this.elements = elements;
        return this;
    }

    public int getStartNumber()
    {
        return this.startNumber;
    }

    public OrderedListNode setStartNumber(int startNumber)
    {
        this.startNumber = startNumber;
        return this;
    }

}
