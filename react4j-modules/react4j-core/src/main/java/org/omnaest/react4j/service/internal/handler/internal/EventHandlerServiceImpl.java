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
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.omnaest.react4j.domain.Location;
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
import org.omnaest.utils.StreamUtils;
import org.omnaest.utils.element.bi.BiElement;
import org.springframework.stereotype.Service;

@Service
public class EventHandlerServiceImpl implements EventHandlerService, EventHandlerRegistry
{
    private Map<Target, List<DataEventHandler>> handlers                = new ConcurrentHashMap<>();
    private Map<Target, RerenderedNodeProvider> rerenderedNodeProviders = new ConcurrentHashMap<>();

    @Override
    public void register(Target target, DataEventHandler eventHandler)
    {
        if (target != null && eventHandler != null)
        {
            this.handlers.computeIfAbsent(target, t -> Collections.synchronizedList(new ArrayList<>()))
                         .add(eventHandler);
        }
    }

    @Override
    public void register(Target target, EventHandler eventHandler)
    {
        this.register(target, data ->
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
        List<Target> currentAndAllParentalTargets = StreamUtils.recursiveFlattened(target.map(Stream::of)
                                                                                         .orElse(Stream.empty()),
                                                                                   parentTarget -> Stream.of(Target.from(Location.of(parentTarget)
                                                                                                                                 .getParent())))
                                                               .distinct()
                                                               .collect(Collectors.toList());
        Optional<TargetNode> rerenderedNode = currentAndAllParentalTargets.stream()
                                                                          .filter(this.rerenderedNodeProviders::containsKey)
                                                                          .findFirst()
                                                                          .map(iTarget -> BiElement.of(iTarget, this.rerenderedNodeProviders.get(iTarget)))
                                                                          .filter(BiElement::hasNoNullValue)
                                                                          .map(targetAndNodeProvider -> new TargetNode(targetAndNodeProvider.getFirst(),
                                                                                                                       targetAndNodeProvider.getSecond()
                                                                                                                                            .get()));
        return target.map(this.handlers::get)
                     .filter(handlers -> !handlers.isEmpty())
                     .flatMap(handlers -> handlers.stream()
                                                  .map(handler -> handler.invoke(Optional.ofNullable(eventBody.getDataWithContext())
                                                                                         .map(dwc -> Data.of(dwc.getContextId(), dwc.getData()))
                                                                                         .orElse(Data.EMPTY)))
                                                  .reduce((d1, d2) -> d1.mergeWith(d2)))
                     .map(data -> new ResponseBody().setTarget(eventBody.getTarget())
                                                    .setDataWithContext(new DataWithContext(data.getContextId(), data.toMap()))
                                                    .setTargetNode(rerenderedNode.orElse(null)));
    }

    @Override
    public void register(Target target, RerenderedNodeProvider rerenderedNodeProvider)
    {
        this.rerenderedNodeProviders.put(target, rerenderedNodeProvider);
    }

}
