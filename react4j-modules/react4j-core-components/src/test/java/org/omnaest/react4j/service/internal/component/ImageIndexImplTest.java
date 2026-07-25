package org.omnaest.react4j.service.internal.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.i18n.I18nText;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.service.internal.nodes.ImageIndexNode;
import org.omnaest.react4j.service.internal.nodes.ImageIndexNode.Entry;
import org.omnaest.react4j.service.internal.nodes.i18n.I18nTextValue;
import org.omnaest.react4j.service.internal.service.LocalizedTextResolverService;

/**
 * Goal-1 contract-fidelity test: asserts the {@link ImageIndexImpl} builder API ({@code addEntry}) maps onto the
 * produced {@link ImageIndexNode} entries, in order, with title resolution.
 *
 * @see ImageIndexImpl
 * @author omnaest
 */
public class ImageIndexImplTest
{
    private ComponentContext newContext()
    {
        ComponentContext context = mock(ComponentContext.class);
        when(context.getTextResolver()).thenReturn(mock(LocalizedTextResolverService.class));
        return context;
    }

    @Test
    public void testDefaultEntriesAreEmpty()
    {
        ComponentContext context = this.newContext();
        ImageIndexImpl imageIndex = new ImageIndexImpl(context);

        Node node = imageIndex.asRenderer()
                              .render(mock(RenderingProcessor.class), mock(Location.class), Optional.empty());

        assertTrue(((ImageIndexNode) node).getEntries()
                                          .isEmpty());
    }

    @Test
    public void testAddEntryMapsTitleIdAndImageInOrder()
    {
        ComponentContext context = this.newContext();
        LocalizedTextResolverService textResolver = context.getTextResolver();
        Location location = mock(Location.class);
        I18nTextValue resolvedTitle = new I18nTextValue(Map.of("DEFAULT", "First Entry"));
        when(textResolver.apply(any(I18nText.class), eq(location))).thenReturn(resolvedTitle);

        ImageIndexImpl imageIndex = new ImageIndexImpl(context);
        imageIndex.addEntry("First Entry", "entry-1", "photo1.png");

        Node node = imageIndex.asRenderer()
                              .render(mock(RenderingProcessor.class), location, Optional.empty());

        List<Entry> entries = ((ImageIndexNode) node).getEntries();
        assertEquals(1, entries.size());
        assertEquals("entry-1", entries.get(0)
                                       .getId());
        assertEquals("photo1.png", entries.get(0)
                                          .getImage());
        assertSame(resolvedTitle, entries.get(0)
                                         .getTitle());
    }

    @Test
    public void testEntriesSurviveTemplating()
    {
        ComponentContext context = this.newContext();
        ImageIndexImpl imageIndex = new ImageIndexImpl(context);
        imageIndex.addEntry("Entry A", "a", "a.png");
        imageIndex.addEntry("Entry B", "b", "b.png");

        ImageIndexImpl templated = (ImageIndexImpl) imageIndex.asTemplateProvider()
                                                              .get();

        Node node = templated.asRenderer()
                             .render(mock(RenderingProcessor.class), mock(Location.class), Optional.empty());

        assertEquals(2, ((ImageIndexNode) node).getEntries()
                                               .size());
    }
}
