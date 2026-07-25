package org.omnaest.react4j.service.internal.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.Modal.Size;
import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer.EventHandlerRegistrationSupport;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.service.internal.handler.domain.EventHandler;
import org.omnaest.react4j.service.internal.nodes.ModalNode;
import org.omnaest.react4j.service.internal.nodes.handler.ServerHandler;
import org.omnaest.react4j.service.internal.service.LocalizedTextResolverService;

/**
 * @see ModalImpl
 * @author omnaest
 */
public class ModalImplTest
{
    private ComponentContext newContext()
    {
        ComponentContext context = mock(ComponentContext.class);
        when(context.getTextResolver()).thenReturn(mock(LocalizedTextResolverService.class));
        return context;
    }

    @Test
    public void testDefaults()
    {
        ComponentContext context = this.newContext();
        ModalImpl modal = new ModalImpl(context);

        UIComponentRenderer renderer = modal.asRenderer();
        RenderingProcessor renderingProcessor = mock(RenderingProcessor.class);
        Location location = mock(Location.class);

        Node node = renderer.render(renderingProcessor, location, Optional.empty());

        assertFalse(((ModalNode) node).isVisible());
        assertNull(((ModalNode) node).getSize());
        assertFalse(((ModalNode) node).isCentered());
        assertNull(((ModalNode) node).getFooter());
        assertNull(((ModalNode) node).getOnClose());
    }

    @Test
    public void testSettersUpdateRenderedNode()
    {
        ComponentContext context = this.newContext();
        ModalImpl modal = new ModalImpl(context);

        modal.withSize(Size.LARGE);
        modal.withCentered(true);
        modal.withVisible(true);

        UIComponentRenderer renderer = modal.asRenderer();
        RenderingProcessor renderingProcessor = mock(RenderingProcessor.class);
        Location location = mock(Location.class);

        Node node = renderer.render(renderingProcessor, location, Optional.empty());

        assertEquals("lg", ((ModalNode) node).getSize());
        assertTrue(((ModalNode) node).isCentered());
        assertTrue(((ModalNode) node).isVisible());
    }

    @Test
    public void testContentAndFooterPresence()
    {
        ComponentContext context = this.newContext();
        ModalImpl modal = new ModalImpl(context);

        UIComponent<?> content = mock(UIComponent.class);
        UIComponent<?> footer = mock(UIComponent.class);
        modal.withContent(content);
        modal.withFooter(footer);

        UIComponentRenderer renderer = modal.asRenderer();
        RenderingProcessor renderingProcessor = mock(RenderingProcessor.class);
        Location location = mock(Location.class);
        Node contentNode = mock(Node.class);
        Node footerNode = mock(Node.class);
        when(renderingProcessor.process(eq(content), eq(location))).thenReturn(contentNode);
        when(renderingProcessor.process(eq(footer), eq(location))).thenReturn(footerNode);

        Node node = renderer.render(renderingProcessor, location, Optional.empty());

        assertNotNull(((ModalNode) node).getContent());
        assertNotNull(((ModalNode) node).getFooter());
    }

    @Test
    public void testFieldsSurviveTemplating()
    {
        ComponentContext context = this.newContext();
        ModalImpl modal = new ModalImpl(context);
        UIComponent<?> content = mock(UIComponent.class);
        UIComponent<?> footer = mock(UIComponent.class);
        modal.withContent(content);
        modal.withFooter(footer);
        modal.withSize(Size.EXTRA_LARGE);
        modal.withCentered(true);
        modal.withVisible(true);

        ModalImpl templated = (ModalImpl) modal.asTemplateProvider()
                                               .get();

        UIComponentRenderer renderer = templated.asRenderer();
        RenderingProcessor renderingProcessor = mock(RenderingProcessor.class);
        Location location = mock(Location.class);
        Node contentNode = mock(Node.class);
        Node footerNode = mock(Node.class);
        when(renderingProcessor.process(eq(content), eq(location))).thenReturn(contentNode);
        when(renderingProcessor.process(eq(footer), eq(location))).thenReturn(footerNode);

        Node node = renderer.render(renderingProcessor, location, Optional.empty());

        assertEquals("xl", ((ModalNode) node).getSize());
        assertTrue(((ModalNode) node).isCentered());
        assertTrue(((ModalNode) node).isVisible());
        assertNotNull(((ModalNode) node).getContent());
        assertNotNull(((ModalNode) node).getFooter());
    }

    @Test
    public void testOnCloseHandlerGating()
    {
        ComponentContext context = this.newContext();
        RenderingProcessor renderingProcessor = mock(RenderingProcessor.class);
        Location location = mock(Location.class);

        ModalImpl withHandler = new ModalImpl(context);
        withHandler.onClose(mock(EventHandler.class));
        Node nodeWithHandler = withHandler.asRenderer()
                                          .render(renderingProcessor, location, Optional.empty());
        assertNotNull(((ModalNode) nodeWithHandler).getOnClose());
        assertTrue(((ModalNode) nodeWithHandler).getOnClose() instanceof ServerHandler);

        ModalImpl withoutHandler = new ModalImpl(context);
        Node nodeWithoutHandler = withoutHandler.asRenderer()
                                                .render(renderingProcessor, location, Optional.empty());
        assertNull(((ModalNode) nodeWithoutHandler).getOnClose());
    }

    @Test
    public void testManageEventHandlerRegistersOnlyWhenOnCloseSet()
    {
        ComponentContext context = this.newContext();

        ModalImpl withHandler = new ModalImpl(context);
        EventHandler handler = mock(EventHandler.class);
        withHandler.onClose(handler);
        EventHandlerRegistrationSupport supportWithHandler = mock(EventHandlerRegistrationSupport.class);
        withHandler.asRenderer()
                   .manageEventHandler(supportWithHandler);
        verify(supportWithHandler, times(1)).register(handler);

        ModalImpl withoutHandler = new ModalImpl(context);
        EventHandlerRegistrationSupport supportWithoutHandler = mock(EventHandlerRegistrationSupport.class);
        withoutHandler.asRenderer()
                      .manageEventHandler(supportWithoutHandler);
        verify(supportWithoutHandler, never()).register(any(EventHandler.class));
    }

    @Test
    public void testSizeOfRoundTrip()
    {
        assertEquals(Size.LARGE, Size.of("LARGE")
                                     .get());
        assertEquals(Optional.empty(), Size.of("bogus"));
    }
}
