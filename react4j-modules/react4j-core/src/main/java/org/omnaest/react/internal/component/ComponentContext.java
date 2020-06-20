package org.omnaest.react.internal.component;

import org.omnaest.react.domain.UIComponentFactory;
import org.omnaest.react.domain.i18n.UILocale;
import org.omnaest.react.internal.handler.EventHandlerRegistry;
import org.omnaest.react.internal.service.LocalizedTextResolverService;

public class ComponentContext
{
    protected UILocale                     defaultLocale;
    protected LocalizedTextResolverService textResolver;
    protected EventHandlerRegistry         eventHandlerRegistry;
    protected UIComponentFactory           uiComponentFactory;

    public ComponentContext(UILocale defaultLocale, LocalizedTextResolverService textResolver, EventHandlerRegistry eventHandlerRegistry,
                            UIComponentFactory uiComponentFactory)
    {
        super();
        this.defaultLocale = defaultLocale;
        this.textResolver = textResolver;
        this.eventHandlerRegistry = eventHandlerRegistry;
        this.uiComponentFactory = uiComponentFactory;
    }

    public UILocale getDefaultLocale()
    {
        return this.defaultLocale;
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
                + this.eventHandlerRegistry + ", uiComponentFactory=" + this.uiComponentFactory + "]";
    }

}
