package org.omnaest.react4j.service.internal.component.uicontext;

import java.util.Optional;
import java.util.function.Supplier;

import org.omnaest.react4j.domain.Locations;
import org.omnaest.react4j.domain.context.data.Data;
import org.omnaest.react4j.domain.context.document.Document.Field;
import org.omnaest.react4j.domain.context.ui.UIContext;
import org.omnaest.react4j.domain.rendering.RenderableUIComponent.UIContextData;
import org.omnaest.react4j.service.internal.component.ComponentContext;
import org.omnaest.utils.element.cached.CachedElement;

public class UIContextManager
{
    protected final CachedElement<UIContext>     uiContextProvider;
    protected final CachedElement<UIContextData> uiContextInitialDataHolder;

    public UIContextManager(ComponentContext context, Supplier<Locations> locationsProvider)
    {
        super();
        this.uiContextProvider = CachedElement.of(() -> context.getContextFactory()
                                                               .newUIContextInstance(locationsProvider.get()));
        this.uiContextInitialDataHolder = CachedElement.of(() -> UIContextData.builder()
                                                                              .contextIdCreator(this.uiContextProvider.get()::getId)
                                                                              .data(Data.newInstance())
                                                                              .internalData(Data.newInstance())
                                                                              .build());
    }

    public Optional<UIContextData> getUIContextInitialData()
    {
        return this.uiContextInitialDataHolder.getIfCached();
    }

    public void updateInitialDataFieldValue(String field, Object value)
    {
        this.getOrCreateInitialData()
            .setFieldValue(field, value);
    }

    public void updateInitialDataFieldValue(Field field, Object value)
    {
        this.getOrCreateInitialData()
            .setFieldValue(field, value);
    }

    public UIContext getOrCreateUIContext()
    {
        return this.uiContextProvider.get();
    }

    public Data getOrCreateInitialData()
    {
        return this.uiContextInitialDataHolder.get()
                                              .getData();
    }

}