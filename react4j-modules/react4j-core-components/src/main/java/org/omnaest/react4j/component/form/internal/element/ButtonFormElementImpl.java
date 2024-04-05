package org.omnaest.react4j.component.form.internal.element;

import java.util.function.Function;
import java.util.function.Supplier;

import org.omnaest.react4j.component.form.Form.ButtonFormElement;
import org.omnaest.react4j.component.form.internal.renderer.node.element.FormElementNode;
import org.omnaest.react4j.component.form.internal.renderer.node.element.FormElementNodeImpl;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.context.Context;
import org.omnaest.react4j.domain.context.data.DataContext;
import org.omnaest.react4j.domain.context.document.Document;
import org.omnaest.react4j.domain.i18n.I18nText;
import org.omnaest.react4j.service.internal.handler.EventHandlerRegistry;
import org.omnaest.react4j.service.internal.handler.domain.DataEventHandler;
import org.omnaest.react4j.service.internal.handler.domain.Target;
import org.omnaest.react4j.service.internal.nodes.handler.ServerHandler;
import org.omnaest.react4j.service.internal.service.LocalizedTextResolverService;

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
    protected FormElementNode renderNode(FormElementNodeImpl node, Location location)
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
        this.eventHandler = data -> eventHandler.apply(data, this.getEffectiveContext());
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
        return this.onClick((data, dataContext) -> dataContext.asDataContext()
                                                              .orElseThrow(() -> new IllegalArgumentException("A DataContext must be provided"))
                                                              .persist(data)
                                                              .get());
    }
}