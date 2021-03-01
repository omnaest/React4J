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

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import org.omnaest.react4j.domain.context.Context;
import org.omnaest.react4j.domain.context.data.Data;
import org.omnaest.react4j.domain.context.document.Document;
import org.omnaest.react4j.domain.raw.FormElementNode;

public interface Form extends UIComponent<Form>
{
    public Form add(Function<FormElementFactory, FormElement<?>> formElementFactoryConsumer);

    public Form add(FormElement<?> formElement);

    public Form addInputField(Consumer<InputFormElement> formElementConsumer);

    public Form addButton(Consumer<ButtonFormElement> formElementConsumer);

    public static interface FormElement<FE extends FormElement<?>>
    {
        public FE withLabel(String label);

        public FE withDescription(String description);

        public FormElementNode render(Location parentLocation);
    }

    public static interface FormFieldElement<FE extends FormFieldElement<?>> extends FormElement<FE>
    {

        public FE attachToField(Document.Field field);
    }

    public static interface InputFormElement extends FormFieldElement<InputFormElement>
    {
        public InputFormElement withPlaceholder(String placeholder);
    }

    public static interface ButtonFormElement extends FormElement<ButtonFormElement>
    {
        public ButtonFormElement attachTo(Document document);

        public ButtonFormElement withText(String text);

        public ButtonFormElement onClick(ButtonEventHandler eventHandler);

        public ButtonFormElement saveOnClick();

        public static interface ButtonEventHandler extends BiFunction<Data, Context, Data>
        {
        }
    }

    public static interface FormElementFactory
    {
        public InputFormElement newInputField();

        public ButtonFormElement newButton();
    }

}
