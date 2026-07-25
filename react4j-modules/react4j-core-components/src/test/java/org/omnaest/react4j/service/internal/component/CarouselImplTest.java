package org.omnaest.react4j.service.internal.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.omnaest.react4j.domain.Image;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.UIComponentFactory;
import org.omnaest.react4j.domain.rendering.RenderableUIComponent;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.service.internal.nodes.CarouselItemNode;
import org.omnaest.react4j.service.internal.nodes.CarouselNode;
import org.omnaest.react4j.service.internal.nodes.ImageNode;
import org.omnaest.react4j.service.internal.service.LocalizedTextResolverService;

/**
 * @see CarouselImpl
 * @see CarouselSlideImpl
 * @author omnaest
 */
public class CarouselImplTest
{
    private ComponentContext newContext(UIComponentFactory factory)
    {
        ComponentContext context = mock(ComponentContext.class);
        when(context.getTextResolver()).thenReturn(mock(LocalizedTextResolverService.class));
        when(context.getUiComponentFactory()).thenReturn(factory);
        return context;
    }

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
    public void testDefaultFieldValues()
    {
        UIComponentFactory factory = mock(UIComponentFactory.class);
        ComponentContext context = this.newContext(factory);
        CarouselImpl carousel = new CarouselImpl(context);

        UIComponentRenderer renderer = carousel.asRenderer();
        RenderingProcessor renderingProcessor = mock(RenderingProcessor.class);
        Location location = mock(Location.class);

        CarouselNode node = (CarouselNode) renderer.render(renderingProcessor, location, Optional.empty());

        assertNull(node.getInterval());
        assertTrue(node.isControls());
        assertTrue(node.isIndicators());
        assertFalse(node.isFade());
        assertTrue(node.getItems()
                       .isEmpty());
    }

    @Test
    public void testBuilderSettersAppearOnNode()
    {
        UIComponentFactory factory = mock(UIComponentFactory.class);
        ComponentContext context = this.newContext(factory);
        CarouselImpl carousel = new CarouselImpl(context);

        carousel.withInterval(2, TimeUnit.SECONDS)
                .withControls(false)
                .withFade(true);

        UIComponentRenderer renderer = carousel.asRenderer();
        RenderingProcessor renderingProcessor = mock(RenderingProcessor.class);
        Location location = mock(Location.class);

        CarouselNode node = (CarouselNode) renderer.render(renderingProcessor, location, Optional.empty());

        assertEquals(2000, node.getInterval());
        assertFalse(node.isControls());
        assertTrue(node.isFade());
    }

    @Test
    public void testSlidesRenderWithNonNullDistinctImageChildren()
    {
        UIComponentFactory factory = mock(UIComponentFactory.class);
        when(factory.newImage()).thenAnswer(invocation -> new ImageImpl(this.newContext(factory)));
        ComponentContext context = this.newContext(factory);
        CarouselImpl carousel = new CarouselImpl(context);

        carousel.addSlide(slide -> slide.withImage("photo1.png"));
        carousel.addSlide(slide -> slide.withImage("photo2.png"));

        UIComponentRenderer renderer = carousel.asRenderer();
        RenderingProcessor renderingProcessor = this.selfRenderingProcessor();
        Location location = mock(Location.class);
        when(location.get()).thenReturn(Arrays.asList("root"));

        CarouselNode node = (CarouselNode) renderer.render(renderingProcessor, location, Optional.empty());

        List<CarouselItemNode> items = node.getItems();
        assertEquals(2, items.size());
        assertNotNull(items.get(0)
                           .getImage());
        assertNotNull(items.get(1)
                           .getImage());
        assertEquals("photo1.png", ((ImageNode) items.get(0)
                                                     .getImage()).getImage());
        assertEquals("photo2.png", ((ImageNode) items.get(1)
                                                     .getImage()).getImage());
    }

    @Test
    public void testFieldsAndSlidesSurviveTemplating()
    {
        UIComponentFactory factory = mock(UIComponentFactory.class);
        Image realImage = new ImageImpl(this.newContext(factory));
        when(factory.newImage()).thenReturn(realImage);
        ComponentContext context = this.newContext(factory);
        CarouselImpl carousel = new CarouselImpl(context);

        carousel.withInterval(5, TimeUnit.SECONDS)
                .withControls(false)
                .withIndicators(false)
                .withFade(true);
        carousel.addSlide(slide -> slide.withImage("photo.png")
                                        .withCaption("a caption"));

        CarouselImpl templated = (CarouselImpl) carousel.asTemplateProvider()
                                                        .get();

        UIComponentRenderer renderer = templated.asRenderer();
        RenderingProcessor renderingProcessor = this.selfRenderingProcessor();
        Location location = mock(Location.class);
        when(location.get()).thenReturn(Arrays.asList("root"));

        CarouselNode node = (CarouselNode) renderer.render(renderingProcessor, location, Optional.empty());

        assertEquals(5000, node.getInterval());
        assertFalse(node.isControls());
        assertFalse(node.isIndicators());
        assertTrue(node.isFade());
        List<CarouselItemNode> items = node.getItems();
        assertEquals(1, items.size());
        assertNotNull(items.get(0)
                           .getImage());
    }
}
