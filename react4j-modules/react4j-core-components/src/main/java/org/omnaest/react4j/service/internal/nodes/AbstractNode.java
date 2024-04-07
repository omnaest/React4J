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
import org.omnaest.utils.json.AbstractJSONSerializable;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;

@Getter
public abstract class AbstractNode extends AbstractJSONSerializable implements Node
{
    @JsonProperty
    private Target target;

    @JsonProperty
    private String uiContextId;

    protected Node setTarget(Target target)
    {
        this.target = target;
        return this;
    }

    protected Node setUiContextId(String uiContextId)
    {
        this.uiContextId = uiContextId;
        return this;
    }

}
