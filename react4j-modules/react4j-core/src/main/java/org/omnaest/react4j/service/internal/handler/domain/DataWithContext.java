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

import java.util.Map;

import org.omnaest.utils.json.AbstractJSONSerializable;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DataWithContext extends AbstractJSONSerializable
{
    @JsonProperty
    private String contextId;

    @JsonProperty
    private Map<String, Object> data;

    public DataWithContext(String contextId, Map<String, Object> data)
    {
        super();
        this.contextId = contextId;
        this.data = data;
    }

    protected DataWithContext()
    {
        super();
    }

    public String getContextId()
    {
        return this.contextId;
    }

    public Map<String, Object> getData()
    {
        return this.data;
    }

    @Override
    public String toString()
    {
        return "DataWithContext [contextId=" + this.contextId + ", data=" + this.data + "]";
    }

}
