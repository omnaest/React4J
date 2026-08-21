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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

import org.omnaest.react4j.domain.context.data.Data;
import org.omnaest.react4j.service.internal.handler.EventHandlerRegistry;
import org.omnaest.react4j.service.internal.handler.EventHandlerService;
import org.omnaest.react4j.service.internal.handler.HandlerResolver;
import org.omnaest.react4j.service.internal.handler.domain.DataEventHandler;
import org.omnaest.react4j.service.internal.handler.domain.DataEventHandler.MappedData;
import org.omnaest.react4j.service.internal.handler.domain.DataWithContext;
import org.omnaest.react4j.service.internal.handler.domain.EventBody;
import org.omnaest.react4j.service.internal.handler.domain.EventHandler;
import org.omnaest.react4j.service.internal.handler.domain.ResponseBody;
import org.omnaest.react4j.service.internal.handler.domain.Target;
import org.omnaest.react4j.service.internal.handler.domain.TargetNode;
import org.omnaest.react4j.service.internal.rerenderer.RerenderingService;
import org.apache.commons.lang3.StringUtils;
import org.omnaest.utils.element.transactional.TransactionalElement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class EventHandlerServiceImpl implements EventHandlerService, EventHandlerRegistry
{
    private TransactionalElement<Map<Target, List<DataEventHandler>>> handlers = this.createTransactionalHandlerMap();

    @Autowired
    protected RerenderingService                                      rerenderingService;

    // plan-78 Slice 1: FALLBACK ONLY, consulted by handleEvent when the active handler map misses (below). Not
    // autowired in EventHandlerServiceImplTest's manual construction, so every use is null-guarded. @Lazy
    // breaks the real bean-creation cycle this fallback dependency introduces (EventHandlerServiceImpl ->
    // HandlerResolver -> RootNodeResolverService(ReactUIServiceImpl) -> EventHandlerRegistry(=this bean)) -
    // Spring Boot's `allow-circular-references` defaults to false, so plain @Autowired field injection alone
    // does not resolve it; a lazily-resolved proxy here does, and is semantically apt since this field is only
    // ever consulted on the rare map-miss fallback path.
    @Autowired
    @Lazy
    protected HandlerResolver                                         handlerResolver;

    private TransactionalElement<Map<Target, List<DataEventHandler>>> createTransactionalHandlerMap()
    {
        return TransactionalElement.<Map<Target, List<DataEventHandler>>>of(() -> new ConcurrentHashMap<>())
                                   .asThreadLocalStaged();
    }

    @Override
    public void registerDataEventHandler(Target target, DataEventHandler eventHandler)
    {
        if (target != null && eventHandler != null)
        {
            // Idempotent per render: replace any prior registration for this Target within the
            // current (thread-local) staging cycle, so re-rendering the same Target - which plan-12's
            // two-pass render and plan-13's per-render content rebuild both cause - yields exactly one
            // handler and thus exactly one invocation per event. The merge in
            // executeTransactionalAndPublishStagingHandlers (putIfAbsent) still carries forward
            // handlers for Targets NOT re-rendered this cycle.
            this.handlers.getStaging()
                         .put(target, Collections.synchronizedList(new ArrayList<>(List.of(eventHandler))));
        }
    }

    @Override
    public void registerEventHandler(Target target, EventHandler eventHandler)
    {
        if (eventHandler != null)
        {
            this.registerDataEventHandler(target, (data, internalData) ->
            {
                eventHandler.invoke();
                return MappedData.builder()
                                 .data(data)
                                 .internalData(internalData)
                                 .build();
            });
        }
    }

    @Override
    public Optional<ResponseBody> handleEvent(EventBody eventBody)
    {
        Optional<Target> target = Optional.ofNullable(eventBody)
                                          .map(EventBody::getTarget);
        Optional<Data> data = this.submittedDataAcrossAllContexts(eventBody);
        Optional<Data> internalData = Optional.ofNullable(eventBody.getDataWithContext())
                                              .map(dwc -> Data.of(dwc.getContextId(), dwc.getInternalData()));

        // First render pass: (re)registers the target's current handlers into the active map (unchanged behavior).
        // Its resulting node is intentionally discarded - the response node is produced by the SECOND, post-handler
        // render pass below, so that any RerenderingContainer subtree reflects server state mutated by the handler.
        this.executeTransactionalAndPublishStagingHandlers(() -> target.flatMap(targetNode -> this.rerenderingService.rerenderTargetNode(targetNode,
                                                                                                                                         data)));

        Optional<DataEventHandler.MappedData> mappedData = target.map(Optional.ofNullable(this.handlers.getActive())
                                                                              .orElse(Collections.emptyMap())::get)
                                                                 .filter(handlers -> !handlers.isEmpty())
                                                                 .flatMap(handlers -> handlers.stream()
                                                                                              .map(handler -> handler.invoke(data.orElse(Data.newInstance()),
                                                                                                                             internalData.orElse(Data.newInstance())))
                                                                                              .reduce((d1, d2) -> d1.mergeWith(d2)))
                                                                 // plan-78 Slice 1: FALLBACK ONLY when the active map misses - resolve by
                                                                 // descending the component tree instead. Does not change the map lookup above,
                                                                 // registerDataEventHandler, or the plan-12 two-pass ordering.
                                                                 .or(() -> this.resolveViaDescentFallback(target, data, internalData));

        Optional<Data> renderData = mappedData.isPresent() ? mappedData.map(DataEventHandler.MappedData::getData) : data;

        // Second render pass: produces the response targetNode using the post-handler data, so the returned node
        // reflects server state mutated inside the handler invoked above.
        Optional<TargetNode> rerenderedNode = this.executeTransactionalAndPublishStagingHandlers(() -> target.flatMap(targetNode -> this.rerenderingService.rerenderTargetNode(targetNode,
                                                                                                                                                                               renderData)));

        return mappedData.map(responseData -> new ResponseBody().setTarget(eventBody.getTarget())
                                                                .setDataWithContext(new DataWithContext(responseData.getData()
                                                                                                                    .getContextId(),
                                                                                                        responseData.getData()
                                                                                                                    .toMap(),
                                                                                                        responseData.getInternalData()
                                                                                                                    .toMap()))
                                                                .setTargetNode(rerenderedNode.orElse(null)));
    }

    /**
     * The submitted {@link Data} a render pass sees: every ui context the page holds, flattened into one lookup.
     *
     * <h2>The defect this exists for</h2>
     * A component reads its own state out of the submitted {@link Data} while rendering - {@code TreeTable} keeps
     * its flat/tree mode, per-column filters, sort spec and load-more window under {@code treetable.<location>.*}.
     * Those fields live in the ROOT ({@code ""}) context, while a form posts under its own context id. So an event
     * raised in the form carried none of them, and the table was re-rendered from DEFAULTS on every such round
     * trip - not stale, RESET. Measured live: a chat submission sent one field under
     * {@code contextId=cardimpl.formimpl} and the response contained the page's table in tree mode with
     * {@code activeFilterCount=0}, discarding a flat view the user had switched on and a filter the agent had just
     * applied.
     *
     * <h2>Why flattening is the right shape</h2>
     * Field lookup is already flat - {@code data.getFieldValue(key)} takes a key and ignores the context id, which
     * only labels the map. Merging therefore needs no reader to change: a component that could find its field
     * before still finds it, and one whose context was absent now finds it too.
     *
     * <h2>Collision rule, and why the originating context wins</h2>
     * Two contexts may in principle use the same key. The originating context is applied LAST because it is the
     * one the user just interacted with, so its value is the freshest thing in the request. In practice components
     * namespace their keys ({@code treetable.<location>.filter.<column>}) and applications name form fields, so a
     * collision means two components have genuinely claimed one key - a bug this cannot paper over, only order.
     *
     * <h2>What is deliberately NOT merged</h2>
     * {@code internalData}, which carries per-form validation feedback. It is read only by the form that wrote it,
     * and merging it would let one form's messages surface under another. The event's own internal data continues
     * to travel alone.
     */
    private Optional<Data> submittedDataAcrossAllContexts(EventBody eventBody)
    {
        DataWithContext originating = eventBody.getDataWithContext();
        if (originating == null)
        {
            return Optional.empty();
        }

        Map<String, Object> merged = new LinkedHashMap<>();
        eventBody.getDataWithContexts()
                 .stream()
                 .filter(Objects::nonNull)
                 .filter(context -> !StringUtils.equals(context.getContextId(), originating.getContextId()))
                 .map(DataWithContext::getData)
                 .filter(Objects::nonNull)
                 .forEach(merged::putAll);
        Optional.ofNullable(originating.getData())
                .ifPresent(merged::putAll);

        return Optional.of(Data.of(originating.getContextId(), merged));
    }

    /**
     * plan-78 Slice 1: FALLBACK ONLY, consulted from {@link #handleEvent(EventBody)} above when the active
     * handler map misses. Null-guarded because {@link #handlerResolver} is never set in
     * {@code EventHandlerServiceImplTest}'s manual (non-Spring) construction.
     */
    private Optional<DataEventHandler.MappedData> resolveViaDescentFallback(Optional<Target> target, Optional<Data> data, Optional<Data> internalData)
    {
        return Optional.ofNullable(this.handlerResolver)
                       .flatMap(resolver -> target.flatMap(resolvedTarget -> resolver.resolve(resolvedTarget, data)))
                       .map(handler -> handler.invoke(data.orElse(Data.newInstance()), internalData.orElse(Data.newInstance())));
    }

    @Override
    public <R> R executeTransactionalAndPublishStagingHandlers(Callable<R> operation)
    {
        try
        {
            R result = operation.call();
            this.handlers.withFinalMergeFunction((staging, active) ->
            {
                Optional.ofNullable(active)
                        .orElse(Collections.emptyMap())
                        .forEach(staging::putIfAbsent);
                return staging;
            })
                         .commit();
            return result;
        }
        catch (Exception e)
        {
            throw new IllegalStateException(e);
        }
    }

}
