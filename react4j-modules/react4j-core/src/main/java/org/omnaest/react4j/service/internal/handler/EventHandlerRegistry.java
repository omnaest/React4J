package org.omnaest.react4j.service.internal.handler;

import org.omnaest.react4j.service.internal.handler.domain.DataEventHandler;
import org.omnaest.react4j.service.internal.handler.domain.EventHandler;
import org.omnaest.react4j.service.internal.handler.domain.Target;

public interface EventHandlerRegistry
{
    public void register(Target target, EventHandler eventHandler);

    public void register(Target target, DataEventHandler eventHandler);
}
