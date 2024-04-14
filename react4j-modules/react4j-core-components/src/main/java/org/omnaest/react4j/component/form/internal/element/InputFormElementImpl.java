package org.omnaest.react4j.component.form.internal.element;

import java.util.function.Function;
import java.util.function.Supplier;

import org.omnaest.react4j.component.form.Form.InputFormElement;
import org.omnaest.react4j.component.form.internal.renderer.node.element.FormElementNode;
import org.omnaest.react4j.component.form.internal.renderer.node.element.FormElementNode.FormInputNode;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.context.data.DataContext;
import org.omnaest.react4j.domain.context.document.Document;
import org.omnaest.react4j.domain.i18n.I18nText;
import org.omnaest.react4j.service.internal.handler.EventHandlerRegistry;
import org.omnaest.react4j.service.internal.service.LocalizedTextResolverService;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;

public class InputFormElementImpl extends AbstractFormElementImpl<InputFormElement> implements InputFormElement
{
    private InputData.InputDataBuilder data = InputData.builder();

    public InputFormElementImpl(Function<Class<?>, String> identitiyProvider, LocalizedTextResolverService textResolver,
                                Function<String, I18nText> i18nTextMapper, EventHandlerRegistry eventHandlerRegistry,
                                Supplier<? extends DataContext> parentDataContext)
    {
        super(identitiyProvider, textResolver, i18nTextMapper, eventHandlerRegistry, parentDataContext);
    }

    @Override
    public InputFormElement withPlaceholder(String placeholder)
    {
        this.data.placeholder(this.i18nTextMapper.apply(placeholder));
        return this;
    }

    @Override
    protected FormElementNode renderNode(FormElementNode node, Location location)
    {
        InputData inputData = this.data.build();
        return node.toBuilder()
                   .type("INPUT")
                   .input(FormInputNode.builder()
                                       .type(inputData.getInputType()
                                                      .name()
                                                      .toLowerCase())
                                       .placeholder(this.textResolver.apply(inputData.getPlaceholder(), location))
                                       .build())
                   .build();
    }

    @Override
    public InputFormElement attachToField(Document.Field field)
    {
        this.field = field;
        this.document = field.getDocument();
        return this;
    }

    @Override
    public InputFormElement withType(InputType inputType)
    {
        if (inputType != null)
        {
            this.data.inputType(inputType);
        }
        return this;
    }

    @Data
    @Builder
    private static class InputData
    {
        @Default
        private InputType inputType = InputType.TEXT;

        private I18nText placeholder;
    }

}