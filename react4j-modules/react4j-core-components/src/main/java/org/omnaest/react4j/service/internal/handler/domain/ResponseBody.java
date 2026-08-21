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
package org.omnaest.react4j.service.internal.handler.domain;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ResponseBody extends EventBody
{
    @JsonProperty
    protected TargetNode targetNode;

    public TargetNode getTargetNode()
    {
        return this.targetNode;
    }

    public ResponseBody setTargetNode(TargetNode targetNode)
    {
        this.targetNode = targetNode;
        return this;
    }

    @Override
    public ResponseBody setTarget(Target target)
    {
        super.setTarget(target);
        return this;
    }

    @Override
    public ResponseBody setDataWithContext(DataWithContext dataWithContext)
    {
        super.setDataWithContext(dataWithContext);
        return this;
    }

    /**
     * The response carries EVERY context, each with only the fields it owns.
     * <p>
     * Public here, unlike on {@link EventBody}, because building a response is what this type is for - the base
     * class's setter is protected so that an incoming request cannot be edited in place.
     */
    @Override
    public ResponseBody setDataWithContexts(List<DataWithContext> dataWithContexts)
    {
        super.setDataWithContexts(dataWithContexts);
        return this;
    }
}
