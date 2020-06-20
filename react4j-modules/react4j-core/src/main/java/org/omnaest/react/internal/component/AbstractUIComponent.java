package org.omnaest.react.internal.component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.omnaest.react.domain.Location;
import org.omnaest.react.domain.Locations;
import org.omnaest.react.domain.UIComponent;
import org.omnaest.react.domain.UIComponentFactory;
import org.omnaest.react.domain.data.DataContext;
import org.omnaest.react.domain.i18n.I18nText;
import org.omnaest.react.domain.i18n.UILocale;
import org.omnaest.react.internal.handler.EventHandlerRegistry;
import org.omnaest.react.internal.service.LocalizedTextResolverService;
import org.omnaest.utils.element.cached.CachedElement;

public abstract class AbstractUIComponent<UIC extends UIComponent<?>> implements UIComponent<UIC>
{
    private static int                   idCounter   = 0;
    private String                       id          = this.getClass()
                                                           .getSimpleName()
                                                           .toLowerCase()
            + idCounter++;
    private List<UIComponent<?>>         parents     = new ArrayList<>();
    protected CachedElement<DataContext> dataContext = CachedElement.of(() -> DataContext.newInstance(this.getLocations()));

    protected ComponentContext context;

    public AbstractUIComponent(ComponentContext context)
    {
        super();
        this.context = context;
    }

    @Override
    public String getId()
    {
        return this.id;
    }

    public AbstractUIComponent<UIC> withId(String id)
    {
        this.id = id;
        return this;
    }

    @Override
    public Locations getLocations()
    {
        return new Locations()
        {
            @Override
            public List<Location> get()
            {
                if (AbstractUIComponent.this.parents.isEmpty())
                {
                    return Arrays.asList(() -> Arrays.asList(AbstractUIComponent.this.id));
                }
                else
                {
                    return AbstractUIComponent.this.parents.stream()
                                                           .flatMap(parent -> Locations.of(parent.getLocations(), AbstractUIComponent.this.id)
                                                                                       .get()
                                                                                       .stream())
                                                           .collect(Collectors.toList());
                }
            }
        };
    }

    @SuppressWarnings("unchecked")
    @Override
    public UIC registerParent(UIComponent<?> parent)
    {
        this.parents.add(parent);
        return (UIC) this;
    }

    protected I18nText toI18nText(String text)
    {
        return I18nText.of(this.getLocations(), text, this.getDefaultLocale());
    }

    protected Function<String, I18nText> i18nTextMapper()
    {
        return text -> I18nText.of(this.getLocations(), text, this.getDefaultLocale());
    }

    public UILocale getDefaultLocale()
    {
        return this.context.getDefaultLocale();
    }

    public LocalizedTextResolverService getTextResolver()
    {
        return this.context.getTextResolver();
    }

    public EventHandlerRegistry getEventHandlerRegistry()
    {
        return this.context.getEventHandlerRegistry();
    }

    public UIComponentFactory getUiComponentFactory()
    {
        return this.context.getUiComponentFactory();
    }

    @SuppressWarnings("unchecked")
    @Override
    public UIC withDataContext(BiConsumer<UIC, DataContext> dataContextConsumer)
    {
        dataContextConsumer.accept((UIC) this, this.dataContext.get());
        return (UIC) this;
    }

}
