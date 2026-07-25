package org.omnaest.react4j.service.internal.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.omnaest.react4j.domain.Badge.Style;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.service.internal.nodes.BadgeNode;
import org.omnaest.react4j.service.internal.service.LocalizedTextResolverService;

/**
 * @see BadgeImpl
 * @author omnaest
 */
public class BadgeImplTest
{
    private ComponentContext newContext()
    {
        ComponentContext context = mock(ComponentContext.class);
        when(context.getTextResolver()).thenReturn(mock(LocalizedTextResolverService.class));
        return context;
    }

    @Test
    public void testStyleDefaultsToPrimary()
    {
        ComponentContext context = this.newContext();
        BadgeImpl badge = new BadgeImpl(context);

        UIComponentRenderer renderer = badge.asRenderer();
        RenderingProcessor renderingProcessor = mock(RenderingProcessor.class);
        Location location = mock(Location.class);

        Node node = renderer.render(renderingProcessor, location, Optional.empty());

        assertEquals("primary", ((BadgeNode) node).getStyle());
    }

    @Test
    public void testStyleSetterUpdatesRenderedNode()
    {
        ComponentContext context = this.newContext();
        BadgeImpl badge = new BadgeImpl(context);

        badge.withStyle(Style.DANGER);

        UIComponentRenderer renderer = badge.asRenderer();
        RenderingProcessor renderingProcessor = mock(RenderingProcessor.class);
        Location location = mock(Location.class);

        Node node = renderer.render(renderingProcessor, location, Optional.empty());

        assertEquals("danger", ((BadgeNode) node).getStyle());
    }

    @Test
    public void testStyleSurvivesTemplating()
    {
        ComponentContext context = this.newContext();
        BadgeImpl badge = new BadgeImpl(context);
        badge.withStyle(Style.WARNING);

        BadgeImpl templated = (BadgeImpl) badge.asTemplateProvider()
                                               .get();

        UIComponentRenderer renderer = templated.asRenderer();
        RenderingProcessor renderingProcessor = mock(RenderingProcessor.class);
        Location location = mock(Location.class);

        Node node = renderer.render(renderingProcessor, location, Optional.empty());

        assertEquals("warning", ((BadgeNode) node).getStyle());
    }

    @Test
    public void testStyleOfRoundTrip()
    {
        assertTrue(Style.of("DANGER")
                        .isPresent());
        assertEquals(Style.DANGER, Style.of("DANGER")
                                        .get());
        assertFalse(Style.of("bogus")
                         .isPresent());
    }
}
