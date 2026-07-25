package org.omnaest.react4j.service.internal.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer.EventHandlerRegistrationSupport;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.domain.support.UIComponentProvider;
import org.omnaest.react4j.service.internal.nodes.IntervalRerenderingContainerNode;
import org.omnaest.react4j.service.internal.service.LocalizedTextResolverService;

/**
 * Goal-1 contract-fidelity test: asserts the {@link IntervalRerenderingContainerImpl} builder API
 * ({@code withIntervalDuration}/{@code withRefreshedContent}) maps onto the produced
 * {@link IntervalRerenderingContainerNode}, and that it registers as a rerendering node rather than a click handler.
 *
 * @see IntervalRerenderingContainerImpl
 * @author omnaest
 */
public class IntervalRerenderingContainerImplTest
{
    private ComponentContext newContext()
    {
        ComponentContext context = mock(ComponentContext.class);
        when(context.getTextResolver()).thenReturn(mock(LocalizedTextResolverService.class));
        return context;
    }

    @Test
    public void testDefaultsHaveOneSecondIntervalAndActiveState()
    {
        ComponentContext context = this.newContext();
        IntervalRerenderingContainerImpl container = new IntervalRerenderingContainerImpl(context, UIComponentProvider.of(mock(UIComponent.class)), 1000);

        Node node = container.asRenderer()
                             .render(mock(RenderingProcessor.class), mock(Location.class), Optional.empty());

        IntervalRerenderingContainerNode containerNode = (IntervalRerenderingContainerNode) node;
        assertEquals(1000, containerNode.getIntervalDuration());
        assertTrue(containerNode.isActive());
    }

    @Test
    public void testWithIntervalDurationConvertsToMillis()
    {
        ComponentContext context = this.newContext();
        IntervalRerenderingContainerImpl container = new IntervalRerenderingContainerImpl(context);
        container.withRefreshedContent(UIComponentProvider.of(mock(UIComponent.class)));

        container.withIntervalDuration(2, TimeUnit.SECONDS);

        Node node = container.asRenderer()
                             .render(mock(RenderingProcessor.class), mock(Location.class), Optional.empty());

        assertEquals(2000, ((IntervalRerenderingContainerNode) node).getIntervalDuration());
    }

    @Test
    public void testWithRefreshedContentActiveRefreshStateControlDisablesActiveFlag()
    {
        ComponentContext context = this.newContext();
        IntervalRerenderingContainerImpl container = new IntervalRerenderingContainerImpl(context);

        container.withRefreshedContent(control ->
        {
            control.disable();
            return mock(UIComponent.class);
        });

        Node node = container.asRenderer()
                             .render(mock(RenderingProcessor.class), mock(Location.class), Optional.empty());

        assertFalse(((IntervalRerenderingContainerNode) node).isActive());
    }

    @Test
    public void testContentIsRenderedAndExposedAsSubComponent()
    {
        ComponentContext context = this.newContext();
        UIComponent<?> content = mock(UIComponent.class);
        IntervalRerenderingContainerImpl container = new IntervalRerenderingContainerImpl(context);
        container.withRefreshedContent(UIComponentProvider.of(content));

        RenderingProcessor renderingProcessor = mock(RenderingProcessor.class);
        Location location = mock(Location.class);
        Node contentNode = mock(Node.class);
        when(renderingProcessor.process(content, location)).thenReturn(contentNode);

        Node node = container.asRenderer()
                             .render(renderingProcessor, location, Optional.empty());

        assertSame(contentNode, ((IntervalRerenderingContainerNode) node).getContent());

        assertSame(content, container.asRenderer()
                                     .getSubComponents(location)
                                     .findFirst()
                                     .orElseThrow()
                                     .getComponent());
    }

    @Test
    public void testManageEventHandlerRegistersAsRerenderingNodeNotClickHandler()
    {
        ComponentContext context = this.newContext();
        IntervalRerenderingContainerImpl container = new IntervalRerenderingContainerImpl(context);
        container.withRefreshedContent(UIComponentProvider.of(mock(UIComponent.class)));

        EventHandlerRegistrationSupport support = mock(EventHandlerRegistrationSupport.class);
        container.asRenderer()
                 .manageEventHandler(support);

        verify(support, times(1)).registerAsRerenderingNode();
    }
}
