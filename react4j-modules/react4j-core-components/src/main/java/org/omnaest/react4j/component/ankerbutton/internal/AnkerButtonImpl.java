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
package org.omnaest.react4j.component.ankerbutton.internal;

import org.omnaest.react4j.component.ankerbutton.AnkerButton;
import org.omnaest.react4j.component.ankerbutton.internal.data.AnkerButtonData;
import org.omnaest.react4j.component.ankerbutton.internal.renderer.AnkerButtonRenderer;
import org.omnaest.react4j.domain.Button.Style;
import org.omnaest.react4j.domain.i18n.I18nText;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer;
import org.omnaest.react4j.domain.support.UIComponentProvider;
import org.omnaest.react4j.service.internal.component.AbstractUIComponent;
import org.omnaest.react4j.service.internal.component.ComponentContext;

public class AnkerButtonImpl extends AbstractUIComponent<AnkerButton> implements AnkerButton
{
    private final AnkerButtonData.AnkerButtonDataBuilder data;

    public AnkerButtonImpl(ComponentContext context)
    {
        this(context, AnkerButtonData.builder());
    }

    public AnkerButtonImpl(ComponentContext context, AnkerButtonData.AnkerButtonDataBuilder data)
    {
        super(context);
        this.data = data;
    }

    @Override
    public UIComponentRenderer asRenderer()
    {
        return new AnkerButtonRenderer(this.data.build(), AnkerButtonImpl.this.getTextResolver(), this::getId);
    }

    @Override
    public AnkerButton withText(String text)
    {
        this.data.text(I18nText.of(this.getLocations(), text, this.getDefaultLocale()));
        return this;
    }

    @Override
    public AnkerButton withLink(String link)
    {
        this.data.link(link);
        return this;
    }

    @Override
    public AnkerButton withStyle(Style style)
    {
        this.data.style(style);
        return this;
    }

    @Override
    public AnkerButton withLocator(String locator)
    {
        return this.withLink("#" + locator)
                   .whichOpensOnSamePage();
    }

    @Override
    public AnkerButton whichOpensOnSamePage()
    {
        this.data.isSamePage(true);
        return this;
    }

    @Override
    public UIComponentProvider<AnkerButton> asTemplateProvider()
    {
        return () -> new AnkerButtonImpl(this.context, this.data);
    }
}
