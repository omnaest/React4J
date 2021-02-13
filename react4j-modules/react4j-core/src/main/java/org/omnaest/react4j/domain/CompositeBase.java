package org.omnaest.react4j.domain;

import java.util.List;

import org.omnaest.react4j.domain.support.UIComponentFactoryFunction;
import org.omnaest.react4j.domain.support.UIComponentProvider;

public interface CompositeBase<R>
{
    public R addNewComponent(UIComponentFactoryFunction factoryConsumer);

    public R addComponent(UIComponent<?> component);

    public R addComponents(List<UIComponent<?>> components);

    public R addComponent(UIComponentProvider<?> componentProvider);

}