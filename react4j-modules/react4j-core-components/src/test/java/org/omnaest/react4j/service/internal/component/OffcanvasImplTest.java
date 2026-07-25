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
import org.omnaest.react4j.domain.Offcanvas.Placement;
import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer.EventHandlerRegistrationSupport;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.service.internal.handler.domain.EventHandler;
import org.omnaest.react4j.service.internal.nodes.OffcanvasNode;
import org.omnaest.react4j.service.internal.nodes.handler.ServerHandler;
import org.omnaest.react4j.service.internal.service.LocalizedTextResolverService;

/**
 * @see OffcanvasImpl
 * @author omnaest
 */
public class OffcanvasImplTest
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
        OffcanvasImpl offcanvas = new OffcanvasImpl(context);

        UIComponentRenderer renderer = offcanvas.asRenderer();
        RenderingProcessor renderingProcessor = mock(RenderingProcessor.class);
        Location location = mock(Location.class);

        Node node = renderer.render(renderingProcessor, location, Optional.empty());

        assertFalse(((OffcanvasNode) node).isVisible());
        assertNull(((OffcanvasNode) node).getPlacement());
        assertNull(((OffcanvasNode) node).getOnClose());
    }

    @Test
    public void testSettersUpdateRenderedNode()
    {
        ComponentContext context = this.newContext();
        OffcanvasImpl offcanvas = new OffcanvasImpl(context);

        offcanvas.withPlacement(Placement.END);
        offcanvas.withVisible(true);

        UIComponentRenderer renderer = offcanvas.asRenderer();
        RenderingProcessor renderingProcessor = mock(RenderingProcessor.class);
        Location location = mock(Location.class);

        Node node = renderer.render(renderingProcessor, location, Optional.empty());

        assertEquals("end", ((OffcanvasNode) node).getPlacement());
        assertTrue(((OffcanvasNode) node).isVisible());
    }

    @Test
    public void testContentPresence()
    {
        ComponentContext context = this.newContext();
        OffcanvasImpl offcanvas = new OffcanvasImpl(context);

        UIComponent<?> content = mock(UIComponent.class);
        offcanvas.withContent(content);

        UIComponentRenderer renderer = offcanvas.asRenderer();
        RenderingProcessor renderingProcessor = mock(RenderingProcessor.class);
        Location location = mock(Location.class);
        Node contentNode = mock(Node.class);
        when(renderingProcessor.process(eq(content), eq(location))).thenReturn(contentNode);

        Node node = renderer.render(renderingProcessor, location, Optional.empty());

        assertNotNull(((OffcanvasNode) node).getContent());
    }

    @Test
    public void testFieldsSurviveTemplating()
    {
        ComponentContext context = this.newContext();
        OffcanvasImpl offcanvas = new OffcanvasImpl(context);
        UIComponent<?> content = mock(UIComponent.class);
        offcanvas.withContent(content);
        offcanvas.withPlacement(Placement.TOP);
        offcanvas.withVisible(true);

        OffcanvasImpl templated = (OffcanvasImpl) offcanvas.asTemplateProvider()
                                                           .get();

        UIComponentRenderer renderer = templated.asRenderer();
        RenderingProcessor renderingProcessor = mock(RenderingProcessor.class);
        Location location = mock(Location.class);
        Node contentNode = mock(Node.class);
        when(renderingProcessor.process(eq(content), eq(location))).thenReturn(contentNode);

        Node node = renderer.render(renderingProcessor, location, Optional.empty());

        assertEquals("top", ((OffcanvasNode) node).getPlacement());
        assertTrue(((OffcanvasNode) node).isVisible());
        assertNotNull(((OffcanvasNode) node).getContent());
    }

    @Test
    public void testOnCloseHandlerGating()
    {
        ComponentContext context = this.newContext();
        RenderingProcessor renderingProcessor = mock(RenderingProcessor.class);
        Location location = mock(Location.class);

        OffcanvasImpl withHandler = new OffcanvasImpl(context);
        withHandler.onClose(mock(EventHandler.class));
        Node nodeWithHandler = withHandler.asRenderer()
                                          .render(renderingProcessor, location, Optional.empty());
        assertNotNull(((OffcanvasNode) nodeWithHandler).getOnClose());
        assertTrue(((OffcanvasNode) nodeWithHandler).getOnClose() instanceof ServerHandler);

        OffcanvasImpl withoutHandler = new OffcanvasImpl(context);
        Node nodeWithoutHandler = withoutHandler.asRenderer()
                                                .render(renderingProcessor, location, Optional.empty());
        assertNull(((OffcanvasNode) nodeWithoutHandler).getOnClose());
    }

    @Test
    public void testManageEventHandlerRegistersOnlyWhenOnCloseSet()
    {
        ComponentContext context = this.newContext();

        OffcanvasImpl withHandler = new OffcanvasImpl(context);
        EventHandler handler = mock(EventHandler.class);
        withHandler.onClose(handler);
        EventHandlerRegistrationSupport supportWithHandler = mock(EventHandlerRegistrationSupport.class);
        withHandler.asRenderer()
                   .manageEventHandler(supportWithHandler);
        verify(supportWithHandler, times(1)).register(handler);

        OffcanvasImpl withoutHandler = new OffcanvasImpl(context);
        EventHandlerRegistrationSupport supportWithoutHandler = mock(EventHandlerRegistrationSupport.class);
        withoutHandler.asRenderer()
                      .manageEventHandler(supportWithoutHandler);
        verify(supportWithoutHandler, never()).register(any(EventHandler.class));
    }

    @Test
    public void testPlacementOfRoundTrip()
    {
        assertEquals(Placement.END, Placement.of("END")
                                             .get());
        assertEquals(Optional.empty(), Placement.of("bogus"));
    }
}
