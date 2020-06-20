package org.omnaest.react.domain;

import java.util.Locale;

import org.omnaest.react.domain.NavigationBar.NavigationBarConsumer;
import org.omnaest.react.domain.NavigationBar.NavigationBarProvider;

public interface ReactUI extends CompositeBase<ReactUI>
{
    public UIComponentFactory componentFactory();

    public ReactUI withDefaultLanguage(Locale locale);

    public ReactUI withNavigationBar(NavigationBarProvider navigationBarProvider);

    public ReactUI withNavigationBar(NavigationBarConsumer navigationBarConsumer);

    public ReactUI withNavigationBar(NavigationBar navigationBar);
}