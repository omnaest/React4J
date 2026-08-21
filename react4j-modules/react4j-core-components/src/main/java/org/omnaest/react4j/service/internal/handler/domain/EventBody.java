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

import java.util.Collections;
import java.util.List;

import org.omnaest.utils.json.AbstractJSONSerializable;

import com.fasterxml.jackson.annotation.JsonProperty;

public class EventBody extends AbstractJSONSerializable
{
    @JsonProperty
    protected Target          target;

    @JsonProperty
    protected DataWithContext dataWithContext;

    /**
     * EVERY ui context the page holds, not only the one the event came from.
     * <p>
     * <b>Why a second field rather than a wider {@link #dataWithContext}.</b> A round trip used to carry exactly
     * the originating context, and each component reads its own state out of the submitted {@link Data} while
     * rendering. So an event raised in one component re-rendered every OTHER component from defaults - their
     * fields were simply not in the request. Measured live: a chat submission posted
     * {@code contextId=cardimpl.formimpl} with one field, and the response came back with the page's TreeTable in
     * tree mode, unfiltered, because {@code treetable.*} lives in the root ({@code ""}) context and was never
     * sent.
     * <p>
     * The singular field stays because it carries the event's IDENTITY - which context the response echoes back
     * to - and because every existing caller and test builds one. This list is additive: absent or empty, the
     * behaviour is exactly what it was.
     */
    @JsonProperty
    protected List<DataWithContext> dataWithContexts;

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

    /**
     * Never null - an older client that sends no list is indistinguishable from one that sends an empty one, and
     * both mean "only the originating context".
     */
    public List<DataWithContext> getDataWithContexts()
    {
        return this.dataWithContexts != null ? this.dataWithContexts : Collections.emptyList();
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

    protected EventBody setDataWithContexts(List<DataWithContext> dataWithContexts)
    {
        this.dataWithContexts = dataWithContexts;
        return this;
    }

}
