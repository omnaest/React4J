package org.omnaest.react4j.service.internal.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.Popover.Placement;
import org.omnaest.react4j.domain.Popover.Trigger;
import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.service.internal.nodes.PopoverNode;
import org.omnaest.react4j.service.internal.service.LocalizedTextResolverService;

/**
 * @see PopoverImpl
 * @author omnaest
 */
public class PopoverImplTest
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
        PopoverImpl popover = new PopoverImpl(context);

        UIComponentRenderer renderer = popover.asRenderer();
        RenderingProcessor renderingProcessor = mock(RenderingProcessor.class);
        Location location = mock(Location.class);

        Node node = renderer.render(renderingProcessor, location, Optional.empty());

        assertNull(((PopoverNode) node).getPlacement());
        assertNull(((PopoverNode) node).getTrigger());
        assertNull(((PopoverNode) node).getBody());
    }

    @Test
    public void testSettersUpdateRenderedNode()
    {
        ComponentContext context = this.newContext();
        PopoverImpl popover = new PopoverImpl(context);

        popover.withTrigger(Trigger.HOVER);
        popover.withPlacement(Placement.RIGHT);

        UIComponentRenderer renderer = popover.asRenderer();
        RenderingProcessor renderingProcessor = mock(RenderingProcessor.class);
        Location location = mock(Location.class);

        Node node = renderer.render(renderingProcessor, location, Optional.empty());

        assertEquals("hover", ((PopoverNode) node).getTrigger());
        assertEquals("right", ((PopoverNode) node).getPlacement());
    }

    @Test
    public void testContentAndBodyPresence()
    {
        ComponentContext context = this.newContext();
        PopoverImpl popover = new PopoverImpl(context);

        UIComponent<?> content = mock(UIComponent.class);
        UIComponent<?> body = mock(UIComponent.class);
        popover.withContent(content);
        popover.withBody(body);

        UIComponentRenderer renderer = popover.asRenderer();
        RenderingProcessor renderingProcessor = mock(RenderingProcessor.class);
        Location location = mock(Location.class);
        Node contentNode = mock(Node.class);
        Node bodyNode = mock(Node.class);
        when(renderingProcessor.process(eq(content), eq(location))).thenReturn(contentNode);
        when(renderingProcessor.process(eq(body), eq(location))).thenReturn(bodyNode);

        Node node = renderer.render(renderingProcessor, location, Optional.empty());

        assertNotNull(((PopoverNode) node).getContent());
        assertNotNull(((PopoverNode) node).getBody());
    }

    @Test
    public void testFieldsSurviveTemplating()
    {
        ComponentContext context = this.newContext();
        PopoverImpl popover = new PopoverImpl(context);
        UIComponent<?> content = mock(UIComponent.class);
        UIComponent<?> body = mock(UIComponent.class);
        popover.withContent(content);
        popover.withBody(body);
        popover.withTrigger(Trigger.FOCUS);
        popover.withPlacement(Placement.TOP);

        PopoverImpl templated = (PopoverImpl) popover.asTemplateProvider()
                                                     .get();

        UIComponentRenderer renderer = templated.asRenderer();
        RenderingProcessor renderingProcessor = mock(RenderingProcessor.class);
        Location location = mock(Location.class);
        Node contentNode = mock(Node.class);
        Node bodyNode = mock(Node.class);
        when(renderingProcessor.process(eq(content), eq(location))).thenReturn(contentNode);
        when(renderingProcessor.process(eq(body), eq(location))).thenReturn(bodyNode);

        Node node = renderer.render(renderingProcessor, location, Optional.empty());

        assertEquals("focus", ((PopoverNode) node).getTrigger());
        assertEquals("top", ((PopoverNode) node).getPlacement());
        assertNotNull(((PopoverNode) node).getContent());
        assertNotNull(((PopoverNode) node).getBody());
    }

    @Test
    public void testPlacementAndTriggerOfRoundTrip()
    {
        assertEquals(Placement.RIGHT, Placement.of("RIGHT")
                                               .get());
        assertEquals(Optional.empty(), Placement.of("bogus"));

        assertEquals(Trigger.HOVER, Trigger.of("HOVER")
                                           .get());
        assertEquals(Optional.empty(), Trigger.of("bogus"));
    }
}
