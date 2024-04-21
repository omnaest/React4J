package org.omnaest.react4j.domain.support;

import java.util.function.BiConsumer;

import org.omnaest.react4j.domain.context.ui.UIContext;

public interface ComponentAndUIContextConsumer<C> extends BiConsumer<C, UIContext>
{

}