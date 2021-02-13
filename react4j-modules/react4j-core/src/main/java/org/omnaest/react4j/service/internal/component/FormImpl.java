package org.omnaest.react4j.service.internal.component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.omnaest.react4j.domain.Form;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.data.DataContext;
import org.omnaest.react4j.domain.data.DataContext.Field;
import org.omnaest.react4j.domain.i18n.I18nText;
import org.omnaest.react4j.domain.raw.FormElementNode;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.raw.UIComponentRenderer;
import org.omnaest.react4j.service.internal.handler.EventHandlerRegistry;
import org.omnaest.react4j.service.internal.handler.domain.DataEventHandler;
import org.omnaest.react4j.service.internal.handler.domain.Target;
import org.omnaest.react4j.service.internal.nodes.FormNode;
import org.omnaest.react4j.service.internal.nodes.FormNode.FormElementNodeImpl;
import org.omnaest.react4j.service.internal.nodes.handler.ServerHandler;
import org.omnaest.react4j.service.internal.service.LocalizedTextResolverService;
import org.omnaest.utils.element.cached.CachedElement;

public class FormImpl extends AbstractUIComponent<Form> implements Form
{
    private List<FormElement<?>> elements = new ArrayList<>();

    public FormImpl(ComponentContext context)
    {
        super(context);
    }

    @Override
    public UIComponentRenderer asRenderer()
    {
        return new UIComponentRenderer()
        {
            @Override
            public Node render(Location parentLocation)
            {
                Location location = Location.of(parentLocation, FormImpl.this.getId());
                return new FormNode().setElements(FormImpl.this.elements.stream()
                                                                        .map(element -> element.render(location))
                                                                        .collect(Collectors.toList()));
            }
        };
    }

    @Override
    public Form add(Function<FormElementFactory, FormElement<?>> formElementFactoryConsumer)
    {
        LocalizedTextResolverService textResolver = FormImpl.this.getTextResolver();
        Function<String, I18nText> i18nTextMapper = FormImpl.this.i18nTextMapper();
        EventHandlerRegistry eventHandlerRegistry = this.getEventHandlerRegistry();
        CachedElement<? extends DataContext> dataContext = this.dataContext;
        FormElement<?> formElement = formElementFactoryConsumer.apply(new FormElementFactory()
        {
            @Override
            public InputFormElement newInputField()
            {
                return new InputFormElementImpl(FormImpl.this::newComponentId, textResolver, i18nTextMapper, eventHandlerRegistry, dataContext);
            }

            @Override
            public ButtonFormElement newButton()
            {
                return new ButtonFormElementImpl(FormImpl.this::newComponentId, textResolver, i18nTextMapper, eventHandlerRegistry, dataContext);
            }
        });
        return this.add(formElement);
    }

    @Override
    public Form add(FormElement<?> formElement)
    {
        this.elements.add(formElement);
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

    protected static class ButtonFormElementImpl extends AbstractFormElementImpl<ButtonFormElement> implements ButtonFormElement
    {
        private I18nText         text;
        private DataEventHandler eventHandler;

        protected ButtonFormElementImpl(Function<Class<?>, String> identityProvider, LocalizedTextResolverService textResolver,
                                        Function<String, I18nText> i18nTextMapper, EventHandlerRegistry eventHandlerRegistry,
                                        Supplier<? extends DataContext> parentDataContext)
        {
            super(identityProvider, textResolver, i18nTextMapper, eventHandlerRegistry, parentDataContext);
        }

        @Override
        protected FormElementNode renderNode(FormElementNodeImpl node, Location location)
        {
            DataContext dataContext = this.getEffectiveDataContext();
            Target target = Target.from(location);
            this.eventHandlerRegistry.register(target, this.eventHandler);
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
            this.eventHandler = data -> eventHandler.apply(data, this.getEffectiveDataContext());
            return this;
        }

        @Override
        public ButtonFormElement attachTo(DataContext dataContext)
        {
            this.dataContext = dataContext;
            return this;
        }

        @Override
        public ButtonFormElement saveOnClick()
        {
            return this.onClick((data, dataContext) -> dataContext.persist(data)
                                                                  .get());
        }
    }

    protected static class InputFormElementImpl extends AbstractFormElementImpl<InputFormElement> implements InputFormElement
    {
        private I18nText placeholder;

        protected InputFormElementImpl(Function<Class<?>, String> identitiyProvider, LocalizedTextResolverService textResolver,
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
            node.setType("INPUT")
                .setPlaceholder(this.textResolver.apply(this.placeholder, location));
            return node;
        }

        @Override
        public InputFormElement attachToField(DataContext.Field field)
        {
            this.field = field;
            this.dataContext = field.getDataContext();
            return this;
        }
    }

    protected static abstract class AbstractFormElementImpl<FE extends FormElement<?>> implements FormElement<FE>
    {
        protected final LocalizedTextResolverService textResolver;
        protected final Function<String, I18nText>   i18nTextMapper;
        protected final EventHandlerRegistry         eventHandlerRegistry;

        protected DataContext.Field     field;
        protected DataContext           dataContext;
        protected Supplier<DataContext> parentDataContext;

        private I18nText label;
        private I18nText description;
        private String   id;

        @SuppressWarnings("unchecked")
        protected AbstractFormElementImpl(Function<Class<?>, String> identitiyProvider, LocalizedTextResolverService textResolver,
                                          Function<String, I18nText> i18nTextMapper, EventHandlerRegistry eventHandlerRegistry,
                                          Supplier<? extends DataContext> parentDataContext)
        {
            this.id = identitiyProvider.apply(this.getClass());
            this.textResolver = textResolver;
            this.i18nTextMapper = i18nTextMapper;
            this.eventHandlerRegistry = eventHandlerRegistry;
            this.parentDataContext = (Supplier<DataContext>) parentDataContext;
        }

        @Override
        public FormElementNode render(Location parentLocation)
        {
            DataContext dataContext = this.getEffectiveDataContext();
            Location location = Optional.ofNullable(this.getField())
                                        .map(field -> Location.of(Location.of(parentLocation, this.id), field))
                                        .orElse(Location.of(parentLocation, this.id));
            return this.renderNode(new FormNode.FormElementNodeImpl().setField(this.getField())
                                                                     .setContextId(dataContext.getId(location))
                                                                     .setLabel(this.textResolver.apply(this.label, location))
                                                                     .setDescription(this.textResolver.apply(this.description, location)),
                                   location);
        }

        protected DataContext getEffectiveDataContext()
        {
            return Optional.ofNullable(this.dataContext)
                           .orElse(this.parentDataContext.get());
        }

        protected abstract FormElementNode renderNode(FormElementNodeImpl node, Location location);

        private String getField()
        {
            return Optional.ofNullable(this.field)
                           .map(Field::getFieldName)
                           .orElse(null);
        }

        @SuppressWarnings("unchecked")
        @Override
        public FE withLabel(String label)
        {
            this.label = this.i18nTextMapper.apply(label);
            return (FE) this;
        }

        @SuppressWarnings("unchecked")
        @Override
        public FE withDescription(String description)
        {
            this.description = this.i18nTextMapper.apply(description);
            return (FE) this;
        }

    }

}