package org.omnaest.react.domain;

import org.omnaest.react.domain.support.UIComponentFactoryFunction;
import org.omnaest.react.domain.support.UIComponentProvider;

public interface Jumbotron extends UIComponent<Jumbotron>
{
    public Jumbotron withTitle(String title);

    public Jumbotron addContentLeft(UIComponent<?> component);

    public Jumbotron addContentLeft(UIComponentFactoryFunction factoryConsumer);

    public Jumbotron addContentLeft(UIComponentProvider<?> componentProvider);

    public Jumbotron addContentRight(UIComponent<?> component);

    public Jumbotron addContentRight(UIComponentFactoryFunction factoryConsumer);

    public Jumbotron addContentRight(UIComponentProvider<?> componentProvider);

}