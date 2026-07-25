package org.omnaest.react4j.component.form.internal.element;

import java.util.function.Function;
import java.util.function.Supplier;

import org.omnaest.react4j.component.form.Form.ButtonFormElement.ButtonEventHandler;
import org.omnaest.react4j.component.form.Form.InputFormElement;
import org.omnaest.react4j.component.form.internal.renderer.node.element.FormElementNode;
import org.omnaest.react4j.component.form.internal.renderer.node.element.FormElementNode.FormInputNode;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.context.Context;
import org.omnaest.react4j.domain.context.data.DataContext;
import org.omnaest.react4j.domain.context.document.Document;
import org.omnaest.react4j.domain.i18n.I18nText;
import org.omnaest.react4j.service.internal.handler.EventHandlerRegistry;
import org.omnaest.react4j.service.internal.handler.domain.DataEventHandler;
import org.omnaest.react4j.service.internal.handler.domain.DataEventHandler.MappedData;
import org.omnaest.react4j.service.internal.handler.domain.Target;
import org.omnaest.react4j.service.internal.nodes.handler.ServerHandler;
import org.omnaest.react4j.service.internal.service.LocalizedTextResolverService;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;

public class InputFormElementImpl extends AbstractFormElementImpl<InputFormElement> implements InputFormElement
{
    private InputData.InputDataBuilder data = InputData.builder();

    /**
     * {@code null} until {@link #onEnter(ButtonEventHandler)} is called - keeps Enter-to-submit fully opt-in so pre-existing inputs (which never call
     * {@code onEnter}) render {@code submitOnEnter == null} and register no handler, exactly as before this capability was added.
     */
    private DataEventHandler           eventHandler;

    public InputFormElementImpl(Function<Class<?>, String> identitiyProvider, LocalizedTextResolverService textResolver, Function<String, I18nText> i18nTextMapper, EventHandlerRegistry eventHandlerRegistry, Supplier<? extends DataContext> parentDataContext)
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

        FormInputNode.FormInputNodeBuilder inputNodeBuilder = FormInputNode.builder()
                                                                           .type(inputData.getInputType()
                                                                                          .name()
                                                                                          .toLowerCase())
                                                                           .placeholder(this.textResolver.apply(inputData.getPlaceholder(), location));

        if (this.eventHandler != null)
        {
            Context dataContext = this.getEffectiveContext();
            Location enterLocation = location.and("input");
            Target target = Target.from(enterLocation);
            this.eventHandlerRegistry.registerDataEventHandler(target, this.eventHandler);
            inputNodeBuilder.submitOnEnter(new ServerHandler(target).setContextId(dataContext.getId(location)));
        }

        return node.toBuilder()
                   .type("INPUT")
                   .input(inputNodeBuilder.build())
                   .build();
    }

    @Override
    public InputFormElement onEnter(ButtonEventHandler eventHandler)
    {
        this.eventHandler = (previousData, previousInternalData) -> MappedData.builder()
                                                                              .data(eventHandler.apply(previousData, this.getEffectiveContext()))
                                                                              .internalData(previousInternalData)
                                                                              .build();
        return this;
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

        private I18nText  placeholder;
    }

}
