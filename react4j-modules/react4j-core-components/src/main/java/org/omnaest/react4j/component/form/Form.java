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

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import org.omnaest.react4j.component.form.internal.renderer.node.element.FormElementNode;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.context.Context;
import org.omnaest.react4j.domain.context.data.Data;
import org.omnaest.react4j.domain.context.data.Value;
import org.omnaest.react4j.domain.context.document.Document;
import org.omnaest.react4j.domain.context.document.Document.Field;
import org.omnaest.utils.functional.TriFunction;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

public interface Form extends UIComponent<Form>
{
    public Form withResponsiveness(boolean responsive);

    public Form withDisabledResponsiveness();

    public Form add(Function<FormElementFactory, FormElement<?>> formElementFactoryConsumer);

    public Form add(FormElement<?> formElement);

    public Form addInputField(Consumer<InputFormElement> inputField);

    public Form addButton(Consumer<ButtonFormElement> button);

    public Form addRange(Consumer<RangeFormElement> range);

    public Form addDropdown(Consumer<DropDownFormElement> dropdown);

    public Form addCheckbox(Consumer<CheckboxFormElement> checkbox);

    public static interface FormElement<FE extends FormElement<?>>
    {
        public FE withLabel(String label);

        public FE withDescription(String description);

        public FE withColumnSpan(ColumnSpan columnSpan);

        public FE withColumnSpan(int columnSpan);

        public FormElementNode render(Location parentLocation);

        public static enum ColumnSpan
        {
            ONE_COLUMN,
            TWO_COLUMNS,
            THREE_COLUMNS,
            FOUR_COLUMNS,
            FIVE_COLUMNS,
            SIX_COLUMNS,
            SEVEN_COLUMNS,
            EIGHT_COLUMNS,
            NINE_COLUMNS,
            TEN_COLUMNS,
            ELEVEN_COLUMNS,
            TWELVE_COLUMNS
        }
    }

    public static interface FormFieldElement<FE extends FormFieldElement<?>> extends FormElement<FE>
    {
        public FE attachToField(Document.Field field);
    }

    public static interface InputFormElement extends FormFieldElement<InputFormElement>
    {
        public InputFormElement withPlaceholder(String placeholder);

        public InputFormElement withType(InputType inputType);

        public static enum InputType
        {
            TEXT, PASSWORD
        }

    }

    public static interface DropDownFormElement extends FormFieldElement<DropDownFormElement>
    {
        /**
         * Enables the multiselect support. Note that with this flag enabled the {@link Data#getFieldValue(String)} will return a {@link Value#asStringList()}
         * object instead of a simple {@link Value#asString()}.
         * 
         * @param enabled
         * @return
         */
        public DropDownFormElement withMultiselectSupport(boolean enabled);

        /**
         * Similar to {@link #withMultiselectSupport(boolean)} with true as parameter
         * 
         * @return
         */
        public DropDownFormElement withMultiselectSupport();

        public DropDownFormElement withOptions(Consumer<DropDownOptions> options);

        public static interface DropDownOptions
        {
            public DropDownOptions addOption(String key, String label);

            public DropDownOptions addOption(String key, String label, boolean disabled);

            public DropDownOptions addDisabledOption(String key, String label);

            public DropDownOptions addOptions(Map<String, String> options);
        }

    }

    public static interface RangeFormElement extends FormFieldElement<RangeFormElement>
    {
        @Override
        public RangeFormElement withLabel(String label);

        public RangeFormElement withInitialValue(int initialValue);

        public RangeFormElement withMin(int min);

        public RangeFormElement withMax(int max);

        public RangeFormElement withStep(int step);

        public RangeFormElement withDisabled(boolean disabled);

    }

    public static interface ButtonFormElement extends FormElement<ButtonFormElement>
    {
        public ButtonFormElement attachTo(Document document);

        public ButtonFormElement withText(String text);

        public ButtonFormElement withVariant(Variant variant);

        public ButtonFormElement withSize(Size size);

        public ButtonFormElement withOutline(boolean outline);

        public ButtonFormElement withOutline();

        public ButtonFormElement onClick(ButtonEventHandler eventHandler);

        public ButtonFormElement onClick(ButtonEventHandlerWithMessaging eventHandler);

        public ButtonFormElement saveOnClick();

        @Getter
        @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
        public static enum Variant
        {
            REGULAR(""),
            PRIMARY("primary"),
            SECONDARY("secondary"),
            SUCCESS("success"),
            DANGER("danger"),
            WARNING("warning"),
            INFO("info"),
            LIGHT("light"),
            DARK("dark"),
            LINK("link");

            private final String identifier;
        }

        @Getter
        @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
        public static enum Size
        {
            REGULAR(""), SMALL("sm"), LARGE("lg");

            private final String identifier;
        }

        public static interface ButtonEventHandler extends BiFunction<Data, Context, Data>
        {
        }

        public static interface ButtonEventHandlerWithMessaging extends TriFunction<Data, Messaging, Context, Data>
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

    public static interface CheckboxFormElement extends FormFieldElement<CheckboxFormElement>
    {
        @Override
        public CheckboxFormElement withLabel(String label);

        public CheckboxFormElement withInitialValue(boolean initialValue);

        public CheckboxFormElement withDisabled(boolean disabled);

        public CheckboxFormElement withType(CheckboxType checkboxType);

        public static enum CheckboxType
        {
            REGULAR, SWITCH
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

        public DropDownFormElement newDropdown();

        public CheckboxFormElement newCheckbox();
    }

}
