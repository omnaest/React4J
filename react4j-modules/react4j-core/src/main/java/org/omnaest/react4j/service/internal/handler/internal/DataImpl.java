package org.omnaest.react4j.service.internal.handler.internal;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.omnaest.react4j.domain.data.Data;
import org.omnaest.utils.JSONHelper;
import org.omnaest.utils.MapUtils;
import org.omnaest.utils.MapUtils.MapDelta;
import org.omnaest.utils.optional.NullOptional;

import com.fasterxml.jackson.annotation.JsonProperty;

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