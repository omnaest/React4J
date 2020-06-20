package org.omnaest.react.domain.data;

import java.util.Collections;
import java.util.Map;

import org.omnaest.react.internal.handler.internal.DataImpl;

public interface Data
{
    public static final Data EMPTY = Data.of(null, Collections.emptyMap());

    public Map<String, Object> asMap();

    public static Data of(String contextId, Map<String, Object> map)
    {
        return new DataImpl(contextId, map);
    }

}
