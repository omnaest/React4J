package org.omnaest.react.domain;

import java.util.function.Consumer;
import java.util.function.Function;

import org.omnaest.react.domain.data.DataContext;
import org.omnaest.react.domain.raw.FormElementNode;
import org.omnaest.react.internal.handler.domain.DataEventHandler;

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
        public FE attachToField(DataContext dataContext, String field);
    }

    public static interface InputFormElement extends FormFieldElement<InputFormElement>
    {
        public InputFormElement withPlaceholder(String placeholder);
    }

    public static interface ButtonFormElement extends FormElement<ButtonFormElement>
    {
        public ButtonFormElement attachTo(DataContext dataContext);

        public ButtonFormElement withText(String text);

        public ButtonFormElement onClick(DataEventHandler eventHandler);
    }

    public static interface FormElementFactory
    {
        public InputFormElement newInputField();

        public ButtonFormElement newButton();
    }

}