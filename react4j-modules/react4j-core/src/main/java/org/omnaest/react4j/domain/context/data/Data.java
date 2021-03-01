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
package org.omnaest.react4j.domain.context.data;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import org.omnaest.react4j.domain.context.document.Document.Field;
import org.omnaest.react4j.service.internal.handler.internal.DataImpl;
import org.omnaest.utils.optional.NullOptional;

public interface Data
{
    public static final Data EMPTY = Data.of(null, Collections.emptyMap());

    public Map<String, Object> toMap();

    /**
     * Returns a {@link Data} instance with the initial values before any mutable methods have been called.
     * 
     * @return
     */
    public Data getInitial();

    /**
     * Returns the id of the {@link Data} instance in its collection. Can be {@link Optional#empty()} , if it has no id yet.
     * 
     * @return
     */
    public NullOptional<String> getId();

    /**
     * Returns a new {@link Data} instance with the same underlying data with a new id
     * 
     * @param id
     * @return
     */
    public Data withId(String id);

    /**
     * Merges the changes of the current {@link Data} object and the given one. Conflicting changes will be applied in random order.
     * 
     * @param data
     * @return
     */
    public Data mergeWith(Data data);

    public String getContextId();

    public static Data of(String contextId, Map<String, Object> map)
    {
        return new DataImpl(contextId, map);
    }

    public Data setFieldValue(String field, Object value);

    public <O> O getFieldValue(String field);

    public <O> O getFieldValue(Field field);

}
