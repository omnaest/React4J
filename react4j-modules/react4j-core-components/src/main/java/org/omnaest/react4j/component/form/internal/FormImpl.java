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
package org.omnaest.react4j.component.form.internal;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

import org.omnaest.react4j.component.form.Form;
import org.omnaest.react4j.component.form.internal.data.FormData;
import org.omnaest.react4j.component.form.internal.element.ButtonFormElementImpl;
import org.omnaest.react4j.component.form.internal.element.DropDownFormElementImpl;
import org.omnaest.react4j.component.form.internal.element.InputFormElementImpl;
import org.omnaest.react4j.component.form.internal.element.RangeFormElementImpl;
import org.omnaest.react4j.component.form.internal.renderer.FormRendererImpl;
import org.omnaest.react4j.domain.context.data.DataContext;
import org.omnaest.react4j.domain.i18n.I18nText;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer;
import org.omnaest.react4j.domain.support.UIComponentProvider;
import org.omnaest.react4j.service.internal.component.AbstractUIComponent;
import org.omnaest.react4j.service.internal.component.ComponentContext;
import org.omnaest.react4j.service.internal.component.uicontext.UIContextManager;
import org.omnaest.react4j.service.internal.handler.EventHandlerRegistry;
import org.omnaest.react4j.service.internal.service.LocalizedTextResolverService;
import org.omnaest.utils.element.cached.CachedElement;

public class FormImpl extends AbstractUIComponent<Form> implements Form
{
    private FormData.FormDataBuilder data;

    public FormImpl(ComponentContext context)
    {
        this(context, FormData.builder());
    }

    public FormImpl(ComponentContext context, FormData.FormDataBuilder data)
    {
        super(context);
        this.data = data;
    }

    @Override
    public UIComponentRenderer asRenderer()
    {
        return new FormRendererImpl(this.data.build(), this::getId);
    }

    @Override
    public Form add(Function<FormElementFactory, FormElement<?>> formElementFactoryConsumer)
    {
        LocalizedTextResolverService textResolver = FormImpl.this.getTextResolver();
        Function<String, I18nText> i18nTextMapper = FormImpl.this.i18nTextMapper();
        EventHandlerRegistry eventHandlerRegistry = this.getEventHandlerRegistry();
        UIContextManager uiContextManager = this.uiContextManager;
        CachedElement<? extends DataContext> dataContext = this.dataContextProvider;
        int numberOfFormElements = this.data.build()
                                            .getElements()
                                            .size();
        FormElement<?> formElement = formElementFactoryConsumer.apply(new FormElementFactory()
        {
            private AtomicInteger buttonCounter = new AtomicInteger(numberOfFormElements);

            @Override
            public InputFormElement newInputField()
            {
                return new InputFormElementImpl(FormImpl.this::newComponentId, textResolver, i18nTextMapper, eventHandlerRegistry, dataContext);
            }

            @Override
            public ButtonFormElement newButton()
            {
                return new ButtonFormElementImpl(FormImpl.this::newComponentId, textResolver, i18nTextMapper, eventHandlerRegistry, dataContext,
                                                 this.buttonCounter.incrementAndGet());
            }

            @Override
            public RangeFormElement newRange()
            {
                return new RangeFormElementImpl(FormImpl.this::newComponentId, textResolver, i18nTextMapper, eventHandlerRegistry, dataContext,
                                                uiContextManager);
            }

            @Override
            public DropDownFormElement newDropDown()
            {
                return new DropDownFormElementImpl(FormImpl.this::newComponentId, textResolver, i18nTextMapper, eventHandlerRegistry, dataContext);
            }

        });
        return this.add(formElement);
    }

    @Override
    public Form add(FormElement<?> formElement)
    {
        this.data.addElement(formElement);
        return this;
    }

    @Override
    public Form addInputField(Consumer<InputFormElement> formElementConsumer)
    {
        return this.add(factory ->
        {
            InputFormElement formElement = factory.newInputField();
            formElementConsumer.accept(formElement);
            return formElement;
        });
    }

    @Override
    public Form addButton(Consumer<ButtonFormElement> formElementConsumer)
    {
        return this.add(factory ->
        {
            ButtonFormElement formElement = factory.newButton();
            formElementConsumer.accept(formElement);
            return formElement;
        });
    }

    @Override
    public Form addRange(Consumer<RangeFormElement> formElementConsumer)
    {
        return this.add(factory ->
        {
            RangeFormElement range = factory.newRange();
            formElementConsumer.accept(range);
            return range;
        });
    }

    @Override
    public Form addDropDown(Consumer<DropDownFormElement> formElementConsumer)
    {
        return this.add(factory ->
        {
            DropDownFormElement dropDown = factory.newDropDown();
            formElementConsumer.accept(dropDown);
            return dropDown;
        });
    }

    @Override
    public UIComponentProvider<Form> asTemplateProvider()
    {
        return () -> new FormImpl(this.context, this.data.build()
                                                         .toBuilder());
    }

    @Override
    public Form withResponsiveness(boolean responsive)
    {
        this.data.responsive(responsive);
        return this;
    }

    @Override
    public Form withDisabledResponsiveness()
    {
        return this.withResponsiveness(false);
    }

}
