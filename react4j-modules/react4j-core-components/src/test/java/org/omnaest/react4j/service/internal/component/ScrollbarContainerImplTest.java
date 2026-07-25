package org.omnaest.react4j.service.internal.component;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.service.internal.nodes.ScrollbarContainerNode;

/**
 * @see ScrollbarContainerImpl
 * @author omnaest
 */
public class ScrollbarContainerImplTest
{
    @Test
    public void testScrollToBottomOnUpdateDefaultsToFalse()
    {
        ComponentContext context = mock(ComponentContext.class);
        ScrollbarContainerImpl container = new ScrollbarContainerImpl(context);

        UIComponentRenderer renderer = container.asRenderer();
        RenderingProcessor renderingProcessor = mock(RenderingProcessor.class);
        Location location = mock(Location.class);

        Node node = renderer.render(renderingProcessor, location, Optional.empty());

        assertFalse(((ScrollbarContainerNode) node).isScrollToBottomOnUpdate());
    }

    @Test
    public void testScrollToBottomOnUpdateEnablesFlagOnRenderedNode()
    {
        ComponentContext context = mock(ComponentContext.class);
        ScrollbarContainerImpl container = new ScrollbarContainerImpl(context);

        container.scrollToBottomOnUpdate();

        UIComponentRenderer renderer = container.asRenderer();
        RenderingProcessor renderingProcessor = mock(RenderingProcessor.class);
        Location location = mock(Location.class);

        Node node = renderer.render(renderingProcessor, location, Optional.empty());

        assertTrue(((ScrollbarContainerNode) node).isScrollToBottomOnUpdate());
    }

    @Test
    public void testScrollToBottomOnUpdateSurvivesTemplating()
    {
        ComponentContext context = mock(ComponentContext.class);
        ScrollbarContainerImpl container = new ScrollbarContainerImpl(context);
        container.scrollToBottomOnUpdate();

        ScrollbarContainerImpl templated = (ScrollbarContainerImpl) container.asTemplateProvider()
                                                                             .get();

        UIComponentRenderer renderer = templated.asRenderer();
        RenderingProcessor renderingProcessor = mock(RenderingProcessor.class);
        Location location = mock(Location.class);

        Node node = renderer.render(renderingProcessor, location, Optional.empty());

        assertTrue(((ScrollbarContainerNode) node).isScrollToBottomOnUpdate());
    }
}
