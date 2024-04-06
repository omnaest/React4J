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
package org.omnaest.react4j.component.form;

import java.util.function.Consumer;
import java.util.function.Function;

import org.omnaest.react4j.component.form.internal.renderer.node.element.FormElementNode;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.context.Context;
import org.omnaest.react4j.domain.context.data.Data;
import org.omnaest.react4j.domain.context.document.Document;
import org.omnaest.react4j.domain.context.document.Document.Field;
import org.omnaest.utils.functional.TriFunction;

public interface Form extends UIComponent<Form>
{
    public Form add(Function<FormElementFactory, FormElement<?>> formElementFactoryConsumer);

    public Form add(FormElement<?> formElement);

    public Form addInputField(Consumer<InputFormElement> formElementConsumer);

    public Form addButton(Consumer<ButtonFormElement> formElementConsumer);

    public Form addRange(Consumer<RangeFormElement> formElementConsumer);

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

    public static interface RangeFormElement extends FormFieldElement<RangeFormElement>
    {
        @Override
        public RangeFormElement withLabel(String label);

        public RangeFormElement withMin(int min);

        public RangeFormElement withMax(int max);

        public RangeFormElement withStep(int step);

        public RangeFormElement withDisabled(boolean disabled);
    }

    public static interface ButtonFormElement extends FormElement<ButtonFormElement>
    {
        public ButtonFormElement attachTo(Document document);

        public ButtonFormElement withText(String text);

        public ButtonFormElement onClick(ButtonEventHandler eventHandler);

        public ButtonFormElement saveOnClick();

        public static interface ButtonEventHandler extends TriFunction<Data, Messaging, Context, Data>
        {
        }

        public static interface Messaging
        {
            public Messaging addValidationMessage(String field, String text);

            public Messaging addValidationMessage(Field field, String text);

            public Messaging addValidationMessage(String field, ValidationMessageType validationMessageType, String text);

            public Messaging addValidationMessage(Field field, ValidationMessageType validationMessageType, String text);
        }
    }

    public static enum ValidationMessageType
    {
        VALID, INVALID
    }

    public static interface FormElementFactory
    {
        public InputFormElement newInputField();

        public ButtonFormElement newButton();

        public RangeFormElement newRange();
    }
}
