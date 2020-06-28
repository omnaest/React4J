package org.omnaest.react4j.service.internal.component;

import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.support.UIComponentFactoryFunction;
import org.omnaest.react4j.domain.support.UIComponentProvider;
import org.omnaest.react4j.domain.support.UIComponentWithContent;
import org.omnaest.react4j.domain.support.UIContentHolder;

public abstract class AbstractUIComponentAndContentHolder<UIC extends UIComponentWithContent<?>> extends AbstractUIComponentWithSubComponents<UIC>
        implements UIContentHolder<UIC>
{

    public AbstractUIComponentAndContentHolder(ComponentContext context)
    {
        super(context);
    }

    @Override
    public UIC withContent(UIComponentProvider<?> componentProvider)
    {
        return this.withContent(componentProvider.get());
    }

    @Override
    public UIC withContent(UIComponentFactoryFunction factoryConsumer)
    {
        return this.withContent(factoryConsumer.apply(this.getUiComponentFactory()));
    }

    @Override
    public UIC withContent(LayoutProvider layout, UIComponent<?> component)
    {
        return this.withContent(layout.apply(this.getUiComponentFactory())
                                      .withContent(component));
    }

    @Override
    public UIC withContent(LayoutProvider layout, UIComponentProvider<?> componentProvider)
    {
        return this.withContent(layout, componentProvider.get());
    }

    @Override
    public UIC withContent(LayoutProvider layout, UIComponentFactoryFunction factoryConsumer)
    {
        return this.withContent(factory -> layout.apply(factory)
                                                 .withContent(factoryConsumer.apply(factory)));
    }

}
