package org.omnaest.react4j.service.internal.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.i18n.I18nText;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.service.internal.nodes.NavigationBarNode;
import org.omnaest.react4j.service.internal.nodes.NavigationBarNode.Entry;
import org.omnaest.react4j.service.internal.nodes.i18n.I18nTextValue;
import org.omnaest.react4j.service.internal.service.LocalizedTextResolverService;

/**
 * Goal-1 contract-fidelity test: asserts the {@link NavigationBarImpl} builder API ({@code addEntry} with
 * text/link/linkedLocator/activeState/disabledState) maps onto the produced {@link NavigationBarNode} entries.
 *
 * @see NavigationBarImpl
 * @author omnaest
 */
public class NavigationBarImplTest
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
        NavigationBarImpl navigationBar = new NavigationBarImpl(context);

        Node node = navigationBar.asRenderer()
                                 .render(mock(RenderingProcessor.class), mock(Location.class), Optional.empty());

        assertTrue(((NavigationBarNode) node).getEntries()
                                             .isEmpty());
    }

    @Test
    public void testAddEntryWithTextLinkActiveAndDisabledMapsToNode()
    {
        ComponentContext context = this.newContext();
        LocalizedTextResolverService textResolver = context.getTextResolver();
        Location location = mock(Location.class);
        I18nTextValue resolvedText = new I18nTextValue(Map.of("DEFAULT", "Home"));
        when(textResolver.apply(any(I18nText.class), eq(location))).thenReturn(resolvedText);

        NavigationBarImpl navigationBar = new NavigationBarImpl(context);
        navigationBar.addEntry(entry -> entry.withText("Home")
                                             .withLink("/home")
                                             .withActiveState(true)
                                             .withDisabledState(false));

        Node node = navigationBar.asRenderer()
                                 .render(mock(RenderingProcessor.class), location, Optional.empty());

        List<Entry> entries = ((NavigationBarNode) node).getEntries();
        assertEquals(1, entries.size());
        assertEquals("/home", entries.get(0)
                                     .getLink());
        assertEquals(resolvedText, entries.get(0)
                                          .getText());
        assertTrue(entries.get(0)
                          .isActive());
        assertFalse(entries.get(0)
                           .isDisabled());
    }

    @Test
    public void testAddEntryWithLinkedLocatorUsesComponentId()
    {
        ComponentContext context = this.newContext();
        NavigationBarImpl navigationBar = new NavigationBarImpl(context);

        UIComponent<?> linkedComponent = mock(UIComponent.class);
        when(linkedComponent.getId()).thenReturn("some-section");

        navigationBar.addEntry(entry -> entry.withLinked(linkedComponent));

        Node node = navigationBar.asRenderer()
                                 .render(mock(RenderingProcessor.class), mock(Location.class), Optional.empty());

        assertEquals("some-section", ((NavigationBarNode) node).getEntries()
                                                               .get(0)
                                                               .getLinkedId());
    }

    @Test
    public void testMultipleEntriesPreserveOrder()
    {
        ComponentContext context = this.newContext();
        NavigationBarImpl navigationBar = new NavigationBarImpl(context);

        navigationBar.addEntry(entry -> entry.withLink("/first"));
        navigationBar.addEntry(entry -> entry.withLink("/second"));

        Node node = navigationBar.asRenderer()
                                 .render(mock(RenderingProcessor.class), mock(Location.class), Optional.empty());

        List<Entry> entries = ((NavigationBarNode) node).getEntries();
        assertEquals(2, entries.size());
        assertEquals("/first", entries.get(0)
                                      .getLink());
        assertEquals("/second", entries.get(1)
                                       .getLink());
    }

    @Test
    public void testEntriesSurviveTemplating()
    {
        ComponentContext context = this.newContext();
        NavigationBarImpl navigationBar = new NavigationBarImpl(context);
        navigationBar.addEntry(entry -> entry.withLink("/home"));

        NavigationBarImpl templated = (NavigationBarImpl) navigationBar.asTemplateProvider()
                                                                       .get();

        Node node = templated.asRenderer()
                             .render(mock(RenderingProcessor.class), mock(Location.class), Optional.empty());

        assertEquals(1, ((NavigationBarNode) node).getEntries()
                                                  .size());
    }
}
