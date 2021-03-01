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

import org.omnaest.react4j.domain.support.UIComponentFactoryFunction;
import org.omnaest.react4j.domain.support.UIComponentProvider;

public interface Jumbotron extends UIComponent<Jumbotron>
{
    public Jumbotron withTitle(String title);

    public Jumbotron addContentLeft(UIComponent<?> component);

    public Jumbotron addContentLeft(UIComponentFactoryFunction factoryConsumer);

    public Jumbotron addContentLeft(UIComponentProvider<?> componentProvider);

    public Jumbotron addContentRight(UIComponent<?> component);

    public Jumbotron addContentRight(UIComponentFactoryFunction factoryConsumer);

    public Jumbotron addContentRight(UIComponentProvider<?> componentProvider);

}
