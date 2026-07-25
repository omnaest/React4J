package org.omnaest.react4j.service.internal.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer.ParentLocationAndComponent;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.service.internal.nodes.PaddingContainerNode;
import org.omnaest.react4j.service.internal.service.LocalizedTextResolverService;

/**
 * Goal-1 contract-fidelity test: asserts the {@link PaddingContainerImpl} builder API ({@code withVerticalPadding}/
 * {@code withHorizontalPadding}/{@code withNoVerticalPadding}/{@code withNoHorizontalPadding}/{@code withContent}) maps
 * onto the produced {@link PaddingContainerNode}, and that the wrapped content is exposed as a sub component.
 *
 * @see PaddingContainerImpl
 * @author omnaest
 */
public class PaddingContainerImplTest
{
    private ComponentContext newContext()
    {
        ComponentContext context = mock(ComponentContext.class);
        when(context.getTextResolver()).thenReturn(mock(LocalizedTextResolverService.class));
        return context;
    }

    @Test
    public void testDefaultsHavePaddingEnabledOnBothAxes()
    {
        ComponentContext context = this.newContext();
        PaddingContainerImpl paddingContainer = new PaddingContainerImpl(context);
        paddingContainer.withContent(mock(UIComponent.class));

        Node node = paddingContainer.asRenderer()
                                    .render(mock(RenderingProcessor.class), mock(Location.class), Optional.empty());

        PaddingContainerNode paddingNode = (PaddingContainerNode) node;
        assertTrue(paddingNode.isHorizontal());
        assertTrue(paddingNode.isVertical());
    }

    @Test
    public void testWithNoHorizontalAndNoVerticalPaddingDisableRespectiveAxes()
    {
        ComponentContext context = this.newContext();
        PaddingContainerImpl paddingContainer = new PaddingContainerImpl(context);
        paddingContainer.withContent(mock(UIComponent.class));
        paddingContainer.withNoHorizontalPadding();
        paddingContainer.withNoVerticalPadding();

        Node node = paddingContainer.asRenderer()
                                    .render(mock(RenderingProcessor.class), mock(Location.class), Optional.empty());

        PaddingContainerNode paddingNode = (PaddingContainerNode) node;
        assertFalse(paddingNode.isHorizontal());
        assertFalse(paddingNode.isVertical());
    }

    @Test
    public void testContentIsRenderedAndExposedAsSubComponent()
    {
        ComponentContext context = this.newContext();
        UIComponent<?> content = mock(UIComponent.class);
        PaddingContainerImpl paddingContainer = new PaddingContainerImpl(context);
        paddingContainer.withContent(content);

        RenderingProcessor renderingProcessor = mock(RenderingProcessor.class);
        Location location = mock(Location.class);
        Node contentNode = mock(Node.class);
        when(renderingProcessor.process(content, location)).thenReturn(contentNode);

        UIComponentRenderer renderer = paddingContainer.asRenderer();
        Node node = renderer.render(renderingProcessor, location, Optional.empty());

        assertSame(contentNode, ((PaddingContainerNode) node).getContent());

        ParentLocationAndComponent subComponent = renderer.getSubComponents(location)
                                                          .findFirst()
                                                          .orElseThrow();
        assertSame(content, subComponent.getComponent());
    }

    @Test
    public void testFieldsSurviveTemplating()
    {
        ComponentContext context = this.newContext();
        UIComponent<?> content = mock(UIComponent.class);
        PaddingContainerImpl paddingContainer = new PaddingContainerImpl(context);
        paddingContainer.withContent(content);
        paddingContainer.withNoHorizontalPadding();

        PaddingContainerImpl templated = (PaddingContainerImpl) paddingContainer.asTemplateProvider()
                                                                                .get();

        Node node = templated.asRenderer()
                             .render(mock(RenderingProcessor.class), mock(Location.class), Optional.empty());

        assertEquals(false, ((PaddingContainerNode) node).isHorizontal());
    }
}
