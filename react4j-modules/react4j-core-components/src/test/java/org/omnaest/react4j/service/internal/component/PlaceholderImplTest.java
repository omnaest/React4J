package org.omnaest.react4j.service.internal.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.Placeholder.Animation;
import org.omnaest.react4j.domain.Placeholder.Size;
import org.omnaest.react4j.domain.Placeholder.Style;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.service.internal.nodes.PlaceholderNode;
import org.omnaest.react4j.service.internal.service.LocalizedTextResolverService;

/**
 * @see PlaceholderImpl
 * @author omnaest
 */
public class PlaceholderImplTest
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
        PlaceholderImpl placeholder = new PlaceholderImpl(context);

        UIComponentRenderer renderer = placeholder.asRenderer();
        RenderingProcessor renderingProcessor = mock(RenderingProcessor.class);
        Location location = mock(Location.class);

        Node node = renderer.render(renderingProcessor, location, Optional.empty());

        assertEquals(12, ((PlaceholderNode) node).getColumns());
        assertNull(((PlaceholderNode) node).getStyle());
        assertNull(((PlaceholderNode) node).getSize());
        assertNull(((PlaceholderNode) node).getAnimation());
    }

    @Test
    public void testSettersUpdateRenderedNode()
    {
        ComponentContext context = this.newContext();
        PlaceholderImpl placeholder = new PlaceholderImpl(context);

        placeholder.withStyle(Style.SECONDARY);
        placeholder.withSize(Size.LG);
        placeholder.withColumns(6);
        placeholder.withAnimation(Animation.GLOW);

        UIComponentRenderer renderer = placeholder.asRenderer();
        RenderingProcessor renderingProcessor = mock(RenderingProcessor.class);
        Location location = mock(Location.class);

        Node node = renderer.render(renderingProcessor, location, Optional.empty());

        assertEquals("secondary", ((PlaceholderNode) node).getStyle());
        assertEquals("lg", ((PlaceholderNode) node).getSize());
        assertEquals(6, ((PlaceholderNode) node).getColumns());
        assertEquals("glow", ((PlaceholderNode) node).getAnimation());
    }

    @Test
    public void testFieldsSurviveTemplating()
    {
        ComponentContext context = this.newContext();
        PlaceholderImpl placeholder = new PlaceholderImpl(context);
        placeholder.withStyle(Style.DARK);
        placeholder.withSize(Size.SM);
        placeholder.withColumns(4);
        placeholder.withAnimation(Animation.WAVE);

        PlaceholderImpl templated = (PlaceholderImpl) placeholder.asTemplateProvider()
                                                                 .get();

        UIComponentRenderer renderer = templated.asRenderer();
        RenderingProcessor renderingProcessor = mock(RenderingProcessor.class);
        Location location = mock(Location.class);

        Node node = renderer.render(renderingProcessor, location, Optional.empty());

        assertEquals("dark", ((PlaceholderNode) node).getStyle());
        assertEquals("sm", ((PlaceholderNode) node).getSize());
        assertEquals(4, ((PlaceholderNode) node).getColumns());
        assertEquals("wave", ((PlaceholderNode) node).getAnimation());
    }

    @Test
    public void testEnumOfRoundTrip()
    {
        assertEquals(Style.LIGHT, Style.of("LIGHT")
                                       .get());
        assertEquals(Size.SM, Size.of("SM")
                                  .get());
        assertEquals(Animation.GLOW, Animation.of("GLOW")
                                              .get());
        assertEquals(Optional.empty(), Size.of("bogus"));
    }
}
