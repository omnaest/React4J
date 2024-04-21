/*******************************************************************************
 * Copyright 2021 Danny Kunz
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package org.omnaest.react4j.service.internal.component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.omnaest.react4j.component.value.field.TextFieldSource;
import org.omnaest.react4j.component.value.i18n.I18nTextValueSource;
import org.omnaest.react4j.component.value.i18n.internal.I18nTextValueSourceImpl;
import org.omnaest.react4j.component.value.internal.TextFieldSourceImpl;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.Locations;
import org.omnaest.react4j.domain.RerenderingContainer;
import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.UIComponentFactory;
import org.omnaest.react4j.domain.context.data.DefineableDataContext;
import org.omnaest.react4j.domain.context.data.TypedDataContext;
import org.omnaest.react4j.domain.context.document.Document.Field;
import org.omnaest.react4j.domain.context.ui.UIContext;
import org.omnaest.react4j.domain.i18n.I18nText;
import org.omnaest.react4j.domain.i18n.UILocale;
import org.omnaest.react4j.domain.rendering.RenderableUIComponent;
import org.omnaest.react4j.service.internal.component.uicontext.UIContextManager;
import org.omnaest.react4j.service.internal.handler.EventHandlerRegistry;
import org.omnaest.react4j.service.internal.service.ContextFactory;
import org.omnaest.react4j.service.internal.service.LocalizedTextResolverService;
import org.omnaest.react4j.service.internal.service.LocalizedTextResolverService.LocationAwareTextResolver;
import org.omnaest.utils.ConsumerUtils;
import org.omnaest.utils.element.cached.CachedElement;

public abstract class AbstractUIComponent<UIC extends UIComponent<?>> implements RenderableUIComponent<UIC>
{
    private String                                 id                  = this.newComponentId(this.getClass());
    private List<UIComponent<?>>                   parents             = new ArrayList<>();
    protected ComponentContext                     context;
    protected CachedElement<DefineableDataContext> dataContextProvider = CachedElement.of(() -> this.context.getContextFactory()
                                                                                                            .newDataContextInstance(this.getLocations()));
    protected UIContextManager                     uiContextManager;

    public AbstractUIComponent(ComponentContext context)
    {
        super();
        this.context = context;
        this.uiContextManager = new UIContextManager(context, this::getLocations);
    }

    protected String newComponentId(Class<?> type)
    {
        return type.getSimpleName()
                   .toLowerCase();
    }

    @Override
    public Optional<UIContextData> getUIContextInitialDataIfPresent()
    {
        return this.uiContextManager.getUIContextInitialData();
    }

    @Override
    public UIComponentWrapper<UIC> getWrapper()
    {
        return (factory, component) -> component;
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

    protected I18nTextValueSource toI18nTextValueSource(String text)
    {
        return I18nTextValueSourceImpl.of(this.toI18nText(text));
    }

    protected TextFieldSource toTextFieldSource(Field field)
    {
        return TextFieldSourceImpl.of(field);
    }

    protected I18nText toNonTranslatableI18nText(String text)
    {
        return I18nText.of(this.getLocations(), text, this.getDefaultLocale(), true);
    }

    protected Function<String, I18nText> i18nTextMapper()
    {
        return text -> I18nText.of(this.getLocations(), text, this.getDefaultLocale());
    }

    protected UILocale getDefaultLocale()
    {
        return this.context.getDefaultLocale();
    }

    protected LocationAwareTextResolver getLocationAwareTextResolver(Location location)
    {
        return this.getTextResolver()
                   .apply(location);
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

    protected ContextFactory getDataContextFactory()
    {
        return this.context.getContextFactory();
    }

    @Override
    public UIC withUIContext(UIContextConsumer<UIC> uiContextConsumer)
    {
        return this.withUIContext((component, context, data) -> uiContextConsumer.accept(component, context));
    }

    @SuppressWarnings("unchecked")
    @Override
    public UIC withUIContext(UIContextAndDataConsumer<UIC> uiContextConsumer)
    {
        UIContext uiContext = this.uiContextManager.getOrCreateUIContext();
        ConsumerUtils.consumeWith((UIC) this, uiContext, this.uiContextManager.getOrCreateInitialData(), uiContextConsumer);
        return (UIC) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public UIC withDataContext(BiConsumer<UIC, DefineableDataContext> dataContextConsumer)
    {
        dataContextConsumer.accept((UIC) this, this.dataContextProvider.get());
        return (UIC) this;
    }

    @Override
    public <T> UIC withDataContext(Class<T> type, BiConsumer<UIC, TypedDataContext<T>> dataContextConsumer)
    {
        return this.withDataContext((component, dataContext) -> dataContextConsumer.accept(component, dataContext.asTypedDataContext(type)));
    }

    @Override
    public RerenderingContainer withRerenderingUIContext(UIContextAndDataConsumer<UIC> rerenderingUIContextConsumer)
    {
        UIContext uiContext = this.uiContextManager.getOrCreateUIContext();
        return this.getUiComponentFactory()
                   .newRerenderingContainer()
                   .withDataDrivenContent(data ->
                   {
                       UIC clone = this.asTemplateProvider()
                                       .get();
                       rerenderingUIContextConsumer.accept(clone, uiContext, data);
                       return clone;
                   })
                   .withUIContext(uiContext)
                   .enableStaticNodeRerendering();
    }

}
