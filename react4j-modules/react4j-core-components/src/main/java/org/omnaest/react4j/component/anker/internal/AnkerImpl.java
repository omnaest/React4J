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
package org.omnaest.react4j.component.anker.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.omnaest.react4j.component.anker.Anker;
import org.omnaest.react4j.component.anker.internal.data.AnkerData;
import org.omnaest.react4j.component.anker.internal.data.AnkerData.AnkerDataBuilder;
import org.omnaest.react4j.component.anker.internal.renderer.AnkerRenderer;
import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.i18n.I18nText;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer;
import org.omnaest.react4j.domain.support.UIComponentProvider;
import org.omnaest.react4j.service.internal.component.AbstractUIComponent;
import org.omnaest.react4j.service.internal.component.ComponentContext;

public class AnkerImpl extends AbstractUIComponent<Anker> implements Anker
{
    private AnkerDataBuilder     data;
    private List<UIComponent<?>> components = new ArrayList<>();

    public AnkerImpl(ComponentContext context)
    {
        this(context, AnkerData.builder());
    }

    public AnkerImpl(ComponentContext context, AnkerDataBuilder ankerData)
    {
        super(context);
        this.data = ankerData;
    }

    public AnkerImpl(ComponentContext context, AnkerDataBuilder ankerData, List<UIComponent<?>> components)
    {
        this(context, ankerData);
        this.components = components;
    }

    @Override
    public UIComponentRenderer asRenderer()
    {
        return new AnkerRenderer(this.getTextResolver(), this.data.build(), this::getId, this.components);
    }

    @Override
    public Anker withText(String text)
    {
        this.data.text(I18nText.of(this.getLocations(), text, this.getDefaultLocale()));
        return this;
    }

    @Override
    public Anker withTitle(String title)
    {
        this.data.title(I18nText.of(this.getLocations(), title, this.getDefaultLocale()));
        return this;
    }

    @Override
    public Anker withNonTranslatedTitle(String title)
    {
        boolean isNonTranslatable = true;
        this.data.title(I18nText.of(this.getLocations(), title, this.getDefaultLocale(), isNonTranslatable));
        return this;
    }

    @Override
    public Anker withNonTranslatedText(String text)
    {
        boolean isNonTranslatable = true;
        this.data.text(I18nText.of(this.getLocations(), text, this.getDefaultLocale(), isNonTranslatable));
        return this;
    }

    @Override
    public Anker withLink(String link)
    {
        this.data.link(link);
        return this;
    }

    @Override
    public Anker withLocator(String locator)
    {
        return this.withLink("#" + locator)
                   .whichOpensOnSamePage();
    }

    @Override
    public Anker whichOpensOnSamePage()
    {
        this.data.isSamePage(true);
        return this;
    }

    @Override
    public Anker addComponent(UIComponent<?> component)
    {
        if (component != null)
        {
            this.components.add(component);
        }
        return this;
    }

    @Override
    public Anker addComponents(List<? extends UIComponent<?>> components)
    {
        if (components != null)
        {
            components.forEach(this::addComponent);
        }
        return this;
    }

    @Override
    public UIComponentProvider<Anker> asTemplateProvider()
    {
        // the builder of the text is shared with the template, the list of children must not be: a component added to a templated instance is not added to
        // the template
        return () -> new AnkerImpl(this.context, this.data, this.components.stream()
                                                                           .collect(Collectors.toList()));
    }
}
