package org.omnaest.react4j.service.internal.handler.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import org.springframework.stereotype.Service;

@Service
public class EventHandlerServiceImpl implements EventHandlerService, EventHandlerRegistry
{
    private Map<Target, List<DataEventHandler>> handlers = new ConcurrentHashMap<>();

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
        return Optional.ofNullable(eventBody)
                       .map(body -> body.getTarget())
                       .map(target -> this.handlers.get(target))
                       .filter(handlers -> !handlers.isEmpty())
                       .flatMap(handlers -> handlers.stream()
                                                    .map(handler -> handler.invoke(Optional.ofNullable(eventBody.getDataWithContext())
                                                                                           .map(dwc -> Data.of(dwc.getContextId(), dwc.getData()))
                                                                                           .orElse(Data.EMPTY)))
                                                    .reduce((d1, d2) -> d1.mergeWith(d2)))
                       .map(data -> new ResponseBody().setTarget(eventBody.getTarget())
                                                      .setDataWithContext(new DataWithContext(data.getContextId(), data.toMap())));
    }

}
