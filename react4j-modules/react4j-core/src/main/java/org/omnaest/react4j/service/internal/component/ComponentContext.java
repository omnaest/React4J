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
