package org.omnaest.react.internal.handler;

import org.omnaest.react.internal.handler.domain.DataEventHandler;
import org.omnaest.react.internal.handler.domain.EventHandler;
import org.omnaest.react.internal.handler.domain.Target;

public interface EventHandlerRegistry
{
    public void register(Target target, EventHandler eventHandler);

    public void register(Target target, DataEventHandler eventHandler);
}
