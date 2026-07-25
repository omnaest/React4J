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
import org.omnaest.react4j.domain.rendering.components.HandlerEmitter;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.service.internal.handler.EventHandlerRegistry;
import org.omnaest.react4j.service.internal.handler.domain.DataEventHandler;
import org.omnaest.react4j.service.internal.handler.domain.DataEventHandler.MappedData;
import org.omnaest.react4j.service.internal.handler.domain.Target;
import org.omnaest.react4j.service.internal.nodes.handler.Handler;
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
    protected FormElementNode renderNode(RenderingProcessor renderingProcessor, FormElementNode node, Location location)
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
            Handler submitOnEnter = this.emitSubmitOnEnterHandler(renderingProcessor, target);
            if (submitOnEnter instanceof ServerHandler)
            {
                ((ServerHandler) submitOnEnter).setContextId(dataContext.getId(location));
            }
            inputNodeBuilder.submitOnEnter(submitOnEnter);
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

    /**
     * plan-78 Cliff C1-A: obtains the {@code submitOnEnter} node-DTO {@link Handler} through the
     * {@link RenderingProcessor}'s {@link HandlerEmitter} instead of the previous direct pair -
     * {@code this.eventHandlerRegistry.registerDataEventHandler(...)} then {@code new ServerHandler(target)}.
     * Null-tolerant, mirroring {@code ButtonFormElementImpl.emitOnClickHandler(...)}: a {@code null} processor
     * (a raw Mockito mock) falls back to registering directly against the field-held
     * {@link #eventHandlerRegistry}, preserving pre-conversion behavior for callers that supply no real
     * {@link HandlerEmitter}. Only called from within the {@code this.eventHandler != null} gate at the call
     * site, so {@link #eventHandler} stays fully opt-in.
     *
     * @param renderingProcessor
     * @param target
     * @return
     */
    private Handler emitSubmitOnEnterHandler(RenderingProcessor renderingProcessor, Target target)
    {
        HandlerEmitter handlerEmitter = renderingProcessor != null ? renderingProcessor.handlers() : null;
        if (handlerEmitter != null)
        {
            return handlerEmitter.emitDataEventHandler(target, this.eventHandler);
        }
        this.eventHandlerRegistry.registerDataEventHandler(target, this.eventHandler);
        return this.eventHandler != null ? new ServerHandler(target) : null;
    }

}
