package org.omnaest.react4j.component.form.internal.element;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import org.omnaest.react4j.component.form.Form.InputFormElement;
import org.omnaest.react4j.component.form.internal.renderer.node.element.FormElementNode;
import org.omnaest.react4j.component.form.internal.renderer.node.element.FormElementNodeImpl;
import org.omnaest.react4j.component.form.internal.renderer.node.element.validation.ValidationFeedbackNode;
import org.omnaest.react4j.component.form.internal.renderer.node.element.validation.ValidationMessageNode;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.context.data.DataContext;
import org.omnaest.react4j.domain.context.document.Document;
import org.omnaest.react4j.domain.i18n.I18nText;
import org.omnaest.react4j.service.internal.handler.EventHandlerRegistry;
import org.omnaest.react4j.service.internal.service.LocalizedTextResolverService;
import org.omnaest.utils.PredicateUtils;

import lombok.Builder;
import lombok.Data;

public class InputFormElementImpl extends AbstractFormElementImpl<InputFormElement> implements InputFormElement
{
    private I18nText                placeholder;
    private List<ValidationMessage> validationMessages = new ArrayList<>();

    public InputFormElementImpl(Function<Class<?>, String> identitiyProvider, LocalizedTextResolverService textResolver,
                                Function<String, I18nText> i18nTextMapper, EventHandlerRegistry eventHandlerRegistry,
                                Supplier<? extends DataContext> parentDataContext)
    {
        super(identitiyProvider, textResolver, i18nTextMapper, eventHandlerRegistry, parentDataContext);
    }

    @Override
    public InputFormElement withPlaceholder(String placeholder)
    {
        this.placeholder = this.i18nTextMapper.apply(placeholder);
        return this;
    }

    @Override
    protected FormElementNode renderNode(FormElementNodeImpl node, Location location)
    {
        return node.toBuilder()
                   .type("INPUT")
                   .placeholder(this.textResolver.apply(this.placeholder, location))
                   .validationFeedback(this.validationMessages.isEmpty() ? null
                           : ValidationFeedbackNode.builder()
                                                   .valid(this.validationMessages.stream()
                                                                                 .map(ValidationMessage::getType)
                                                                                 .filter(PredicateUtils.notEquals(ValidationMessageType.VALID))
                                                                                 .findAny()
                                                                                 .isEmpty())
                                                   .messages(this.validationMessages.stream()
                                                                                    .map(message -> ValidationMessageNode.builder()
                                                                                                                         .type(ValidationMessageTypeMapper.mapToNodeType(message.getType()))
                                                                                                                         .text(this.textResolver.apply(message.getText(),
                                                                                                                                                       location))
                                                                                                                         .build())
                                                                                    .toList())
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
    public InputFormElement addValidationMessage(ValidationMessageType validationMessageType, String message)
    {
        this.validationMessages.add(ValidationMessage.builder()
                                                     .type(validationMessageType)
                                                     .text(this.i18nTextMapper.apply(message))
                                                     .build());
        return this;
    }

    @Override
    public InputFormElement addValidationMessage(String message)
    {
        return this.addValidationMessage(ValidationMessageType.INVALID, message);
    }

    @Data
    @Builder
    private static class ValidationMessage
    {
        private ValidationMessageType type;
        private I18nText              text;
    }
}