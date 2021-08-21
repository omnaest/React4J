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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

import org.omnaest.react4j.domain.context.data.Data;
import org.omnaest.react4j.service.internal.handler.EventHandlerRegistry;
import org.omnaest.react4j.service.internal.handler.EventHandlerService;
import org.omnaest.react4j.service.internal.handler.domain.DataEventHandler;
import org.omnaest.react4j.service.internal.handler.domain.DataWithContext;
import org.omnaest.react4j.service.internal.handler.domain.EventBody;
import org.omnaest.react4j.service.internal.handler.domain.EventHandler;
import org.omnaest.react4j.service.internal.handler.domain.ResponseBody;
import org.omnaest.react4j.service.internal.handler.domain.Target;
import org.omnaest.react4j.service.internal.handler.domain.TargetNode;
import org.omnaest.react4j.service.internal.rerenderer.RerenderingService;
import org.omnaest.utils.element.transactional.TransactionalElement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EventHandlerServiceImpl implements EventHandlerService, EventHandlerRegistry
{
    private TransactionalElement<Map<Target, List<DataEventHandler>>> handlers = this.createTransactionalHandlerMap();

    @Autowired
    protected RerenderingService rerenderingService;

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
            List<DataEventHandler> handlers = this.handlers.getStaging()
                                                           .computeIfAbsent(target, t -> Collections.synchronizedList(new ArrayList<>()));
            handlers.add(eventHandler);
        }
    }

    @Override
    public void registerEventHandler(Target target, EventHandler eventHandler)
    {
        this.registerDataEventHandler(target, data ->
        {
            eventHandler.invoke();
            return data;
        });
    }

    @Override
    public Optional<ResponseBody> handleEvent(EventBody eventBody)
    {
        Optional<Target> target = Optional.ofNullable(eventBody)
                                          .map(EventBody::getTarget);
        Optional<Data> data = Optional.ofNullable(eventBody.getDataWithContext())
                                      .map(dwc -> Data.of(dwc.getContextId(), dwc.getData()));
        Optional<TargetNode> rerenderedNode = this.executeTransactionalAndPublishStagingHandlers(() -> target.flatMap(targetNode -> this.rerenderingService.rerenderTargetNode(targetNode,
                                                                                                                                                                               data)));

        return target.map(Optional.ofNullable(this.handlers.getActive())
                                  .orElse(Collections.emptyMap())::get)
                     .filter(handlers -> !handlers.isEmpty())
                     .flatMap(handlers -> handlers.stream()
                                                  .map(handler -> handler.invoke(data.orElse(Data.EMPTY)))
                                                  .reduce((d1, d2) -> d1.mergeWith(d2)))
                     .map(responseData -> new ResponseBody().setTarget(eventBody.getTarget())
                                                            .setDataWithContext(new DataWithContext(responseData.getContextId(), responseData.toMap()))
                                                            .setTargetNode(rerenderedNode.orElse(null)));
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
