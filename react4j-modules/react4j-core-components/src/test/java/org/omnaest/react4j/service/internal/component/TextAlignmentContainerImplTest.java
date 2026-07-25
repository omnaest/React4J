package org.omnaest.react4j.service.internal.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.TextAlignmentContainer.HorizontalAlignment;
import org.omnaest.react4j.domain.TextAlignmentContainer.VerticalAlignment;
import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.service.internal.nodes.TextAlignmentContainerNode;
import org.omnaest.react4j.service.internal.service.LocalizedTextResolverService;

/**
 * Goal-1 contract-fidelity test: asserts the {@link TextAlignmentContainerImpl} builder API ({@code withEllipsis}/
 * {@code withNowrap}/{@code withHorizontalAlignment}/{@code withVerticalAlignment}/{@code withContent}) maps onto the
 * produced {@link TextAlignmentContainerNode}. This component is excluded from the Goal-4 static-render meta-test's
 * bare-instantiation fixture (see {@code NodeRendererCompletenessMetaTest}) but is fully testable here with a
 * minimally-configured (non-bare) fixture, following the {@code FigureImplTest} precedent.
 *
 * @see TextAlignmentContainerImpl
 * @author omnaest
 */
public class TextAlignmentContainerImplTest
{
    private ComponentContext newContext()
    {
        ComponentContext context = mock(ComponentContext.class);
        when(context.getTextResolver()).thenReturn(mock(LocalizedTextResolverService.class));
        return context;
    }

    @Test
    public void testDefaultsHaveNoAlignmentAndDisabledFlags()
    {
        ComponentContext context = this.newContext();
        TextAlignmentContainerImpl container = new TextAlignmentContainerImpl(context);
        container.withContent(mock(UIComponent.class));

        Node node = container.asRenderer()
                             .render(mock(RenderingProcessor.class), mock(Location.class), Optional.empty());

        TextAlignmentContainerNode alignmentNode = (TextAlignmentContainerNode) node;
        assertFalse(alignmentNode.isEllipsis());
        assertFalse(alignmentNode.isNowrap());
        assertNull(alignmentNode.getHorizontalAlignment());
        assertNull(alignmentNode.getVerticalAlignment());
    }

    @Test
    public void testWithEllipsisAndNowrapMapToNode()
    {
        ComponentContext context = this.newContext();
        TextAlignmentContainerImpl container = new TextAlignmentContainerImpl(context);
        container.withContent(mock(UIComponent.class));

        container.withEllipsis(true);
        container.withNowrap(true);

        Node node = container.asRenderer()
                             .render(mock(RenderingProcessor.class), mock(Location.class), Optional.empty());

        TextAlignmentContainerNode alignmentNode = (TextAlignmentContainerNode) node;
        assertTrue(alignmentNode.isEllipsis());
        assertTrue(alignmentNode.isNowrap());
    }

    @Test
    public void testWithHorizontalAndVerticalAlignmentMapToLowercaseEnumName()
    {
        ComponentContext context = this.newContext();
        TextAlignmentContainerImpl container = new TextAlignmentContainerImpl(context);
        container.withContent(mock(UIComponent.class));

        container.withHorizontalAlignment(HorizontalAlignment.CENTER);
        container.withVerticalAlignment(VerticalAlignment.MIDDLE);

        Node node = container.asRenderer()
                             .render(mock(RenderingProcessor.class), mock(Location.class), Optional.empty());

        TextAlignmentContainerNode alignmentNode = (TextAlignmentContainerNode) node;
        assertEquals("center", alignmentNode.getHorizontalAlignment());
        assertEquals("middle", alignmentNode.getVerticalAlignment());
    }

    @Test
    public void testContentIsRenderedAndExposedAsSubComponent()
    {
        ComponentContext context = this.newContext();
        UIComponent<?> content = mock(UIComponent.class);
        TextAlignmentContainerImpl container = new TextAlignmentContainerImpl(context);
        container.withContent(content);

        RenderingProcessor renderingProcessor = mock(RenderingProcessor.class);
        Location location = mock(Location.class);
        Node contentNode = mock(Node.class);
        when(renderingProcessor.process(content, location)).thenReturn(contentNode);

        Node node = container.asRenderer()
                             .render(renderingProcessor, location, Optional.empty());

        assertSame(contentNode, ((TextAlignmentContainerNode) node).getContent());
        assertSame(content, container.asRenderer()
                                     .getSubComponents(location)
                                     .findFirst()
                                     .orElseThrow()
                                     .getComponent());
    }

    @Test
    public void testFieldsSurviveTemplating()
    {
        ComponentContext context = this.newContext();
        TextAlignmentContainerImpl container = new TextAlignmentContainerImpl(context);
        container.withContent(mock(UIComponent.class));
        container.withEllipsis(true);
        container.withHorizontalAlignment(HorizontalAlignment.RIGHT);

        TextAlignmentContainerImpl templated = (TextAlignmentContainerImpl) container.asTemplateProvider()
                                                                                     .get();

        Node node = templated.asRenderer()
                             .render(mock(RenderingProcessor.class), mock(Location.class), Optional.empty());

        TextAlignmentContainerNode alignmentNode = (TextAlignmentContainerNode) node;
        assertTrue(alignmentNode.isEllipsis());
        assertEquals("right", alignmentNode.getHorizontalAlignment());
    }
}
