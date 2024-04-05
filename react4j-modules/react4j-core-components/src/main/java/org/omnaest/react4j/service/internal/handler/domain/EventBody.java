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

import org.omnaest.utils.json.AbstractJSONSerializable;

import com.fasterxml.jackson.annotation.JsonProperty;

public class EventBody extends AbstractJSONSerializable
{
    @JsonProperty
    protected Target target;

    @JsonProperty
    protected DataWithContext dataWithContext;

    protected EventBody()
    {
        super();
    }

    public EventBody(Target target, DataWithContext dataWithContext)
    {
        super();
        this.target = target;
        this.dataWithContext = dataWithContext;
    }

    public Target getTarget()
    {
        return this.target;
    }

    public DataWithContext getDataWithContext()
    {
        return this.dataWithContext;
    }

    protected EventBody setTarget(Target target)
    {
        this.target = target;
        return this;
    }

    protected EventBody setDataWithContext(DataWithContext dataWithContext)
    {
        this.dataWithContext = dataWithContext;
        return this;
    }

}
