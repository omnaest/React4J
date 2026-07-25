package org.omnaest.react4j.component.form.internal.renderer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.omnaest.react4j.component.form.internal.data.FormData;
import org.omnaest.react4j.component.form.internal.renderer.node.FormNode;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.context.Context;
import org.omnaest.react4j.domain.context.data.Data;
import org.omnaest.react4j.domain.context.data.DataContext;
import org.omnaest.react4j.domain.context.document.Document;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.service.internal.handler.EventHandlerRegistry;
import org.omnaest.react4j.service.internal.handler.domain.DataEventHandler;
import org.omnaest.react4j.service.internal.handler.domain.EventHandler;
import org.omnaest.react4j.service.internal.handler.domain.Target;
import org.omnaest.react4j.service.internal.nodes.handler.ServerHandler;

/**
 * plan-78 Cliff C1-A corrective round: covers the regression found at review - a {@code Form} that never calls
 * {@code Form.onChange(...)} has a {@code null} {@code FormData#eventHandler} (no {@code @Builder.Default}),
 * and the client-side {@code Form.tsx} dispatches the rendered {@code onChange} node field WITHOUT a null
 * guard. Both scenarios below build such a handler-less Form and assert the emitted {@code onChange} field is
 * non-null - the exact condition a null-propagating emission would violate.
 */
public class FormRendererImplTest
{
    private FakeEventHandlerRegistry eventHandlerRegistry = new FakeEventHandlerRegistry();

    private FormRendererImpl newRenderer(FormData formData)
    {
        return new FormRendererImpl(formData, () -> "form", this.eventHandlerRegistry);
    }

    private FormData formDataWithoutOnChangeHandler()
    {
        return FormData.builder()
                       .document(new FakeDocument())
                       .build();
    }

    /**
     * The decisive regression scenario: a real (non-mock) {@link RenderingProcessor} that does not override
     * {@link RenderingProcessor#handlers()} exercises production's no-op {@code HandlerEmitter}, whose
     * {@code emitDataEventHandler(...)} always returns {@code null} - exactly the channel the null-propagating
     * conversion routed the Form's {@code onChange} through. The emitted node field must still be a non-null
     * {@link ServerHandler} carrying the rendered {@link Target}, not the {@code null} that would make
     * {@code Form.tsx}'s unguarded dispatch throw client-side.
     */
    @Test
    public void testRenderWithoutOnChangeHandlerEmitsNonNullServerHandlerThroughEmitter()
    {
        FormRendererImpl renderer = this.newRenderer(this.formDataWithoutOnChangeHandler());
        Location location = Location.of("form");

        RenderingProcessor renderingProcessor = new RenderingProcessor() {
            @Override
            public Node process(UIComponent<?> component, Location parentLocation, Optional<Data> data)
            {
                throw new UnsupportedOperationException("not exercised by this test");
            }
            // handlers() intentionally not overridden -> exercises the production no-op HandlerEmitter that
            // always returns null for emitDataEventHandler, the exact regression path.
        };

        Node node = renderer.render(renderingProcessor, location, Optional.empty());

        FormNode formNode = (FormNode) node;
        assertNotNull(formNode.getOnChange(), "onChange must be emitted unconditionally, even without a registered handler");
        assertTrue(formNode.getOnChange() instanceof ServerHandler);
        ServerHandler onChange = (ServerHandler) formNode.getOnChange();
        assertEquals(Target.from(location), onChange.getTarget());
        assertNotNull(onChange.getContextId());
    }

    /**
     * The no-{@link RenderingProcessor} fallback branch (e.g. a raw Mockito mock, or {@code null}) must produce
     * the same unconditionally non-null {@link ServerHandler}, registering directly against the field-held
     * {@link EventHandlerRegistry}.
     */
    @Test
    public void testRenderWithoutOnChangeHandlerEmitsNonNullServerHandlerThroughRegistryFallback()
    {
        FormRendererImpl renderer = this.newRenderer(this.formDataWithoutOnChangeHandler());
        Location location = Location.of("form");

        Node node = renderer.render(null, location, Optional.empty());

        FormNode formNode = (FormNode) node;
        assertNotNull(formNode.getOnChange(), "onChange must be emitted unconditionally, even without a registered handler");
        assertTrue(formNode.getOnChange() instanceof ServerHandler);
        ServerHandler onChange = (ServerHandler) formNode.getOnChange();
        assertEquals(Target.from(location), onChange.getTarget());
        assertNotNull(onChange.getContextId());
        assertTrue(this.eventHandlerRegistry.handlers.containsKey(onChange.getTarget()));
    }

    private static class FakeEventHandlerRegistry implements EventHandlerRegistry
    {
        private final Map<Target, DataEventHandler> handlers = new HashMap<>();

        @Override
        public void registerEventHandler(Target target, EventHandler eventHandler)
        {
            // not exercised by this test
        }

        @Override
        public void registerDataEventHandler(Target target, DataEventHandler eventHandler)
        {
            this.handlers.put(target, eventHandler);
        }
    }

    private static class FakeDocument implements Document
    {
        @Override
        public Field getField(String fieldName)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public Context getContext()
        {
            return new FakeContext();
        }
    }

    private static class FakeContext implements Context
    {
        @Override
        public String getId(Location location)
        {
            return "ctx-" + String.join("/", location.get());
        }

        @Override
        public Optional<DataContext> asDataContext()
        {
            return Optional.empty();
        }
    }

}
