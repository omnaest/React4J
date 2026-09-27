package org.omnaest.react4j.service.internal.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.RenderableUIComponent;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer.EventHandlerRegistrationSupport;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.service.internal.handler.domain.EventHandler;
import org.omnaest.react4j.service.internal.nodes.BreadcrumbEntryNode;
import org.omnaest.react4j.service.internal.nodes.BreadcrumbNode;
import org.omnaest.react4j.service.internal.nodes.handler.ServerHandler;
import org.omnaest.react4j.service.internal.service.LocalizedTextResolverService;

/**
 * @see BreadcrumbImpl
 * @see BreadcrumbEntryImpl
 * @author omnaest
 */
public class BreadcrumbImplTest
{
    private ComponentContext newContext()
    {
        ComponentContext context = mock(ComponentContext.class);
        when(context.getTextResolver()).thenReturn(mock(LocalizedTextResolverService.class));
        return context;
    }

    /**
     * Mirrors {@code PaginationImplTest#selfRenderingProcessor()}: a real self-dispatching
     * {@link RenderingProcessor} so {@link BreadcrumbImpl#asRenderer()} can call
     * {@code renderingProcessor.process(entry, location)} on each {@link BreadcrumbEntryImpl} and receive that
     * entry's own rendered {@link BreadcrumbEntryNode}, rather than a bare mock's default {@code null}.
     */
    private RenderingProcessor selfRenderingProcessor()
    {
        RenderingProcessor renderingProcessor = mock(RenderingProcessor.class);
        when(renderingProcessor.process(any(UIComponent.class), any(Location.class))).thenAnswer(invocation ->
        {
            UIComponent<?> component = invocation.getArgument(0);
            Location location = invocation.getArgument(1);
            RenderableUIComponent<?> renderable = (RenderableUIComponent<?>) component;
            return renderable.asRenderer()
                             .render(this.selfRenderingProcessor(), location, Optional.empty());
        });
        return renderingProcessor;
    }

    @Test
    public void testEntriesDefaultToEmptyList()
    {
        ComponentContext context = this.newContext();
        BreadcrumbImpl breadcrumb = new BreadcrumbImpl(context);

        UIComponentRenderer renderer = breadcrumb.asRenderer();
        RenderingProcessor renderingProcessor = mock(RenderingProcessor.class);
        Location location = mock(Location.class);

        Node node = renderer.render(renderingProcessor, location, Optional.empty());

        assertTrue(((BreadcrumbNode) node).getEntries()
                                          .isEmpty());
    }

    @Test
    public void testAddEntryAppendsEntryToRenderedNode()
    {
        ComponentContext context = this.newContext();
        BreadcrumbImpl breadcrumb = new BreadcrumbImpl(context);

        breadcrumb.addEntry(entry -> entry.withLink("/home")
                                          .withActiveState(true));

        UIComponentRenderer renderer = breadcrumb.asRenderer();
        RenderingProcessor renderingProcessor = this.selfRenderingProcessor();
        Location location = mock(Location.class);

        Node node = renderer.render(renderingProcessor, location, Optional.empty());

        List<BreadcrumbEntryNode> entries = ((BreadcrumbNode) node).getEntries();
        assertEquals(1, entries.size());
        assertEquals("/home", entries.get(0)
                                     .getLink());
        assertTrue(entries.get(0)
                          .isActive());
    }

    @Test
    public void testEntriesSurviveTemplating()
    {
        ComponentContext context = this.newContext();
        BreadcrumbImpl breadcrumb = new BreadcrumbImpl(context);
        breadcrumb.addEntry(entry -> entry.withLink("/first")
                                          .withActiveState(false));
        breadcrumb.addEntry(entry -> entry.withLinkedLocator("second-id")
                                          .withActiveState(true));
        breadcrumb.addEntry(entry -> entry.withText("Third")
                                          .onClick(mock(EventHandler.class)));

        BreadcrumbImpl templated = (BreadcrumbImpl) breadcrumb.asTemplateProvider()
                                                              .get();

        UIComponentRenderer renderer = templated.asRenderer();
        RenderingProcessor renderingProcessor = this.selfRenderingProcessor();
        Location location = mock(Location.class);

        Node node = renderer.render(renderingProcessor, location, Optional.empty());

        List<BreadcrumbEntryNode> entries = ((BreadcrumbNode) node).getEntries();
        assertEquals(3, entries.size());
        assertEquals("/first", entries.get(0)
                                      .getLink());
        assertFalse(entries.get(0)
                           .isActive());
        assertEquals("second-id", entries.get(1)
                                         .getLinkedId());
        assertTrue(entries.get(1)
                          .isActive());
        assertNotNull(entries.get(2)
                             .getOnClick(),
                      "an onClick set before templating must survive onto the templated entry's rendered node");
    }

    @Test
    public void testUnsetLinkLocatorRendersNoLocatorOnTheNode()
    {
        ComponentContext context = this.newContext();
        BreadcrumbImpl breadcrumb = new BreadcrumbImpl(context);

        Node node = breadcrumb.asRenderer()
                              .render(mock(RenderingProcessor.class), mock(Location.class), Optional.empty());

        assertNull(((BreadcrumbNode) node).getLocator());
    }

    @Test
    public void testLinkLocatorSurvivesOntoTheRenderedNode()
    {
        ComponentContext context = this.newContext();
        BreadcrumbImpl breadcrumb = new BreadcrumbImpl(context);
        breadcrumb.withLinkLocator("ancestor-trail");

        Node node = breadcrumb.asRenderer()
                              .render(mock(RenderingProcessor.class), mock(Location.class), Optional.empty());

        assertEquals("ancestor-trail", ((BreadcrumbNode) node).getLocator());
    }

    @Test
    public void testLinkLocatorSurvivesTemplating()
    {
        ComponentContext context = this.newContext();
        BreadcrumbImpl breadcrumb = new BreadcrumbImpl(context);
        breadcrumb.withLinkLocator("ancestor-trail");

        BreadcrumbImpl templated = (BreadcrumbImpl) breadcrumb.asTemplateProvider()
                                                              .get();

        Node node = templated.asRenderer()
                             .render(mock(RenderingProcessor.class), mock(Location.class), Optional.empty());

        assertEquals("ancestor-trail", ((BreadcrumbNode) node).getLocator());
    }

    /**
     * AC-4 (plan-262 S1): mirrors {@code PaginationImplTest#testItemWithOnClickRendersNonNullServerHandler}.
     */
    @Test
    public void testEntryWithOnClickRendersNonNullServerHandler()
    {
        ComponentContext context = this.newContext();
        BreadcrumbEntryImpl entry = new BreadcrumbEntryImpl(context, 0);
        entry.onClick(mock(EventHandler.class));

        Location location = mock(Location.class);
        when(location.get()).thenReturn(Arrays.asList("root", entry.getId()));

        Node node = entry.asRenderer()
                         .render(mock(RenderingProcessor.class), location, Optional.empty());

        assertNotNull(((BreadcrumbEntryNode) node).getOnClick());
        assertTrue(((BreadcrumbEntryNode) node).getOnClick() instanceof ServerHandler);
    }

    @Test
    public void testEntryWithoutOnClickHasNullOnClick()
    {
        ComponentContext context = this.newContext();
        BreadcrumbEntryImpl entry = new BreadcrumbEntryImpl(context, 0);

        Location location = mock(Location.class);
        when(location.get()).thenReturn(Arrays.asList("root", entry.getId()));

        Node node = entry.asRenderer()
                         .render(mock(RenderingProcessor.class), location, Optional.empty());

        assertNull(((BreadcrumbEntryNode) node).getOnClick());
    }

    /**
     * AC-4: {@code manageEventHandler} registers exactly one handler per entry carrying one, and none for an
     * entry without.
     */
    @Test
    public void testManageEventHandlerRegistersOnlyWhenOnClickSet()
    {
        ComponentContext context = this.newContext();

        BreadcrumbEntryImpl entryWithHandler = new BreadcrumbEntryImpl(context, 0);
        EventHandler handler = mock(EventHandler.class);
        entryWithHandler.onClick(handler);

        EventHandlerRegistrationSupport supportWithHandler = mock(EventHandlerRegistrationSupport.class);
        entryWithHandler.asRenderer()
                        .manageEventHandler(supportWithHandler);
        verify(supportWithHandler, times(1)).register(handler);

        BreadcrumbEntryImpl entryWithoutHandler = new BreadcrumbEntryImpl(context, 1);
        EventHandlerRegistrationSupport supportWithoutHandler = mock(EventHandlerRegistrationSupport.class);
        entryWithoutHandler.asRenderer()
                           .manageEventHandler(supportWithoutHandler);
        verify(supportWithoutHandler, never()).register(any(EventHandler.class));
    }

    /**
     * AC-4, the guard against the flat-onClick trap (plan-262 pre-mortem): two entries with distinct handlers
     * must resolve to DISTINCT {@code Target}s. Mirrors
     * {@code PaginationImplTest#testTwoItemsWithDistinctHandlersProduceDistinctTargets}.
     */
    @Test
    public void testTwoEntriesWithDistinctHandlersProduceDistinctTargets()
    {
        ComponentContext context = this.newContext();

        BreadcrumbEntryImpl entry0 = new BreadcrumbEntryImpl(context, 0);
        entry0.onClick(mock(EventHandler.class));
        BreadcrumbEntryImpl entry1 = new BreadcrumbEntryImpl(context, 1);
        entry1.onClick(mock(EventHandler.class));

        assertNotEquals(entry0.getId(), entry1.getId());

        Location location0 = mock(Location.class);
        when(location0.get()).thenReturn(Arrays.asList("root", entry0.getId()));
        Location location1 = mock(Location.class);
        when(location1.get()).thenReturn(Arrays.asList("root", entry1.getId()));

        BreadcrumbEntryNode node0 = (BreadcrumbEntryNode) entry0.asRenderer()
                                                                .render(mock(RenderingProcessor.class), location0, Optional.empty());
        BreadcrumbEntryNode node1 = (BreadcrumbEntryNode) entry1.asRenderer()
                                                                .render(mock(RenderingProcessor.class), location1, Optional.empty());

        ServerHandler handler0 = (ServerHandler) node0.getOnClick();
        ServerHandler handler1 = (ServerHandler) node1.getOnClick();

        assertNotEquals(handler0.getTarget(), handler1.getTarget());
    }

    /**
     * AC-5: entries survive {@code asTemplateProvider().get()} with text/link/linkedId/active/onClick/id
     * intact.
     */
    @Test
    public void testEntryFieldsSurviveTemplating()
    {
        ComponentContext context = this.newContext();
        BreadcrumbEntryImpl entry = new BreadcrumbEntryImpl(context, 2);
        entry.withText("Level Two");
        entry.withLink("/level-two");
        entry.withLinkedLocator("level-two-id");
        entry.withActiveState(true);
        entry.onClick(mock(EventHandler.class));

        BreadcrumbEntryImpl templated = (BreadcrumbEntryImpl) entry.asTemplateProvider()
                                                                   .get();

        Location location = mock(Location.class);
        when(location.get()).thenReturn(Arrays.asList("root", templated.getId()));

        Node node = templated.asRenderer()
                             .render(mock(RenderingProcessor.class), location, Optional.empty());

        assertEquals(entry.getId(), templated.getId());
        assertEquals("/level-two", ((BreadcrumbEntryNode) node).getLink());
        assertEquals("level-two-id", ((BreadcrumbEntryNode) node).getLinkedId());
        assertTrue(((BreadcrumbEntryNode) node).isActive());
        assertNotNull(((BreadcrumbEntryNode) node).getOnClick());
    }
}
