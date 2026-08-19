package org.omnaest.react4j.component.form.internal.renderer;

import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.omnaest.react4j.component.form.internal.data.FormData;
import org.omnaest.react4j.component.form.internal.renderer.node.FormNode;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.context.Context;
import org.omnaest.react4j.domain.context.data.Data;
import org.omnaest.react4j.domain.context.document.Document;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer;
import org.omnaest.react4j.domain.rendering.components.HandlerEmitter;
import org.omnaest.react4j.domain.rendering.components.LocationSupport;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.domain.rendering.node.NodeRendererRegistry;
import org.omnaest.react4j.service.internal.handler.EventHandlerRegistry;
import org.omnaest.react4j.service.internal.handler.domain.Target;
import org.omnaest.react4j.service.internal.nodes.handler.Handler;
import org.omnaest.react4j.service.internal.nodes.handler.ServerHandler;
import org.omnaest.utils.functional.Provider;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class FormRendererImpl implements UIComponentRenderer
{
    private final FormData             formData;
    private final Provider<String>     idProvider;
    private final EventHandlerRegistry eventHandlerRegistry;

    @Override
    public Location getLocation(LocationSupport locationSupport)
    {
        return locationSupport.createLocation(this.idProvider.get());
    }

    @Override
    public Node render(RenderingProcessor renderingProcessor, Location location, Optional<Data> data)
    {
        Context dataContext = this.getEffectiveContext();
        Target target = Target.from(location);
        Handler onChange = this.emitOnChangeHandler(renderingProcessor, target);
        if (onChange instanceof ServerHandler)
        {
            ((ServerHandler) onChange).setContextId(dataContext.getId(location));
        }
        return new FormNode().setResponsive(this.formData.isResponsive())
                             .setInlineControls(this.formData.isInlineControls())
                             .setElements(this.formData.getElements()
                                                       .stream()
                                                       .map(element -> element.render(renderingProcessor, location))
                                                       .collect(Collectors.toList()))
                             .setOnChange(onChange);
    }

    /**
     * plan-78 Cliff C1-A, corrective round: obtains the {@code onChange} node-DTO {@link Handler} through the
     * {@link RenderingProcessor}'s {@link HandlerEmitter} instead of the previous direct pair -
     * {@code this.eventHandlerRegistry.registerDataEventHandler(...)} then {@code new ServerHandler(target)}.
     * Null-tolerant, mirroring {@code ButtonFormElementImpl.emitOnClickHandler(...)}: a {@code null} processor
     * (a raw Mockito mock) falls back to registering directly against the field-held
     * {@link #eventHandlerRegistry}, preserving pre-conversion behavior for callers that supply no real
     * {@link HandlerEmitter}.
     * <p>
     * <b>The returned {@link Handler} is unconditionally non-null</b> - deliberately deviating from the
     * null-propagating idiom used at the other Group B sites ({@code FileUploadFormElementImpl},
     * {@code InputFormElementImpl}). {@code FormData#eventHandler} has no {@code @Builder.Default}, so it is
     * {@code null} whenever an application builds a {@code Form} without ever calling
     * {@code Form.onChange(...)} - the common case of a form whose only interactivity is its submit button.
     * {@code Form.tsx} on the client dispatches the rendered {@code onChange} node field through
     * {@code HandlerFactory.handleEvent(...)} WITHOUT a null guard, so a {@code null} here throws client-side
     * on every input change in such a form. Both the emitter-present branch (falling back to
     * {@code new ServerHandler(target)} when {@link HandlerEmitter#emitDataEventHandler(Target, DataEventHandler)}
     * yields {@code null}) and the no-emitter fallback branch therefore always return a {@link ServerHandler},
     * matching this class's pre-conversion shape where there was no {@code eventHandler != null} gate. Do
     * NOT "clean this up" back into the shared null-propagating shape - that reintroduces the client-side
     * throw.
     *
     * @param renderingProcessor
     * @param target
     * @return
     */
    private Handler emitOnChangeHandler(RenderingProcessor renderingProcessor, Target target)
    {
        HandlerEmitter handlerEmitter = renderingProcessor != null ? renderingProcessor.handlers() : null;
        if (handlerEmitter != null)
        {
            Handler emitted = handlerEmitter.emitDataEventHandler(target, this.formData.getEventHandler());
            return emitted != null ? emitted : new ServerHandler(target);
        }
        this.eventHandlerRegistry.registerDataEventHandler(target, this.formData.getEventHandler());
        return new ServerHandler(target);
    }

    protected Context getEffectiveContext()
    {
        return Optional.ofNullable(this.formData.getDocument())
                       .map(Document::getContext)
                       .orElse(null);
    }

    @Override
    public void manageNodeRenderers(NodeRendererRegistry registry)
    {
    }

    @Override
    public void manageEventHandler(EventHandlerRegistrationSupport eventHandlerRegistrationSupport)
    {
    }

    @Override
    public Stream<ParentLocationAndComponent> getSubComponents(Location parentLocation)
    {
        return Stream.empty();
    }
}
