package org.omnaest.react4j.domain;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import org.omnaest.react4j.domain.data.Data;
import org.omnaest.react4j.domain.data.DataContext;
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

        public FE attachToField(DataContext.Field field);
    }

    public static interface InputFormElement extends FormFieldElement<InputFormElement>
    {
        public InputFormElement withPlaceholder(String placeholder);
    }

    public static interface ButtonFormElement extends FormElement<ButtonFormElement>
    {
        public ButtonFormElement attachTo(DataContext dataContext);

        public ButtonFormElement withText(String text);

        public ButtonFormElement onClick(ButtonEventHandler eventHandler);

        public ButtonFormElement saveOnClick();

        public static interface ButtonEventHandler extends BiFunction<Data, DataContext, Data>
        {
        }
    }

    public static interface FormElementFactory
    {
        public InputFormElement newInputField();

        public ButtonFormElement newButton();
    }

}