package org.omnaest.react4j.domain;

import java.util.Locale;
import java.util.function.Consumer;

import org.omnaest.react4j.domain.NavigationBar.NavigationBarConsumer;
import org.omnaest.react4j.domain.NavigationBar.NavigationBarProvider;
import org.omnaest.react4j.domain.configuration.HomePageConfiguration;

/**
 * Root of an {@link ReactUI} application page
 * 
 * @see #addComponent(UIComponent)
 * @see #addNewComponent(org.omnaest.react4j.domain.support.UIComponentFactoryFunction)
 * @see #configureHomePage(Consumer)
 * @author omnaest
 */
public interface ReactUI extends CompositeBase<ReactUI>
{
    /**
     * Provides the central {@link UIComponentFactory} instance
     * 
     * @return
     */
    public UIComponentFactory componentFactory();

    public ReactUI withDefaultLanguage(Locale locale);

    public ReactUI withNavigationBar(NavigationBarProvider navigationBarProvider);

    public ReactUI withNavigationBar(NavigationBarConsumer navigationBarConsumer);

    public ReactUI withNavigationBar(NavigationBar navigationBar);

    /**
     * Configures the index.html file
     * 
     * @param configurationConsumer
     * @return
     */
    public ReactUI configureHomePage(Consumer<HomePageConfiguration> configurationConsumer);
}