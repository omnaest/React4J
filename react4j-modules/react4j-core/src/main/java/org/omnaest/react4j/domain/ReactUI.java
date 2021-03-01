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
