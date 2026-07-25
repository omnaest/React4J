package org.omnaest.react4j.service.internal.component;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.omnaest.react4j.domain.Image;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.UIComponentFactory;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.service.internal.nodes.FigureNode;
import org.omnaest.react4j.service.internal.service.LocalizedTextResolverService;

/**
 * @see FigureImpl
 * @author omnaest
 */
public class FigureImplTest
{
    private ComponentContext newContext(UIComponentFactory factory)
    {
        ComponentContext context = mock(ComponentContext.class);
        when(context.getTextResolver()).thenReturn(mock(LocalizedTextResolverService.class));
        when(context.getUiComponentFactory()).thenReturn(factory);
        return context;
    }

    @Test
    public void testImageChildIsLazilyBuiltAndSharedRegardlessOfSetterOrder()
    {
        UIComponentFactory factory = mock(UIComponentFactory.class);
        ComponentContext context = this.newContext(factory);
        Image realImage = new ImageImpl(context);
        when(factory.newImage()).thenReturn(realImage);

        FigureImpl figure = new FigureImpl(context);
        // name set before image - order must not matter
        figure.withName("alt-text");
        figure.withImage("photo.png");

        RenderingProcessor renderingProcessor = mock(RenderingProcessor.class);
        Location location = mock(Location.class);
        Node imageNode = mock(Node.class);
        when(renderingProcessor.process(realImage, location)).thenReturn(imageNode);

        UIComponentRenderer renderer = figure.asRenderer();
        Node node = renderer.render(renderingProcessor, location, Optional.empty());

        assertSame(imageNode, ((FigureNode) node).getImage());
        verify(factory, times(1)).newImage();
    }

    @Test
    public void testImageChildSurvivesTemplating()
    {
        UIComponentFactory factory = mock(UIComponentFactory.class);
        ComponentContext context = this.newContext(factory);
        Image realImage = new ImageImpl(context);
        when(factory.newImage()).thenReturn(realImage);

        FigureImpl figure = new FigureImpl(context);
        figure.withImage("photo.png");
        figure.withName("alt-text");
        figure.withCaption("a caption");

        FigureImpl templated = (FigureImpl) figure.asTemplateProvider()
                                                  .get();

        RenderingProcessor renderingProcessor = mock(RenderingProcessor.class);
        Location location = mock(Location.class);
        Node imageNode = mock(Node.class);
        when(renderingProcessor.process(realImage, location)).thenReturn(imageNode);

        UIComponentRenderer renderer = templated.asRenderer();
        Node node = renderer.render(renderingProcessor, location, Optional.empty());

        assertSame(imageNode, ((FigureNode) node).getImage());
        // the image was built exactly once (by the original), never rebuilt for the template
        verify(factory, times(1)).newImage();
    }
}
