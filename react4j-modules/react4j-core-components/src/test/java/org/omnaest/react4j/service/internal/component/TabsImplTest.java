package org.omnaest.react4j.service.internal.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.Tabs.Tab.State;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.service.internal.nodes.TabsNode;
import org.omnaest.react4j.service.internal.service.LocalizedTextResolverService;

/**
 * @see TabsImpl
 * @author omnaest
 */
public class TabsImplTest
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
        TabsImpl tabs = new TabsImpl(context);

        UIComponentRenderer renderer = tabs.asRenderer();
        RenderingProcessor renderingProcessor = mock(RenderingProcessor.class);
        Location location = mock(Location.class);

        Node node = renderer.render(renderingProcessor, location, Optional.empty());

        assertTrue(((TabsNode) node).getElements()
                                    .isEmpty());
    }

    @Test
    public void testActiveStateUpdatesRenderedNode()
    {
        ComponentContext context = this.newContext();
        TabsImpl tabs = new TabsImpl(context);

        tabs.addTab(tab -> tab.withTitle("first")
                              .withState(State.ACTIVE));

        UIComponentRenderer renderer = tabs.asRenderer();
        RenderingProcessor renderingProcessor = mock(RenderingProcessor.class);
        Location location = mock(Location.class);

        Node node = renderer.render(renderingProcessor, location, Optional.empty());

        assertEquals(1, ((TabsNode) node).getElements()
                                         .size());
        assertTrue(((TabsNode) node).getElements()
                                    .get(0)
                                    .isActive());
        assertFalse(((TabsNode) node).getElements()
                                     .get(0)
                                     .isDisabled());
    }

    @Test
    public void testDisabledStateUpdatesRenderedNode()
    {
        ComponentContext context = this.newContext();
        TabsImpl tabs = new TabsImpl(context);

        tabs.addTab(tab -> tab.withTitle("second")
                              .withState(State.DISABLED));

        UIComponentRenderer renderer = tabs.asRenderer();
        RenderingProcessor renderingProcessor = mock(RenderingProcessor.class);
        Location location = mock(Location.class);

        Node node = renderer.render(renderingProcessor, location, Optional.empty());

        assertTrue(((TabsNode) node).getElements()
                                    .get(0)
                                    .isDisabled());
        assertFalse(((TabsNode) node).getElements()
                                     .get(0)
                                     .isActive());
    }

    @Test
    public void testFieldsSurviveTemplating()
    {
        ComponentContext context = this.newContext();
        TabsImpl tabs = new TabsImpl(context);
        tabs.addTab(tab -> tab.withTitle("first")
                              .withState(State.ACTIVE));
        tabs.addTab(tab -> tab.withTitle("second")
                              .withState(State.DISABLED));

        TabsImpl templated = (TabsImpl) tabs.asTemplateProvider()
                                            .get();

        UIComponentRenderer renderer = templated.asRenderer();
        RenderingProcessor renderingProcessor = mock(RenderingProcessor.class);
        Location location = mock(Location.class);

        Node node = renderer.render(renderingProcessor, location, Optional.empty());

        assertEquals(2, ((TabsNode) node).getElements()
                                         .size());
        assertTrue(((TabsNode) node).getElements()
                                    .get(0)
                                    .isActive());
        assertTrue(((TabsNode) node).getElements()
                                    .get(1)
                                    .isDisabled());
    }
}
