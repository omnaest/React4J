package org.omnaest.react4j.component.form.internal.element;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.omnaest.react4j.component.form.internal.renderer.node.element.FormElementNode;
import org.omnaest.react4j.component.form.internal.renderer.node.element.FormElementNode.FormInputNode;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.context.data.Data;
import org.omnaest.react4j.domain.context.data.DataContext;
import org.omnaest.react4j.domain.context.data.DataContext.PersistResult;
import org.omnaest.react4j.domain.context.data.TypedDataContext;
import org.omnaest.react4j.service.internal.handler.EventHandlerRegistry;
import org.omnaest.react4j.service.internal.handler.domain.DataEventHandler;
import org.omnaest.react4j.service.internal.handler.domain.EventHandler;
import org.omnaest.react4j.service.internal.handler.domain.Target;
import org.omnaest.react4j.service.internal.nodes.handler.ServerHandler;

public class InputFormElementImplTest
{
    private FakeEventHandlerRegistry eventHandlerRegistry = new FakeEventHandlerRegistry();

    private InputFormElementImpl newElement()
    {
        return new InputFormElementImpl(type -> type.getSimpleName()
                                                    .toLowerCase(),
                                        (text, location) -> null, text -> null, this.eventHandlerRegistry, FakeDataContext::new);
    }

    @Test
    public void testRenderWithOnEnterProducesSubmitOnEnterAndRegistersHandler()
    {
        InputFormElementImpl element = this.newElement();
        element.onEnter((data, context) -> data);

        FormElementNode node = element.render(Location.of("form"));

        FormInputNode input = node.getInput();
        assertNotNull(input);
        assertNotNull(input.getSubmitOnEnter());
        assertEquals("SERVER", input.getSubmitOnEnter()
                                    .getType());

        ServerHandler submitOnEnter = (ServerHandler) input.getSubmitOnEnter();
        assertNotNull(submitOnEnter.getTarget());
        assertFalse(submitOnEnter.getTarget()
                                 .isEmpty());
        assertNotNull(submitOnEnter.getContextId());

        assertTrue(this.eventHandlerRegistry.handlers.containsKey(submitOnEnter.getTarget()));
    }

    @Test
    public void testRenderWithoutOnEnterLeavesSubmitOnEnterNull()
    {
        InputFormElementImpl element = this.newElement();

        FormElementNode node = element.render(Location.of("form"));

        assertNotNull(node.getInput());
        assertNull(node.getInput()
                       .getSubmitOnEnter());
        assertTrue(this.eventHandlerRegistry.handlers.isEmpty());
    }

    @Test
    public void testRerenderKeepsSameTargetAndRegistersExactlyOneHandler()
    {
        InputFormElementImpl element = this.newElement();
        element.onEnter((data, context) -> data);

        FormElementNode firstRender = element.render(Location.of("form"));
        FormElementNode secondRender = element.render(Location.of("form"));

        ServerHandler firstSubmitOnEnter = (ServerHandler) firstRender.getInput()
                                                                      .getSubmitOnEnter();
        ServerHandler secondSubmitOnEnter = (ServerHandler) secondRender.getInput()
                                                                        .getSubmitOnEnter();
        assertEquals(firstSubmitOnEnter.getTarget(), secondSubmitOnEnter.getTarget());
        assertEquals(1, this.eventHandlerRegistry.handlers.size());
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

    private static class FakeDataContext implements DataContext
    {
        @Override
        public String getId(Location location)
        {
            return "ctx-" + String.join("/", location.get());
        }

        @Override
        public Optional<DataContext> asDataContext()
        {
            return Optional.of(this);
        }

        @Override
        public PersistResult persist(Data data)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public Selector selector()
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public View view()
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> TypedDataContext<T> asTypedDataContext(Class<T> type)
        {
            throw new UnsupportedOperationException();
        }
    }

}
