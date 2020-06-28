package org.omnaest.react4j.service.internal.handler;

import java.util.Optional;

import org.omnaest.react4j.service.internal.handler.domain.EventBody;
import org.omnaest.react4j.service.internal.handler.domain.ResponseBody;

public interface EventHandlerService
{
    public Optional<ResponseBody> handleEvent(EventBody eventBody);
}
