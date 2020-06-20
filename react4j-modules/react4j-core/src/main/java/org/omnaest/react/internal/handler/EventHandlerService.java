package org.omnaest.react.internal.handler;

import java.util.Optional;

import org.omnaest.react.internal.handler.domain.EventBody;
import org.omnaest.react.internal.handler.domain.ResponseBody;

public interface EventHandlerService
{
    public Optional<ResponseBody> handleEvent(EventBody eventBody);
}
