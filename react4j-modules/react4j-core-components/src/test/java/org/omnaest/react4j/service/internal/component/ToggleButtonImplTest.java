package org.omnaest.react4j.service.internal.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.ToggleButton.Style;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer.EventHandlerRegistrationSupport;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.service.internal.handler.domain.EventHandler;
import org.omnaest.react4j.service.internal.nodes.ToggleButtonNode;
import org.omnaest.react4j.service.internal.nodes.handler.ServerHandler;
import org.omnaest.react4j.service.internal.service.LocalizedTextResolverService;

/**
 * @see ToggleButtonImpl
 * @author omnaest
 */
public class ToggleButtonImplTest
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
        ToggleButtonImpl toggleButton = new ToggleButtonImpl(context);

        UIComponentRenderer renderer = toggleButton.asRenderer();
        RenderingProcessor renderingProcessor = mock(RenderingProcessor.class);
        Location location = mock(Location.class);

        Node node = renderer.render(renderingProcessor, location, Optional.empty());

        assertFalse(((ToggleButtonNode) node).isPressed());
        assertNull(((ToggleButtonNode) node).getStyle());
        assertNull(((ToggleButtonNode) node).getOnChange());
    }

    @Test
    public void testSettersUpdateRenderedNode()
    {
        ComponentContext context = this.newContext();
        ToggleButtonImpl toggleButton = new ToggleButtonImpl(context);

        toggleButton.withPressed(true);
        toggleButton.withStyle(Style.SUCCESS);

        UIComponentRenderer renderer = toggleButton.asRenderer();
        RenderingProcessor renderingProcessor = mock(RenderingProcessor.class);
        Location location = mock(Location.class);

        Node node = renderer.render(renderingProcessor, location, Optional.empty());

        assertTrue(((ToggleButtonNode) node).isPressed());
        assertEquals("success", ((ToggleButtonNode) node).getStyle());
    }

    @Test
    public void testFieldsSurviveTemplating()
    {
        ComponentContext context = this.newContext();
        ToggleButtonImpl toggleButton = new ToggleButtonImpl(context);
        toggleButton.withPressed(true);
        toggleButton.withStyle(Style.WARNING);

        ToggleButtonImpl templated = (ToggleButtonImpl) toggleButton.asTemplateProvider()
                                                                    .get();

        UIComponentRenderer renderer = templated.asRenderer();
        RenderingProcessor renderingProcessor = mock(RenderingProcessor.class);
        Location location = mock(Location.class);

        Node node = renderer.render(renderingProcessor, location, Optional.empty());

        assertTrue(((ToggleButtonNode) node).isPressed());
        assertEquals("warning", ((ToggleButtonNode) node).getStyle());
    }

    @Test
    public void testOnChangeHandlerGating()
    {
        ComponentContext context = this.newContext();
        RenderingProcessor renderingProcessor = mock(RenderingProcessor.class);
        Location location = mock(Location.class);

        ToggleButtonImpl withHandler = new ToggleButtonImpl(context);
        withHandler.onChange(mock(EventHandler.class));
        Node nodeWithHandler = withHandler.asRenderer()
                                          .render(renderingProcessor, location, Optional.empty());
        assertNotNull(((ToggleButtonNode) nodeWithHandler).getOnChange());
        assertTrue(((ToggleButtonNode) nodeWithHandler).getOnChange() instanceof ServerHandler);

        ToggleButtonImpl withoutHandler = new ToggleButtonImpl(context);
        Node nodeWithoutHandler = withoutHandler.asRenderer()
                                                .render(renderingProcessor, location, Optional.empty());
        assertNull(((ToggleButtonNode) nodeWithoutHandler).getOnChange());
    }

    @Test
    public void testManageEventHandlerRegistersOnlyWhenOnChangeSet()
    {
        ComponentContext context = this.newContext();

        ToggleButtonImpl withHandler = new ToggleButtonImpl(context);
        EventHandler handler = mock(EventHandler.class);
        withHandler.onChange(handler);
        EventHandlerRegistrationSupport supportWithHandler = mock(EventHandlerRegistrationSupport.class);
        withHandler.asRenderer()
                   .manageEventHandler(supportWithHandler);
        verify(supportWithHandler, times(1)).register(handler);

        ToggleButtonImpl withoutHandler = new ToggleButtonImpl(context);
        EventHandlerRegistrationSupport supportWithoutHandler = mock(EventHandlerRegistrationSupport.class);
        withoutHandler.asRenderer()
                      .manageEventHandler(supportWithoutHandler);
        verify(supportWithoutHandler, never()).register(any(EventHandler.class));
    }

    @Test
    public void testStyleOfRoundTrip()
    {
        assertEquals(Style.SUCCESS, Style.of("SUCCESS")
                                         .get());
        assertEquals(Optional.empty(), Style.of("bogus"));
    }
}
