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

import java.util.function.Supplier;

import org.omnaest.react4j.domain.UIComponentFactory;
import org.omnaest.react4j.domain.i18n.UILocale;
import org.omnaest.react4j.service.internal.handler.EventHandlerRegistry;
import org.omnaest.react4j.service.internal.service.ContextFactory;
import org.omnaest.react4j.service.internal.service.LocalizedTextResolverService;
import org.omnaest.react4j.service.internal.upload.UploadChannelRegistry;

public class ComponentContext
{
    protected UILocale                     defaultLocale;
    protected LocalizedTextResolverService textResolver;
    protected EventHandlerRegistry         eventHandlerRegistry;
    protected UploadChannelRegistry        uploadChannelRegistry;
    protected Supplier<UIComponentFactory> uiComponentFactory;
    protected ContextFactory               contextFactory;
    protected NamedComponentRegistry       namedComponentRegistry;

    public ComponentContext(UILocale defaultLocale, LocalizedTextResolverService textResolver, EventHandlerRegistry eventHandlerRegistry, UploadChannelRegistry uploadChannelRegistry, Supplier<UIComponentFactory> uiComponentFactory, ContextFactory contextFactory)
    {
        this(defaultLocale, textResolver, eventHandlerRegistry, uploadChannelRegistry, uiComponentFactory, contextFactory, null);
    }

    public ComponentContext(UILocale defaultLocale, LocalizedTextResolverService textResolver, EventHandlerRegistry eventHandlerRegistry, UploadChannelRegistry uploadChannelRegistry, Supplier<UIComponentFactory> uiComponentFactory, ContextFactory contextFactory, NamedComponentRegistry namedComponentRegistry)
    {
        super();
        this.defaultLocale = defaultLocale;
        this.textResolver = textResolver;
        this.eventHandlerRegistry = eventHandlerRegistry;
        this.uploadChannelRegistry = uploadChannelRegistry;
        this.uiComponentFactory = uiComponentFactory;
        this.contextFactory = contextFactory;
        this.namedComponentRegistry = namedComponentRegistry;
    }

    /**
     * Where a component publishes the {@link org.omnaest.react4j.domain.Location} it rendered at, so a handler
     * can address it by name later.
     * <p>
     * Null in a hand-constructed context - several tests build one directly, and none of them needs naming. A
     * caller must therefore null-check rather than assume; a component that cannot publish its location simply
     * is not addressable, which is the same outcome as never having been named.
     */
    public NamedComponentRegistry getNamedComponentRegistry()
    {
        return this.namedComponentRegistry;
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

    public UploadChannelRegistry getUploadChannelRegistry()
    {
        return this.uploadChannelRegistry;
    }

    public UIComponentFactory getUiComponentFactory()
    {
        return this.uiComponentFactory.get();
    }

    @Override
    public String toString()
    {
        return "ComponentContext [defaultLocale=" + this.defaultLocale + ", textResolver=" + this.textResolver + ", eventHandlerRegistry="
               + this.eventHandlerRegistry + ", uiComponentFactory=" + this.uiComponentFactory + ", dataContextFactory=" + this.contextFactory + "]";
    }

}
