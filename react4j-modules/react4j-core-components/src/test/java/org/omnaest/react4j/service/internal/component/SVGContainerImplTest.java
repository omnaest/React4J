package org.omnaest.react4j.service.internal.component;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.SVGContainer.AlignmentProvider;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.service.internal.nodes.NativeHtmlNode;
import org.omnaest.react4j.service.internal.service.LocalizedTextResolverService;

/**
 * Goal-1 contract-fidelity test: {@link SVGContainerImpl} delegates entirely to an internal {@link NativeHtmlImpl}, so
 * the contract under test is that {@code withSvg}/{@code withCSS}/{@code withAlignment} produce a
 * {@link NativeHtmlNode} whose source embeds the given SVG markup and the CSS from the CSS builder.
 *
 * @see SVGContainerImpl
 * @author omnaest
 */
public class SVGContainerImplTest
{
    private ComponentContext newContext()
    {
        ComponentContext context = mock(ComponentContext.class);
        when(context.getTextResolver()).thenReturn(mock(LocalizedTextResolverService.class));
        return context;
    }

    @Test
    public void testWithSvgEmbedsSvgMarkupInNativeHtmlSource()
    {
        ComponentContext context = this.newContext();
        SVGContainerImpl svgContainer = new SVGContainerImpl(context);

        svgContainer.withSvg("<circle r=\"5\" />");

        Node node = svgContainer.asRenderer()
                                .render(mock(RenderingProcessor.class), mock(Location.class), Optional.empty());

        assertNotNull(node);
        String source = ((NativeHtmlNode) node).getSource();
        assertTrue(source.contains("<circle r=\"5\" />"));
    }

    @Test
    public void testWithCSSAppliesWidthAndHeightBeforeSvgIsEmbedded()
    {
        ComponentContext context = this.newContext();
        SVGContainerImpl svgContainer = new SVGContainerImpl(context);

        svgContainer.withCSS(css -> css.width("100px")
                                       .height("50px"));
        svgContainer.withSvg("<rect />");

        Node node = svgContainer.asRenderer()
                                .render(mock(RenderingProcessor.class), mock(Location.class), Optional.empty());

        String source = ((NativeHtmlNode) node).getSource();
        assertTrue(source.contains("width: 100px;"));
        assertTrue(source.contains("height: 50px;"));
    }

    @Test
    public void testWithAlignmentDelegatesToCSSWidthAndHeight()
    {
        ComponentContext context = this.newContext();
        SVGContainerImpl svgContainer = new SVGContainerImpl(context);

        AlignmentProvider alignment = mock(AlignmentProvider.class);
        when(alignment.getWidth()).thenReturn("200px");
        when(alignment.getHeight()).thenReturn("75px");

        svgContainer.withAlignment(alignment);
        svgContainer.withSvg("<rect />");

        Node node = svgContainer.asRenderer()
                                .render(mock(RenderingProcessor.class), mock(Location.class), Optional.empty());

        String source = ((NativeHtmlNode) node).getSource();
        assertTrue(source.contains("width: 200px;"));
        assertTrue(source.contains("height: 75px;"));
    }

    @Test
    public void testFieldsSurviveTemplating()
    {
        ComponentContext context = this.newContext();
        SVGContainerImpl svgContainer = new SVGContainerImpl(context);
        svgContainer.withSvg("<circle />");

        SVGContainerImpl templated = (SVGContainerImpl) svgContainer.asTemplateProvider()
                                                                    .get();

        Node node = templated.asRenderer()
                             .render(mock(RenderingProcessor.class), mock(Location.class), Optional.empty());

        assertTrue(((NativeHtmlNode) node).getSource()
                                          .contains("<circle />"));
    }
}
