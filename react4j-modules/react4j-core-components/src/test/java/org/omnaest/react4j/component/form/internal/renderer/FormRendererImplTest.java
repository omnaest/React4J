package org.omnaest.react4j.component.form.internal.renderer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
 * Whether a {@code Form} emits an {@code onChange} target, and therefore whether typing in it costs a round
 * trip.
 *
 * <h2>What these tests used to assert, and why they were right to</h2>
 * That a handler-less form emits an {@code onChange} anyway. {@code FormData#eventHandler} has no
 * {@code @Builder.Default}, so it is null for the common form whose only interactivity is its submit button -
 * and {@code Form.tsx} dispatched the rendered field without a null guard, throwing client-side on every
 * keystroke if it was absent. Emitting unconditionally was the only thing that worked.
 *
 * <h2>Why they now assert the opposite</h2>
 * The client guard exists. With it, emitting a target nobody registered a handler for buys nothing and costs a
 * request per keystroke - measured on a chat box at three in flight at once while typing five characters. The
 * value itself never depended on that traffic: {@code Form.tsx} writes it into the form's own data context
 * locally, and that context travels with the next real event.
 *
 * <h2>The pairing is the fragile part</h2>
 * Server emission and client guard have to move together. Drop the guard and a handler-less form throws on
 * every keystroke; emit unconditionally again and every form silently pays for a capability it never asked for
 * (memory {@code react4j-gate-optin-handler-on-existing-node}). The tests below pin both directions: absent
 * when no handler is registered, present and routable when one is.
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
     * A form nobody registered a change handler on emits no {@code onChange} - so typing in it reaches no
     * server.
     * <p>
     * Rendered through a real (non-mock) {@link RenderingProcessor} that does not override
     * {@link RenderingProcessor#handlers()}, which exercises production's no-op {@code HandlerEmitter}.
     */
    @Test
    public void testRenderWithoutOnChangeHandlerEmitsNoOnChangeThroughEmitter()
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
        assertNull(formNode.getOnChange(),
                   "a form with no registered change handler must emit no onChange target - emitting one makes every "
                           + "keystroke a round trip that notifies nobody");
    }

    /**
     * Same on the no-{@link RenderingProcessor} fallback branch (a raw Mockito mock, or {@code null}): nothing
     * emitted, and nothing registered against the {@link EventHandlerRegistry} either. Registering a handler for
     * a target the node never carries would be a leak with no way to fire it.
     */
    @Test
    public void testRenderWithoutOnChangeHandlerRegistersNothingOnTheFallbackBranch()
    {
        FormRendererImpl renderer = this.newRenderer(this.formDataWithoutOnChangeHandler());
        Location location = Location.of("form");

        Node node = renderer.render(null, location, Optional.empty());

        FormNode formNode = (FormNode) node;
        assertNull(formNode.getOnChange(), "a form with no registered change handler must emit no onChange target");
        assertTrue(this.eventHandlerRegistry.handlers.isEmpty(), "and must register no handler for a target it never emits");
    }

    /**
     * THE other direction, and the one that stops this becoming "onChange never works": a form that DID call
     * {@code Form.onChange(...)} must still emit a routable {@link ServerHandler}.
     * <p>
     * Without this, gating emission on a null check would pass every test above while silently disabling the
     * feature for every application that actually uses it.
     */
    @Test
    public void testRenderWithAnOnChangeHandlerStillEmitsARoutableServerHandler()
    {
        FormData formData = FormData.builder()
                                    .document(new FakeDocument())
                                    .eventHandler((data, internalData) -> DataEventHandler.MappedData.builder()
                                                                                                     .data(data)
                                                                                                     .internalData(internalData)
                                                                                                     .build())
                                    .build();
        FormRendererImpl renderer = this.newRenderer(formData);
        Location location = Location.of("form");

        Node node = renderer.render(null, location, Optional.empty());

        FormNode formNode = (FormNode) node;
        assertNotNull(formNode.getOnChange(), "a form WITH a change handler must still emit one");
        assertTrue(formNode.getOnChange() instanceof ServerHandler);
        ServerHandler onChange = (ServerHandler) formNode.getOnChange();
        assertEquals(Target.from(location), onChange.getTarget());
        assertNotNull(onChange.getContextId());
        assertTrue(this.eventHandlerRegistry.handlers.containsKey(onChange.getTarget()), "and must register the handler so the target routes");
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
