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
package org.omnaest.react4j.service.internal.handler.internal;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.omnaest.react4j.domain.context.data.Data;
import org.omnaest.react4j.domain.context.document.Document.Field;
import org.omnaest.utils.JSONHelper;
import org.omnaest.utils.MapUtils;
import org.omnaest.utils.MapUtils.MapDelta;
import org.omnaest.utils.optional.NullOptional;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonAutoDetect(fieldVisibility = Visibility.ANY, isGetterVisibility = Visibility.NONE, getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE)
public class DataImpl implements Data
{
    @JsonProperty
    private String contextId;

    @JsonProperty
    private String id;

    @JsonProperty
    private final Map<String, Object> map;

    @JsonProperty
    private final Map<String, Object> initial;

    public DataImpl(String contextId, Map<String, Object> map)
    {
        this(null, contextId, map, new HashMap<>(map));
    }

    public DataImpl(String id, String contextId, Map<String, Object> map, Map<String, Object> initial)
    {
        this.id = id;
        this.contextId = contextId;
        this.map = map;
        this.initial = initial;
    }

    @Override
    public Data setFieldValue(String field, Object value)
    {
        this.map.put(field, value);
        return this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <O> O getFieldValue(String field)
    {
        return (O) this.map.get(field);
    }

    @Override
    public <O> O getFieldValue(Field field)
    {
        return this.getFieldValue(field.getFieldName());
    }

    @Override
    public NullOptional<String> getId()
    {
        return NullOptional.ofNullable(this.id);
    }

    @Override
    public String getContextId()
    {
        return this.contextId;
    }

    @Override
    public Map<String, Object> toMap()
    {
        return this.map;
    }

    @Override
    public String toString()
    {
        return JSONHelper.prettyPrint(this);
    }

    @Override
    public Data mergeWith(Data other)
    {
        MapDelta<String, Object> deltaThis = MapUtils.delta(this.initial, this.map);
        MapDelta<String, Object> deltaOther = MapUtils.delta(this.initial, other.toMap());

        Map<String, Object> newMap = new LinkedHashMap<>(this.initial);
        deltaThis.applyTo(newMap);
        deltaOther.applyTo(newMap);

        return Data.of(this.contextId, newMap);
    }

    @Override
    public Data getInitial()
    {
        return Data.of(this.contextId, this.initial);
    }

    @Override
    public Data withId(String id)
    {
        return new DataImpl(id, this.contextId, this.map, this.initial);
    }

}
