package org.omnaest.react4j.component.form.internal.element;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.omnaest.react4j.component.form.Form.ButtonFormElement;
import org.omnaest.react4j.component.form.Form.ValidationMessageType;
import org.omnaest.react4j.component.form.internal.renderer.node.element.FormElementNode;
import org.omnaest.react4j.component.form.internal.renderer.node.element.validation.ValidationFeedbackNode;
import org.omnaest.react4j.component.form.internal.renderer.node.element.validation.ValidationMessageNode;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.context.Context;
import org.omnaest.react4j.domain.context.data.Data;
import org.omnaest.react4j.domain.context.data.DataContext;
import org.omnaest.react4j.domain.context.document.Document;
import org.omnaest.react4j.domain.context.document.Document.Field;
import org.omnaest.react4j.domain.i18n.I18nText;
import org.omnaest.react4j.service.internal.handler.EventHandlerRegistry;
import org.omnaest.react4j.service.internal.handler.domain.DataEventHandler;
import org.omnaest.react4j.service.internal.handler.domain.DataEventHandler.MappedData;
import org.omnaest.react4j.service.internal.handler.domain.Target;
import org.omnaest.react4j.service.internal.nodes.handler.ServerHandler;
import org.omnaest.react4j.service.internal.service.LocalizedTextResolverService;
import org.omnaest.utils.PredicateUtils;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

public class ButtonFormElementImpl extends AbstractFormElementImpl<ButtonFormElement> implements ButtonFormElement
{
    private I18nText         text;
    private DataEventHandler eventHandler;

    public ButtonFormElementImpl(Function<Class<?>, String> identityProvider, LocalizedTextResolverService textResolver,
                                 Function<String, I18nText> i18nTextMapper, EventHandlerRegistry eventHandlerRegistry,
                                 Supplier<? extends DataContext> parentDataContext)
    {
        super(identityProvider, textResolver, i18nTextMapper, eventHandlerRegistry, parentDataContext);
    }

    @Override
    protected FormElementNode renderNode(FormElementNode node, Location location)
    {
        Context dataContext = this.getEffectiveContext();
        Target target = Target.from(location);
        this.eventHandlerRegistry.registerDataEventHandler(target, this.eventHandler);
        node.setType("BUTTON")
            .setText(this.textResolver.apply(this.text, location))
            .setOnClick(new ServerHandler(target).setContextId(dataContext.getId(location)));
        return node;
    }

    @Override
    public ButtonFormElement withText(String text)
    {
        this.text = this.i18nTextMapper.apply(text);
        return this;
    }

    @Override
    public ButtonFormElement onClick(ButtonEventHandler eventHandler)
    {
        this.eventHandler = (previousData, previousInternalData) ->
        {
            MessagingImpl messaging = new MessagingImpl(this.i18nTextMapper, this.textResolver);
            Data data = eventHandler.apply(previousData, messaging, this.getEffectiveContext());
            Data internalData = previousInternalData.clone()
                                                    .setFieldValue("validationFeedback", messaging.build()
                                                                                                  .entrySet()
                                                                                                  .stream()
                                                                                                  .collect(Collectors.toMap(entry -> entry.getKey(),
                                                                                                                            entry -> entry.getValue())));
            return MappedData.builder()
                             .data(data)
                             .internalData(internalData)
                             .build();
        };
        return this;
    }

    @Override
    public ButtonFormElement attachTo(Document document)
    {
        this.document = document;
        return this;
    }

    @Override
    public ButtonFormElement saveOnClick()
    {
        return this.onClick((data, messages, dataContext) -> dataContext.asDataContext()
                                                                        .orElseThrow(() -> new IllegalArgumentException("A DataContext must be provided"))
                                                                        .persist(data)
                                                                        .get());
    }

    @RequiredArgsConstructor
    public static class MessagingImpl implements Messaging
    {
        private final Function<String, I18nText>      i18nTextMapper;
        private final LocalizedTextResolverService    textResolver;
        private final Map<String, ValidationMessages> messages = new HashMap<>();

        private static class ValidationMessages
        {
            @Getter
            private final List<ValidationMessageNode> messages = new ArrayList<>();

            public void addMessage(ValidationMessageNode validationMessageNode)
            {
                this.messages.add(validationMessageNode);
            }

            public boolean hasAnyInvalidMessage()
            {
                return this.messages.stream()
                                    .map(ValidationMessageNode::getType)
                                    .filter(PredicateUtils.equalsAnyOf(ValidationMessageNode.ValidationMessageType.INVALID))
                                    .findAny()
                                    .isEmpty();
            }

            public boolean hasAnyMessage()
            {
                return !this.messages.isEmpty();
            }

        }

        @Override
        public Messaging addValidationMessage(String field, String text)
        {
            return this.addValidationMessage(field, ValidationMessageType.INVALID, text);
        }

        @Override
        public Messaging addValidationMessage(Field field, String text)
        {
            return this.addValidationMessage(field.getFieldName(), text);
        }

        @Override
        public Messaging addValidationMessage(Field field, ValidationMessageType validationMessageType, String text)
        {
            return this.addValidationMessage(field.getFieldName(), validationMessageType, text);
        }

        @Override
        public Messaging addValidationMessage(String field, ValidationMessageType validationMessageType, String text)
        {
            this.messages.computeIfAbsent(field, f -> new ValidationMessages())
                         .addMessage(ValidationMessageNode.builder()
                                                          .type(ValidationMessageTypeMapper.mapToNodeType(validationMessageType))
                                                          .text(this.textResolver.apply(this.i18nTextMapper.apply(text), Location.empty()))
                                                          .build());
            return this;
        }

        public Map<String, ValidationFeedbackNode> build()
        {
            return this.messages.entrySet()
                                .stream()
                                .filter(entry -> entry.getValue()
                                                      .hasAnyMessage())
                                .collect(Collectors.toMap(entry -> entry.getKey(), entry ->
                                {
                                    ValidationMessages validationMessages = entry.getValue();
                                    return ValidationFeedbackNode.builder()
                                                                 .valid(validationMessages.hasAnyInvalidMessage())
                                                                 .messages(validationMessages.getMessages())
                                                                 .build();
                                }));
        }

    }
}