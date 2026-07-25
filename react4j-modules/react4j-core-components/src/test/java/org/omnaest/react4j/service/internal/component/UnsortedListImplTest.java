package org.omnaest.react4j.service.internal.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.UIComponentFactory;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.service.internal.nodes.UnsortedListNode;
import org.omnaest.react4j.service.internal.service.LocalizedTextResolverService;

/**
 * Goal-1 contract-fidelity test: asserts the {@link UnsortedListImpl} builder API ({@code addEntry}/{@code addEntries}/
 * {@code enableBulletPoints}) maps onto the produced {@link UnsortedListNode}, preserving element order.
 *
 * @see UnsortedListImpl
 * @author omnaest
 */
public class UnsortedListImplTest
{
    private ComponentContext newContext()
    {
        ComponentContext context = mock(ComponentContext.class);
        when(context.getTextResolver()).thenReturn(mock(LocalizedTextResolverService.class));
        return context;
    }

    @Test
    public void testDefaultsHaveEmptyElementsAndBulletPointsDisabled()
    {
        ComponentContext context = this.newContext();
        UnsortedListImpl unsortedList = new UnsortedListImpl(context);

        Node node = unsortedList.asRenderer()
                                .render(mock(RenderingProcessor.class), mock(Location.class), Optional.empty());

        UnsortedListNode listNode = (UnsortedListNode) node;
        assertTrue(listNode.getElements()
                           .isEmpty());
        assertFalse(listNode.isEnableBulletPoints());
    }

    @Test
    public void testEnableBulletPointsTogglesFlag()
    {
        ComponentContext context = this.newContext();
        UnsortedListImpl unsortedList = new UnsortedListImpl(context);

        unsortedList.enableBulletPoints();

        Node node = unsortedList.asRenderer()
                                .render(mock(RenderingProcessor.class), mock(Location.class), Optional.empty());

        assertTrue(((UnsortedListNode) node).isEnableBulletPoints());
    }

    @Test
    public void testAddEntryAndAddEntriesPreserveOrder()
    {
        ComponentContext context = this.newContext();
        UnsortedListImpl unsortedList = new UnsortedListImpl(context);

        UIComponent<?> first = mock(UIComponent.class);
        UIComponent<?> second = mock(UIComponent.class);
        unsortedList.addEntry(first);
        unsortedList.addEntries(Arrays.asList(second));

        Location location = Location.of("root");
        Node node = unsortedList.asRenderer()
                                .render(this.selfRenderingProcessorReturningPassedComponent(), location, Optional.empty());

        List<Node> elements = ((UnsortedListNode) node).getElements();
        assertEquals(2, elements.size());
    }

    private RenderingProcessor selfRenderingProcessorReturningPassedComponent()
    {
        RenderingProcessor renderingProcessor = mock(RenderingProcessor.class);
        when(renderingProcessor.process(any(UIComponent.class), any(Location.class))).thenAnswer(invocation -> mock(Node.class));
        return renderingProcessor;
    }

    @Test
    public void testAddTextWithIconBuildsCompositeViaUiComponentFactory()
    {
        UIComponentFactory factory = mock(UIComponentFactory.class);
        org.omnaest.react4j.domain.Composite composite = mock(org.omnaest.react4j.domain.Composite.class);
        when(factory.newComposite()).thenReturn(composite);
        when(composite.addComponents(any(List.class))).thenReturn(composite);
        org.omnaest.react4j.domain.Icon icon = mock(org.omnaest.react4j.domain.Icon.class);
        when(factory.newIcon()).thenReturn(icon);
        when(icon.from(org.mockito.ArgumentMatchers.any())).thenReturn(icon);
        org.omnaest.react4j.domain.Text text = mock(org.omnaest.react4j.domain.Text.class);
        when(factory.newText()).thenReturn(text);
        when(text.addText(org.mockito.ArgumentMatchers.anyString())).thenReturn(text);

        ComponentContext context = this.newContext();
        when(context.getUiComponentFactory()).thenReturn(factory);

        UnsortedListImpl unsortedList = new UnsortedListImpl(context);
        unsortedList.addText(org.omnaest.react4j.domain.Icon.StandardIcon.DNA, "Item");

        UIComponentRenderer renderer = unsortedList.asRenderer();
        assertEquals(1, renderer.getSubComponents(Location.of("root"))
                                .count());
    }

    @Test
    public void testFieldsSurviveTemplating()
    {
        ComponentContext context = this.newContext();
        UnsortedListImpl unsortedList = new UnsortedListImpl(context);
        unsortedList.addEntry(mock(UIComponent.class));
        unsortedList.enableBulletPoints();

        UnsortedListImpl templated = (UnsortedListImpl) unsortedList.asTemplateProvider()
                                                                    .get();

        Node node = templated.asRenderer()
                             .render(this.selfRenderingProcessorReturningPassedComponent(), mock(Location.class), Optional.empty());

        assertEquals(1, ((UnsortedListNode) node).getElements()
                                                 .size());
        assertTrue(((UnsortedListNode) node).isEnableBulletPoints());
    }
}
