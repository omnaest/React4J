package org.omnaest.react.internal.component;

import org.omnaest.react.domain.UIComponent;
import org.omnaest.react.domain.UIComponentFactory;
import org.omnaest.react.domain.support.UIComponentFactoryFunction;
import org.omnaest.react.domain.support.UIComponentProvider;
import org.omnaest.react.domain.support.UIContentHolder;

public abstract class AbstractUIContentHolder<R> implements UIContentHolder<R>
{
    private UIComponentFactory uiComponentFactory;

    public AbstractUIContentHolder(UIComponentFactory uiComponentFactory)
    {
        super();
        this.uiComponentFactory = uiComponentFactory;
    }

    @Override
    public R withContent(UIComponentProvider<?> componentProvider)
    {
        return this.withContent(componentProvider.get());
    }

    @Override
    public R withContent(UIComponentFactoryFunction factoryConsumer)
    {
        return this.withContent(factoryConsumer.apply(this.uiComponentFactory));
    }

    @Override
    public R withContent(LayoutProvider layout, UIComponent<?> component)
    {
        return this.withContent(layout.apply(this.uiComponentFactory)
                                      .withContent(component));
    }

    @Override
    public R withContent(LayoutProvider layout, UIComponentProvider<?> componentProvider)
    {
        return this.withContent(layout, componentProvider.get());
    }

    @Override
    public R withContent(LayoutProvider layout, UIComponentFactoryFunction factoryConsumer)
    {
        return this.withContent(factory -> layout.apply(factory)
                                                 .withContent(factoryConsumer.apply(factory)));
    }

}
