package org.omnaest.react4j.service.internal.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.VerticalContentSwitcher.VerticalContent.State;
import org.omnaest.react4j.domain.i18n.I18nText;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.service.internal.nodes.VerticalContentSwitcherNode;
import org.omnaest.react4j.service.internal.nodes.VerticalContentSwitcherNode.ContentElement;
import org.omnaest.react4j.service.internal.nodes.i18n.I18nTextValue;
import org.omnaest.react4j.service.internal.service.LocalizedTextResolverService;

/**
 * Goal-1 contract-fidelity test: asserts the {@link VerticalContentSwitcherImpl} builder API ({@code addContentEntry}
 * with title/state/content) maps onto the produced {@link VerticalContentSwitcherNode} elements.
 *
 * @see VerticalContentSwitcherImpl
 * @author omnaest
 */
public class VerticalContentSwitcherImplTest
{
    private ComponentContext newContext()
    {
        ComponentContext context = mock(ComponentContext.class);
        when(context.getTextResolver()).thenReturn(mock(LocalizedTextResolverService.class));
        return context;
    }

    @Test
    public void testDefaultElementsAreEmpty()
    {
        ComponentContext context = this.newContext();
        VerticalContentSwitcherImpl switcher = new VerticalContentSwitcherImpl(context);

        Node node = switcher.asRenderer()
                            .render(mock(RenderingProcessor.class), mock(Location.class), Optional.empty());

        assertTrue(((VerticalContentSwitcherNode) node).getElements()
                                                       .isEmpty());
    }

    @Test
    public void testAddContentEntryMapsTitleStateAndContent()
    {
        ComponentContext context = this.newContext();
        LocalizedTextResolverService textResolver = context.getTextResolver();
        Location location = mock(Location.class);
        I18nTextValue resolvedTitle = new I18nTextValue(Map.of("DEFAULT", "Tab 1"));
        when(textResolver.apply(any(I18nText.class), eq(location))).thenReturn(resolvedTitle);

        UIComponent<?> content = mock(UIComponent.class);
        RenderingProcessor renderingProcessor = mock(RenderingProcessor.class);
        Node contentNode = mock(Node.class);
        when(renderingProcessor.process(content, location)).thenReturn(contentNode);

        VerticalContentSwitcherImpl switcher = new VerticalContentSwitcherImpl(context);
        switcher.addContentEntry(entry -> entry.withTitle("Tab 1")
                                               .withState(State.ACTIVE)
                                               .withContent(content));

        Node node = switcher.asRenderer()
                            .render(renderingProcessor, location, Optional.empty());

        List<ContentElement> elements = ((VerticalContentSwitcherNode) node).getElements();
        assertEquals(1, elements.size());
        assertSame(resolvedTitle, elements.get(0)
                                          .getTitle());
        assertTrue(elements.get(0)
                           .isActive());
        assertFalse(elements.get(0)
                            .isDisabled());
        assertSame(contentNode, elements.get(0)
                                        .getContent());
    }

    @Test
    public void testDisabledStateMapsToDisabledFlag()
    {
        ComponentContext context = this.newContext();
        VerticalContentSwitcherImpl switcher = new VerticalContentSwitcherImpl(context);

        switcher.addContentEntry(entry -> entry.withState(State.DISABLED));

        Node node = switcher.asRenderer()
                            .render(mock(RenderingProcessor.class), mock(Location.class), Optional.empty());

        ContentElement element = ((VerticalContentSwitcherNode) node).getElements()
                                                                     .get(0);
        assertTrue(element.isDisabled());
        assertFalse(element.isActive());
    }

    @Test
    public void testEntriesSurviveTemplating()
    {
        ComponentContext context = this.newContext();
        VerticalContentSwitcherImpl switcher = new VerticalContentSwitcherImpl(context);
        switcher.addContentEntry(entry -> entry.withState(State.ACTIVE));

        VerticalContentSwitcherImpl templated = (VerticalContentSwitcherImpl) switcher.asTemplateProvider()
                                                                                      .get();

        Node node = templated.asRenderer()
                             .render(mock(RenderingProcessor.class), mock(Location.class), Optional.empty());

        assertEquals(1, ((VerticalContentSwitcherNode) node).getElements()
                                                            .size());
    }
}
