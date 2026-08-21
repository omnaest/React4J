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
     * <b>Returns {@code null} when the application never called {@code Form.onChange(...)}</b>, so no handler is
     * registered and the rendered node carries no {@code onChange} target.
     * <p>
     * <b>Why this is now safe, and was not before.</b> {@code FormData#eventHandler} has no
     * {@code @Builder.Default}, so it is null for the common form whose only interactivity is its submit button.
     * This method used to emit a {@link ServerHandler} anyway, because {@code Form.tsx} dispatched the rendered
     * {@code onChange} field without a null guard and would throw client-side on every keystroke. That guard now
     * exists, which is what this change is paired with - see {@code Form.tsx#handleInputChange}.
     * <p>
     * <b>What it costs to emit one anyway.</b> A round trip per keystroke. Measured on a chat box: typing five
     * characters put three requests in flight at once, and the count stayed above zero through any real burst of
     * typing. For a form with no change handler that traffic notifies nobody - the client already writes the
     * value into its own data context in the same method, and that context travels with the next real event. The
     * pitfall recorded in memory {@code react4j-gate-optin-handler-on-existing-node} is exactly this: registering
     * and emitting unconditionally makes every existing form pay for a capability it never asked for.
     * <p>
     * Both branches still return a {@link ServerHandler} when a handler IS registered - the emitter-present one
     * falling back to {@code new ServerHandler(target)} when
     * {@link HandlerEmitter#emitDataEventHandler(Target, DataEventHandler)} yields null (a raw Mockito mock, or
     * production's no-op emitter), and the no-emitter branch registering directly against the field-held
     * {@link #eventHandlerRegistry}.
     *
     * @param renderingProcessor
     * @param target
     * @return
     */
    private Handler emitOnChangeHandler(RenderingProcessor renderingProcessor, Target target)
    {
        if (this.formData.getEventHandler() == null)
        {
            return null;
        }

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
