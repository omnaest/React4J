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

import java.util.List;

import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.support.UIComponentFactoryFunction;
import org.omnaest.react4j.domain.support.UIComponentProvider;
import org.omnaest.react4j.domain.support.UIComponentWithContent;
import org.omnaest.react4j.domain.support.UIContentHolder;

public abstract class AbstractUIComponentAndContentHolder<UIC extends UIComponentWithContent<?>> extends AbstractUIComponentWithSubComponents<UIC>
        implements UIContentHolder<UIC>
{

    public AbstractUIComponentAndContentHolder(ComponentContext context)
    {
        super(context);
    }

    @Override
    public UIC withContent(UIComponentProvider<?> componentProvider)
    {
        return this.withContent(componentProvider.get());
    }

    @Override
    public UIC withContent(List<UIComponent<?>> components)
    {
        return this.withContent(this.getUiComponentFactory()
                                    .newComposite()
                                    .addComponents(components));
    }

    @Override
    public UIC withContent(UIComponentFactoryFunction factoryConsumer)
    {
        return this.withContent(factoryConsumer.apply(this.getUiComponentFactory()));
    }

    @Override
    public UIC withContent(LayoutProvider layout, UIComponent<?> component)
    {
        return this.withContent(layout.apply(this.getUiComponentFactory())
                                      .withContent(component));
    }

    @Override
    public UIC withContent(LayoutProvider layout, UIComponentProvider<?> componentProvider)
    {
        return this.withContent(layout, componentProvider.get());
    }

    @Override
    public UIC withContent(LayoutProvider layout, UIComponentFactoryFunction factoryConsumer)
    {
        return this.withContent(factory -> layout.apply(factory)
                                                 .withContent(factoryConsumer.apply(factory)));
    }

}
