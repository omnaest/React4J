package org.omnaest.react4j.service.internal.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.Toaster.Placement;
import org.omnaest.react4j.domain.Toaster.Style;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.service.internal.nodes.ToasterNode;
import org.omnaest.react4j.service.internal.service.LocalizedTextResolverService;

/**
 * @see ToasterImpl
 * @author omnaest
 */
public class ToasterImplTest
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
        ToasterImpl toaster = new ToasterImpl(context);

        UIComponentRenderer renderer = toaster.asRenderer();
        RenderingProcessor renderingProcessor = mock(RenderingProcessor.class);
        Location location = mock(Location.class);

        Node node = renderer.render(renderingProcessor, location, Optional.empty());

        assertNull(((ToasterNode) node).getStyle());
        assertNull(((ToasterNode) node).getPlacement());
    }

    @Test
    public void testSettersUpdateRenderedNode()
    {
        ComponentContext context = this.newContext();
        ToasterImpl toaster = new ToasterImpl(context);

        toaster.withStyle(Style.SUCCESS);
        toaster.withPlacement(Placement.TOP_END);

        UIComponentRenderer renderer = toaster.asRenderer();
        RenderingProcessor renderingProcessor = mock(RenderingProcessor.class);
        Location location = mock(Location.class);

        Node node = renderer.render(renderingProcessor, location, Optional.empty());

        assertEquals("success", ((ToasterNode) node).getStyle());
        assertEquals("top-end", ((ToasterNode) node).getPlacement());
    }

    @Test
    public void testFieldsSurviveTemplating()
    {
        ComponentContext context = this.newContext();
        ToasterImpl toaster = new ToasterImpl(context);
        toaster.withStyle(Style.WARNING);
        toaster.withPlacement(Placement.BOTTOM_CENTER);

        ToasterImpl templated = (ToasterImpl) toaster.asTemplateProvider()
                                                     .get();

        UIComponentRenderer renderer = templated.asRenderer();
        RenderingProcessor renderingProcessor = mock(RenderingProcessor.class);
        Location location = mock(Location.class);

        Node node = renderer.render(renderingProcessor, location, Optional.empty());

        assertEquals("warning", ((ToasterNode) node).getStyle());
        assertEquals("bottom-center", ((ToasterNode) node).getPlacement());
    }

    @Test
    public void testStyleAndPlacementOfRoundTrip()
    {
        assertEquals(Style.SUCCESS, Style.of("SUCCESS")
                                         .get());
        assertEquals(Optional.empty(), Style.of("bogus"));

        assertEquals(Placement.TOP_END, Placement.of("TOP_END")
                                                 .get());
        assertEquals(Optional.empty(), Placement.of("bogus"));
    }
}
