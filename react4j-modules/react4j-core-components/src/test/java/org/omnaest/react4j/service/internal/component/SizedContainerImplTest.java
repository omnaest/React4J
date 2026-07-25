package org.omnaest.react4j.service.internal.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.service.internal.nodes.SizedContainerNode;
import org.omnaest.react4j.service.internal.service.LocalizedTextResolverService;

/**
 * Goal-1 contract-fidelity test: asserts the {@link SizedContainerImpl} builder API (ratio/pixel/viewport width and
 * height setters) maps onto the produced {@link SizedContainerNode}.
 *
 * @see SizedContainerImpl
 * @author omnaest
 */
public class SizedContainerImplTest
{
    private ComponentContext newContext()
    {
        ComponentContext context = mock(ComponentContext.class);
        when(context.getTextResolver()).thenReturn(mock(LocalizedTextResolverService.class));
        return context;
    }

    private Node render(SizedContainerImpl sizedContainer)
    {
        UIComponent<?> content = mock(UIComponent.class);
        sizedContainer.withContent(content);
        return sizedContainer.asRenderer()
                             .render(mock(RenderingProcessor.class), mock(Location.class), Optional.empty());
    }

    @Test
    public void testDefaultsAreAuto()
    {
        ComponentContext context = this.newContext();
        SizedContainerImpl sizedContainer = new SizedContainerImpl(context);

        SizedContainerNode node = (SizedContainerNode) this.render(sizedContainer);

        assertEquals("auto", node.getHeight());
        assertEquals("auto", node.getWidth());
    }

    @Test
    public void testWithFullWidthAndFullHeightMapToOneHundredPercent()
    {
        ComponentContext context = this.newContext();
        SizedContainerImpl sizedContainer = new SizedContainerImpl(context);

        sizedContainer.withFullWidth();
        sizedContainer.withFullHeight();

        SizedContainerNode node = (SizedContainerNode) this.render(sizedContainer);

        assertEquals("100%", node.getWidth());
        assertEquals("100%", node.getHeight());
    }

    @Test
    public void testWithWidthAndHeightRatioMapToPercentage()
    {
        ComponentContext context = this.newContext();
        SizedContainerImpl sizedContainer = new SizedContainerImpl(context);

        sizedContainer.withWidthRatio(0.5);
        sizedContainer.withHeightRatio(0.25);

        SizedContainerNode node = (SizedContainerNode) this.render(sizedContainer);

        assertEquals("50%", node.getWidth());
        assertEquals("25%", node.getHeight());
    }

    @Test
    public void testWithWidthAndHeightInPixelMapToPixelSuffix()
    {
        ComponentContext context = this.newContext();
        SizedContainerImpl sizedContainer = new SizedContainerImpl(context);

        sizedContainer.withWidthInPixel(320);
        sizedContainer.withHeightInPixel(240);

        SizedContainerNode node = (SizedContainerNode) this.render(sizedContainer);

        assertEquals("320px", node.getWidth());
        assertEquals("240px", node.getHeight());
    }

    @Test
    public void testWithWidthAndHeightInViewPortRatioMapToViewportSuffix()
    {
        ComponentContext context = this.newContext();
        SizedContainerImpl sizedContainer = new SizedContainerImpl(context);

        sizedContainer.withWidthInViewPortRatio(0.5);
        sizedContainer.withHeightInViewPortRatio(0.75);

        SizedContainerNode node = (SizedContainerNode) this.render(sizedContainer);

        assertEquals("50vw", node.getWidth());
        assertEquals("75vh", node.getHeight());
    }

    @Test
    public void testContentIsRendered()
    {
        ComponentContext context = this.newContext();
        UIComponent<?> content = mock(UIComponent.class);
        SizedContainerImpl sizedContainer = new SizedContainerImpl(context);
        sizedContainer.withContent(content);

        RenderingProcessor renderingProcessor = mock(RenderingProcessor.class);
        Location location = mock(Location.class);
        Node contentNode = mock(Node.class);
        when(renderingProcessor.process(content, location)).thenReturn(contentNode);

        Node node = sizedContainer.asRenderer()
                                  .render(renderingProcessor, location, Optional.empty());

        assertSame(contentNode, ((SizedContainerNode) node).getContent());
    }

    @Test
    public void testFieldsSurviveTemplating()
    {
        ComponentContext context = this.newContext();
        SizedContainerImpl sizedContainer = new SizedContainerImpl(context);
        sizedContainer.withContent(mock(UIComponent.class));
        sizedContainer.withWidthInPixel(100);

        SizedContainerImpl templated = (SizedContainerImpl) sizedContainer.asTemplateProvider()
                                                                          .get();

        Node node = templated.asRenderer()
                             .render(mock(RenderingProcessor.class), mock(Location.class), Optional.empty());

        assertEquals("100px", ((SizedContainerNode) node).getWidth());
    }
}
