package org.omnaest.react.domain;

import org.omnaest.react.domain.support.UIComponentFactoryFunction;
import org.omnaest.react.domain.support.UIComponentProvider;

public interface CompositeBase<R>
{
    public R addNewComponent(UIComponentFactoryFunction factoryConsumer);

    public R addComponent(UIComponent<?> component);

    public R addComponent(UIComponentProvider<?> componentProvider);

}