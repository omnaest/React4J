package org.omnaest.react4j.service.internal.component;

import org.omnaest.react4j.domain.UIComponentFactory;
import org.omnaest.react4j.domain.i18n.UILocale;
import org.omnaest.react4j.service.internal.handler.EventHandlerRegistry;
import org.omnaest.react4j.service.internal.service.ContextFactory;
import org.omnaest.react4j.service.internal.service.LocalizedTextResolverService;

public class ComponentContext
{
    protected UILocale                     defaultLocale;
    protected LocalizedTextResolverService textResolver;
    protected EventHandlerRegistry         eventHandlerRegistry;
    protected UIComponentFactory           uiComponentFactory;
    protected ContextFactory               contextFactory;

    public ComponentContext(UILocale defaultLocale, LocalizedTextResolverService textResolver, EventHandlerRegistry eventHandlerRegistry,
                            UIComponentFactory uiComponentFactory, ContextFactory contextFactory)
    {
        super();
        this.defaultLocale = defaultLocale;
        this.textResolver = textResolver;
        this.eventHandlerRegistry = eventHandlerRegistry;
        this.uiComponentFactory = uiComponentFactory;
        this.contextFactory = contextFactory;
    }

    public UILocale getDefaultLocale()
    {
        return this.defaultLocale;
    }

    public ContextFactory getContextFactory()
    {
        return this.contextFactory;
    }

    public LocalizedTextResolverService getTextResolver()
    {
        return this.textResolver;
    }

    public EventHandlerRegistry getEventHandlerRegistry()
    {
        return this.eventHandlerRegistry;
    }

    public UIComponentFactory getUiComponentFactory()
    {
        return this.uiComponentFactory;
    }

    @Override
    public String toString()
    {
        return "ComponentContext [defaultLocale=" + this.defaultLocale + ", textResolver=" + this.textResolver + ", eventHandlerRegistry="
                + this.eventHandlerRegistry + ", uiComponentFactory=" + this.uiComponentFactory + ", dataContextFactory=" + this.contextFactory + "]";
    }

}
