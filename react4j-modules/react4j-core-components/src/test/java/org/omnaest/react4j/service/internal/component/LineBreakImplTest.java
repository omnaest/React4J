package org.omnaest.react4j.service.internal.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer.EventHandlerRegistrationSupport;
import org.omnaest.react4j.domain.rendering.components.LocationSupport;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.service.internal.nodes.LineBreakNode;
import org.omnaest.react4j.service.internal.service.LocalizedTextResolverService;

/**
 * Goal-1 contract-fidelity test: {@link LineBreakImpl} is the simplest possible leaf - it has no builder-settable
 * properties at all, so the contract under test is purely the node type, the leaf shape (no sub components, no event
 * handler), and Location derivation.
 *
 * @see LineBreakImpl
 * @author omnaest
 */
public class LineBreakImplTest
{
    private ComponentContext newContext()
    {
        ComponentContext context = mock(ComponentContext.class);
        when(context.getTextResolver()).thenReturn(mock(LocalizedTextResolverService.class));
        return context;
    }

    @Test
    public void testRenderProducesLineBreakNode()
    {
        ComponentContext context = this.newContext();
        LineBreakImpl lineBreak = new LineBreakImpl(context);

        Node node = lineBreak.asRenderer()
                             .render(mock(RenderingProcessor.class), mock(Location.class), Optional.empty());

        assertNotNull(node);
        assertEquals(LineBreakNode.class, node.getClass());
    }

    @Test
    public void testLineBreakIsALeafWithNoSubComponentsAndNoEventHandler()
    {
        ComponentContext context = this.newContext();
        LineBreakImpl lineBreak = new LineBreakImpl(context);

        UIComponentRenderer renderer = lineBreak.asRenderer();

        assertEquals(0, renderer.getSubComponents(mock(Location.class))
                                .count());

        EventHandlerRegistrationSupport support = mock(EventHandlerRegistrationSupport.class);
        renderer.manageEventHandler(support);
        verify(support, never()).register(any(org.omnaest.react4j.service.internal.handler.domain.EventHandler.class));
        verify(support, never()).registerAsRerenderingNode();
    }

    @Test
    public void testLocationIsDerivedFromComponentId()
    {
        ComponentContext context = this.newContext();
        LineBreakImpl lineBreak = new LineBreakImpl(context);

        LocationSupport locationSupport = mock(LocationSupport.class);
        Location expectedLocation = mock(Location.class);
        when(locationSupport.createLocation(lineBreak.getId())).thenReturn(expectedLocation);

        Location location = lineBreak.asRenderer()
                                     .getLocation(locationSupport);

        assertEquals(expectedLocation, location);
    }

    @Test
    public void testTemplateProviderProducesIndependentEquivalentInstance()
    {
        ComponentContext context = this.newContext();
        LineBreakImpl lineBreak = new LineBreakImpl(context);

        LineBreakImpl templated = (LineBreakImpl) lineBreak.asTemplateProvider()
                                                           .get();

        assertNotNull(templated);
        assertEquals(lineBreak.getId(), templated.getId());
    }
}
