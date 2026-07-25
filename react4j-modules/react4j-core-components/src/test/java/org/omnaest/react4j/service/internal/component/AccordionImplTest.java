package org.omnaest.react4j.service.internal.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.service.internal.nodes.AccordionNode;
import org.omnaest.react4j.service.internal.service.LocalizedTextResolverService;

/**
 * @see AccordionImpl
 * @author omnaest
 */
public class AccordionImplTest
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
        AccordionImpl accordion = new AccordionImpl(context);

        UIComponentRenderer renderer = accordion.asRenderer();
        RenderingProcessor renderingProcessor = mock(RenderingProcessor.class);
        Location location = mock(Location.class);

        Node node = renderer.render(renderingProcessor, location, Optional.empty());

        assertFalse(((AccordionNode) node).isAlwaysOpen());
        assertTrue(((AccordionNode) node).getPanels()
                                         .isEmpty());
    }

    @Test
    public void testSettersUpdateRenderedNode()
    {
        ComponentContext context = this.newContext();
        AccordionImpl accordion = new AccordionImpl(context);

        accordion.withAlwaysOpen(true);
        accordion.addPanel(panel -> panel.withTitle("first")
                                         .withExpandedState(true));

        UIComponentRenderer renderer = accordion.asRenderer();
        RenderingProcessor renderingProcessor = mock(RenderingProcessor.class);
        Location location = mock(Location.class);

        Node node = renderer.render(renderingProcessor, location, Optional.empty());

        assertTrue(((AccordionNode) node).isAlwaysOpen());
        assertEquals(1, ((AccordionNode) node).getPanels()
                                              .size());
        assertTrue(((AccordionNode) node).getPanels()
                                         .get(0)
                                         .isExpanded());
    }

    @Test
    public void testFieldsSurviveTemplating()
    {
        ComponentContext context = this.newContext();
        AccordionImpl accordion = new AccordionImpl(context);
        accordion.withAlwaysOpen(true);
        accordion.addPanel(panel -> panel.withTitle("first")
                                         .withExpandedState(true));

        AccordionImpl templated = (AccordionImpl) accordion.asTemplateProvider()
                                                           .get();

        UIComponentRenderer renderer = templated.asRenderer();
        RenderingProcessor renderingProcessor = mock(RenderingProcessor.class);
        Location location = mock(Location.class);

        Node node = renderer.render(renderingProcessor, location, Optional.empty());

        assertTrue(((AccordionNode) node).isAlwaysOpen());
        assertEquals(1, ((AccordionNode) node).getPanels()
                                              .size());
        assertTrue(((AccordionNode) node).getPanels()
                                         .get(0)
                                         .isExpanded());
    }
}
