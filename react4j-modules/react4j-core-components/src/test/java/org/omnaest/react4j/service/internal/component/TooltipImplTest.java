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
import org.omnaest.react4j.domain.Tooltip.Placement;
import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.service.internal.nodes.TooltipNode;
import org.omnaest.react4j.service.internal.service.LocalizedTextResolverService;

/**
 * @see TooltipImpl
 * @author omnaest
 */
public class TooltipImplTest
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
        TooltipImpl tooltip = new TooltipImpl(context);

        UIComponentRenderer renderer = tooltip.asRenderer();
        RenderingProcessor renderingProcessor = mock(RenderingProcessor.class);
        Location location = mock(Location.class);

        Node node = renderer.render(renderingProcessor, location, Optional.empty());

        assertNull(((TooltipNode) node).getPlacement());
    }

    @Test
    public void testSettersUpdateRenderedNode()
    {
        ComponentContext context = this.newContext();
        TooltipImpl tooltip = new TooltipImpl(context);

        tooltip.withPlacement(Placement.BOTTOM);

        UIComponentRenderer renderer = tooltip.asRenderer();
        RenderingProcessor renderingProcessor = mock(RenderingProcessor.class);
        Location location = mock(Location.class);

        Node node = renderer.render(renderingProcessor, location, Optional.empty());

        assertEquals("bottom", ((TooltipNode) node).getPlacement());
    }

    @Test
    public void testContentPresence()
    {
        ComponentContext context = this.newContext();
        TooltipImpl tooltip = new TooltipImpl(context);

        UIComponent<?> content = mock(UIComponent.class);
        tooltip.withContent(content);

        UIComponentRenderer renderer = tooltip.asRenderer();
        RenderingProcessor renderingProcessor = mock(RenderingProcessor.class);
        Location location = mock(Location.class);
        Node contentNode = mock(Node.class);
        when(renderingProcessor.process(eq(content), eq(location))).thenReturn(contentNode);

        Node node = renderer.render(renderingProcessor, location, Optional.empty());

        assertNotNull(((TooltipNode) node).getContent());
    }

    @Test
    public void testFieldsSurviveTemplating()
    {
        ComponentContext context = this.newContext();
        TooltipImpl tooltip = new TooltipImpl(context);
        UIComponent<?> content = mock(UIComponent.class);
        tooltip.withContent(content);
        tooltip.withPlacement(Placement.LEFT);

        TooltipImpl templated = (TooltipImpl) tooltip.asTemplateProvider()
                                                     .get();

        UIComponentRenderer renderer = templated.asRenderer();
        RenderingProcessor renderingProcessor = mock(RenderingProcessor.class);
        Location location = mock(Location.class);
        Node contentNode = mock(Node.class);
        when(renderingProcessor.process(eq(content), eq(location))).thenReturn(contentNode);

        Node node = renderer.render(renderingProcessor, location, Optional.empty());

        assertEquals("left", ((TooltipNode) node).getPlacement());
        assertNotNull(((TooltipNode) node).getContent());
    }

    @Test
    public void testPlacementOfRoundTrip()
    {
        assertEquals(Placement.BOTTOM, Placement.of("BOTTOM")
                                                .get());
        assertEquals(Optional.empty(), Placement.of("bogus"));
    }
}
