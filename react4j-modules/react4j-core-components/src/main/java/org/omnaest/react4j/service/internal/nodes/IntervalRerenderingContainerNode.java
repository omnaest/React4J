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
import org.omnaest.react4j.service.internal.handler.domain.Target;

import com.fasterxml.jackson.annotation.JsonProperty;

public class IntervalRerenderingContainerNode extends AbstractNode implements Node
{
    @JsonProperty
    private String type = "INTERVALRERENDERINGCONTAINER";

    @JsonProperty
    private Node content;

    @JsonProperty
    private int intervalDuration;

    @JsonProperty
    private boolean active;

    @Override
    public String getType()
    {
        return this.type;
    }

    public Node getContent()
    {
        return this.content;
    }

    public IntervalRerenderingContainerNode setContent(Node content)
    {
        this.content = content;
        return this;
    }

    public boolean isActive()
    {
        return this.active;
    }

    public IntervalRerenderingContainerNode setActive(boolean active)
    {
        this.active = active;
        return this;
    }

    public int getIntervalDuration()
    {
        return this.intervalDuration;
    }

    public IntervalRerenderingContainerNode setIntervalDuration(int intervalDuration)
    {
        this.intervalDuration = intervalDuration;
        return this;
    }

    @Override
    public IntervalRerenderingContainerNode setTarget(Target target)
    {
        super.setTarget(target);
        return this;
    }

    @Override
    public String toString()
    {
        return "IntervalRerenderingContainerNode [type=" + this.type + ", content=" + this.content + ", intervalDuration=" + this.intervalDuration + ", active="
                + this.active + "]";
    }

}
