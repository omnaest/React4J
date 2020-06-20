package org.omnaest.react.internal.handler.internal;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.omnaest.react.domain.data.Data;
import org.omnaest.react.internal.handler.EventHandlerRegistry;
import org.omnaest.react.internal.handler.EventHandlerService;
import org.omnaest.react.internal.handler.domain.DataEventHandler;
import org.omnaest.react.internal.handler.domain.EventBody;
import org.omnaest.react.internal.handler.domain.EventHandler;
import org.omnaest.react.internal.handler.domain.ResponseBody;
import org.omnaest.react.internal.handler.domain.Target;
import org.springframework.stereotype.Service;

@Service
public class EventHandlerServiceImpl implements EventHandlerService, EventHandlerRegistry
{
    private Map<Target, DataEventHandler> handlers = new ConcurrentHashMap<>();

    @Override
    public void register(Target target, DataEventHandler eventHandler)
    {
        if (target != null && eventHandler != null)
        {
            this.handlers.put(target, eventHandler);
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
                       .map(handler -> handler.invoke(Optional.ofNullable(eventBody.getDataWithContext())
                                                              .map(dwc -> Data.of(dwc.getContextId(), dwc.getData()))
                                                              .orElse(Data.EMPTY)))
                       .map(data -> new ResponseBody());
    }

}
