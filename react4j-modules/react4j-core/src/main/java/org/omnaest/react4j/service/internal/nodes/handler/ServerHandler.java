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
package org.omnaest.react4j.service.internal.nodes.handler;

import org.omnaest.react4j.service.internal.handler.domain.Target;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ServerHandler implements Handler
{
    @JsonProperty
    private String type = "SERVER";

    @JsonProperty
    private Target target;

    @JsonProperty
    private String contextId;

    @JsonCreator
    public ServerHandler(Target target)
    {
        super();
        this.target = target;
    }

    @Override
    public String getType()
    {
        return this.type;
    }

    public Target getTarget()
    {
        return this.target;
    }

    public String getContextId()
    {
        return this.contextId;
    }

    public ServerHandler setContextId(String contextId)
    {
        this.contextId = contextId;
        return this;
    }

}
