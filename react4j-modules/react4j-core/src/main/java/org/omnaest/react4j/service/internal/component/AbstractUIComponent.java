package org.omnaest.react4j.service.internal.component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.Locations;
import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.UIComponentFactory;
import org.omnaest.react4j.domain.data.DataContext;
import org.omnaest.react4j.domain.data.DataContextMapper;
import org.omnaest.react4j.domain.i18n.I18nText;
import org.omnaest.react4j.domain.i18n.UILocale;
import org.omnaest.react4j.service.internal.handler.EventHandlerRegistry;
import org.omnaest.react4j.service.internal.service.DataContextFactory;
import org.omnaest.react4j.service.internal.service.LocalizedTextResolverService;
import org.omnaest.utils.element.cached.CachedElement;

public abstract class AbstractUIComponent<UIC extends UIComponent<?>> implements UIComponent<UIC>
{
    private static int                   idCounter   = 0;
    private String                       id          = this.getClass()
                                                           .getSimpleName()
                                                           .toLowerCase()
            + idCounter++;
    private List<UIComponent<?>>         parents     = new ArrayList<>();
    protected ComponentContext           context;
    protected CachedElement<DataContext> dataContext = CachedElement.of(() -> this.context.getDataContextFactory()
                                                                                          .newInstance(this.getLocations()));

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

    protected UILocale getDefaultLocale()
    {
        return this.context.getDefaultLocale();
    }

    protected LocalizedTextResolverService getTextResolver()
    {
        return this.context.getTextResolver();
    }

    protected EventHandlerRegistry getEventHandlerRegistry()
    {
        return this.context.getEventHandlerRegistry();
    }

    protected UIComponentFactory getUiComponentFactory()
    {
        return this.context.getUiComponentFactory();
    }

    protected DataContextFactory getDataContextFactory()
    {
        return this.context.getDataContextFactory();
    }

    @SuppressWarnings("unchecked")
    public UIC withDataContext(BiConsumer<UIC, DataContext> dataContextConsumer)
    {
        dataContextConsumer.accept((UIC) this, this.dataContext.get());
        return (UIC) this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <DC extends DataContext> UIC withDataContext(DataContextMapper<DC> dataContextType, BiConsumer<UIC, DC> dataContextConsumer)
    {
        dataContextConsumer.accept((UIC) this, (DC) this.dataContext.get());
        return (UIC) this;
    }

}
