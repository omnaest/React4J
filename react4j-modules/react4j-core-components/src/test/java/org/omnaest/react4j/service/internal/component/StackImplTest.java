package org.omnaest.react4j.service.internal.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.Stack.Direction;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.service.internal.nodes.StackNode;
import org.omnaest.react4j.service.internal.service.LocalizedTextResolverService;

/**
 * @see StackImpl
 * @author omnaest
 */
public class StackImplTest
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
        StackImpl stack = new StackImpl(context);

        UIComponentRenderer renderer = stack.asRenderer();
        RenderingProcessor renderingProcessor = mock(RenderingProcessor.class);
        Location location = mock(Location.class);

        Node node = renderer.render(renderingProcessor, location, Optional.empty());

        assertEquals("vertical", ((StackNode) node).getDirection());
        assertEquals(0, ((StackNode) node).getGap());
    }

    @Test
    public void testSettersUpdateRenderedNode()
    {
        ComponentContext context = this.newContext();
        StackImpl stack = new StackImpl(context);

        stack.withDirection(Direction.HORIZONTAL);
        stack.withGap(3);

        UIComponentRenderer renderer = stack.asRenderer();
        RenderingProcessor renderingProcessor = mock(RenderingProcessor.class);
        Location location = mock(Location.class);

        Node node = renderer.render(renderingProcessor, location, Optional.empty());

        assertEquals("horizontal", ((StackNode) node).getDirection());
        assertEquals(3, ((StackNode) node).getGap());
    }

    @Test
    public void testFieldsSurviveTemplating()
    {
        ComponentContext context = this.newContext();
        StackImpl stack = new StackImpl(context);
        stack.withDirection(Direction.HORIZONTAL);
        stack.withGap(5);

        StackImpl templated = (StackImpl) stack.asTemplateProvider()
                                               .get();

        UIComponentRenderer renderer = templated.asRenderer();
        RenderingProcessor renderingProcessor = mock(RenderingProcessor.class);
        Location location = mock(Location.class);

        Node node = renderer.render(renderingProcessor, location, Optional.empty());

        assertEquals("horizontal", ((StackNode) node).getDirection());
        assertEquals(5, ((StackNode) node).getGap());
    }

    @Test
    public void testDirectionOfRoundTrip()
    {
        assertEquals(Direction.HORIZONTAL, Direction.of("HORIZONTAL")
                                                    .get());
        assertEquals(Optional.empty(), Direction.of("bogus"));
    }
}
