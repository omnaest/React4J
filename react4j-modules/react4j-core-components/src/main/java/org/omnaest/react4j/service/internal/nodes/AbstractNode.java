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

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.service.internal.handler.domain.Target;
import org.omnaest.react4j.service.internal.nodes.context.UIContextDataNode;
import org.omnaest.utils.json.AbstractJSONSerializable;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;

@Getter
public abstract class AbstractNode extends AbstractJSONSerializable implements Node
{
    @JsonProperty
    private Target target;

    @JsonProperty
    private Set<String> uiContextIds = new LinkedHashSet<>();

    @JsonProperty
    private Optional<UIContextDataNode> uiContextData = Optional.empty();

    protected Node setTarget(Target target)
    {
        this.target = target;
        return this;
    }

    protected Node addUiContextId(String uiContextId)
    {
        if (uiContextId != null)
        {
            this.uiContextIds.add(uiContextId);
        }
        return this;
    }

    protected Node setUiContextIds(Set<String> uiContextIds)
    {
        this.uiContextIds = uiContextIds;
        return this;
    }

    @Override
    public Node setUiContextData(UIContextDataNode uiContextData)
    {
        this.uiContextData = Optional.ofNullable(uiContextData);
        return this;
    }

}
