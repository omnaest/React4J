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

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.omnaest.react4j.domain.context.data.Data;
import org.omnaest.react4j.domain.context.data.Value;
import org.omnaest.react4j.domain.context.document.Document.Field;
import org.omnaest.react4j.service.internal.service.internal.context.ValueImpl;
import org.omnaest.utils.JSONHelper;
import org.omnaest.utils.MapUtils;
import org.omnaest.utils.MapUtils.MapDelta;
import org.omnaest.utils.optional.NullOptional;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.With;

@JsonAutoDetect(fieldVisibility = Visibility.ANY, isGetterVisibility = Visibility.NONE, getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE)
@AllArgsConstructor
@Builder(toBuilder = true)
@With
public class DataImpl implements Data
{
    @JsonProperty
    private String id;

    @JsonProperty
    private String contextId;

    @JsonProperty
    private final Map<String, Object> fieldToValue;

    @JsonProperty
    private final Map<String, Object> initialFieldToValue;

    public DataImpl(String contextId, Map<String, Object> fieldToValue)
    {
        this(null, contextId, Optional.ofNullable(fieldToValue)
                                      .orElse(new HashMap<>()),
                new HashMap<>(Optional.ofNullable(fieldToValue)
                                      .orElse(Collections.emptyMap())));
    }

    @Override
    public Data setFieldValue(String field, Object value)
    {
        this.fieldToValue.put(field, value);
        return this;
    }

    @Override
    public Data setFieldValue(Field field, Object value)
    {
        return this.setFieldValue(field.getFieldName(), value);
    }

    @Override
    public Optional<Value> getFieldValue(String field)
    {
        return Optional.ofNullable(this.fieldToValue)
                       .map(map -> map.get(field))
                       .map(ValueImpl::new);
    }

    @Override
    public Optional<Value> getFieldValue(Field field)
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
        return this.fieldToValue;
    }

    @Override
    public String toString()
    {
        return JSONHelper.prettyPrint(this);
    }

    @Override
    public Data mergeWith(Data other)
    {
        MapDelta<String, Object> deltaThis = MapUtils.delta(this.initialFieldToValue, this.fieldToValue);
        MapDelta<String, Object> deltaOther = MapUtils.delta(this.initialFieldToValue, other.toMap());

        Map<String, Object> newMap = new LinkedHashMap<>(this.initialFieldToValue);
        deltaThis.applyTo(newMap);
        deltaOther.applyTo(newMap);

        return Data.of(this.contextId, newMap);
    }

    @Override
    public Data getInitial()
    {
        return Data.of(this.contextId, this.initialFieldToValue);
    }

    @Override
    public Data withId(String id)
    {
        return this.withId(id);
    }

    @Override
    public Data clone()
    {
        return this.toBuilder()
                   .build();
    }

}
