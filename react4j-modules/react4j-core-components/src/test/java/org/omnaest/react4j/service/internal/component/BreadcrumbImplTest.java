package org.omnaest.react4j.service.internal.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.service.internal.nodes.BreadcrumbNode;
import org.omnaest.react4j.service.internal.nodes.BreadcrumbNode.Entry;
import org.omnaest.react4j.service.internal.service.LocalizedTextResolverService;

/**
 * @see BreadcrumbImpl
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
        RenderingProcessor renderingProcessor = mock(RenderingProcessor.class);
        Location location = mock(Location.class);

        Node node = renderer.render(renderingProcessor, location, Optional.empty());

        List<Entry> entries = ((BreadcrumbNode) node).getEntries();
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

        BreadcrumbImpl templated = (BreadcrumbImpl) breadcrumb.asTemplateProvider()
                                                              .get();

        UIComponentRenderer renderer = templated.asRenderer();
        RenderingProcessor renderingProcessor = mock(RenderingProcessor.class);
        Location location = mock(Location.class);

        Node node = renderer.render(renderingProcessor, location, Optional.empty());

        List<Entry> entries = ((BreadcrumbNode) node).getEntries();
        assertEquals(2, entries.size());
        assertEquals("/first", entries.get(0)
                                      .getLink());
        assertFalse(entries.get(0)
                           .isActive());
        assertEquals("second-id", entries.get(1)
                                         .getLinkedId());
        assertTrue(entries.get(1)
                          .isActive());
    }
}
