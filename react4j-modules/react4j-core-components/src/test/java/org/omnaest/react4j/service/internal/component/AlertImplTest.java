package org.omnaest.react4j.service.internal.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.omnaest.react4j.domain.Alert.Style;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.service.internal.nodes.AlertNode;
import org.omnaest.react4j.service.internal.service.LocalizedTextResolverService;

/**
 * @see AlertImpl
 * @author omnaest
 */
public class AlertImplTest
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
        AlertImpl alert = new AlertImpl(context);

        UIComponentRenderer renderer = alert.asRenderer();
        RenderingProcessor renderingProcessor = mock(RenderingProcessor.class);
        Location location = mock(Location.class);

        Node node = renderer.render(renderingProcessor, location, Optional.empty());

        assertEquals("primary", ((AlertNode) node).getStyle());
        assertFalse(((AlertNode) node).isDismissible());
    }

    @Test
    public void testSettersUpdateRenderedNode()
    {
        ComponentContext context = this.newContext();
        AlertImpl alert = new AlertImpl(context);

        alert.withStyle(Style.DANGER);
        alert.withDismissible(true);

        UIComponentRenderer renderer = alert.asRenderer();
        RenderingProcessor renderingProcessor = mock(RenderingProcessor.class);
        Location location = mock(Location.class);

        Node node = renderer.render(renderingProcessor, location, Optional.empty());

        assertEquals("danger", ((AlertNode) node).getStyle());
        assertTrue(((AlertNode) node).isDismissible());
    }

    @Test
    public void testFieldsSurviveTemplating()
    {
        ComponentContext context = this.newContext();
        AlertImpl alert = new AlertImpl(context);
        alert.withStyle(Style.WARNING);
        alert.withDismissible(true);

        AlertImpl templated = (AlertImpl) alert.asTemplateProvider()
                                               .get();

        UIComponentRenderer renderer = templated.asRenderer();
        RenderingProcessor renderingProcessor = mock(RenderingProcessor.class);
        Location location = mock(Location.class);

        Node node = renderer.render(renderingProcessor, location, Optional.empty());

        assertEquals("warning", ((AlertNode) node).getStyle());
        assertTrue(((AlertNode) node).isDismissible());
    }

    @Test
    public void testStyleOfRoundTrip()
    {
        assertEquals(Style.INFO, Style.of("INFO")
                                      .get());
        assertEquals(Optional.empty(), Style.of("bogus"));
    }
}
