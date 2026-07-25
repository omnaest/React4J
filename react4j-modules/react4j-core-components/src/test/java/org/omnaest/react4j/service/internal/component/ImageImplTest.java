package org.omnaest.react4j.service.internal.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.i18n.I18nText;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.service.internal.nodes.ImageNode;
import org.omnaest.react4j.service.internal.nodes.i18n.I18nTextValue;
import org.omnaest.react4j.service.internal.service.LocalizedTextResolverService;

/**
 * Goal-1 contract-fidelity test: asserts the {@link ImageImpl} builder API ({@code withName}/{@code withImage}) maps
 * onto the produced {@link ImageNode}, and that Image is a leaf.
 *
 * @see ImageImpl
 * @author omnaest
 */
public class ImageImplTest
{
    private ComponentContext newContext()
    {
        ComponentContext context = mock(ComponentContext.class);
        when(context.getTextResolver()).thenReturn(mock(LocalizedTextResolverService.class));
        return context;
    }

    @Test
    public void testDefaultsAreNull()
    {
        ComponentContext context = this.newContext();
        ImageImpl image = new ImageImpl(context);

        Node node = image.asRenderer()
                         .render(mock(RenderingProcessor.class), mock(Location.class), Optional.empty());

        assertNull(((ImageNode) node).getImage());
        assertNull(((ImageNode) node).getName());
    }

    @Test
    public void testWithNameAndWithImageMapToNode()
    {
        ComponentContext context = this.newContext();
        LocalizedTextResolverService textResolver = context.getTextResolver();
        Location location = mock(Location.class);
        I18nTextValue resolvedName = new I18nTextValue(Map.of("DEFAULT", "alt-text"));
        when(textResolver.apply(any(I18nText.class), eq(location))).thenReturn(resolvedName);

        ImageImpl image = new ImageImpl(context);
        image.withName("alt-text");
        image.withImage("photo.png");

        Node node = image.asRenderer()
                         .render(mock(RenderingProcessor.class), location, Optional.empty());

        assertEquals("photo.png", ((ImageNode) node).getImage());
        assertSame(resolvedName, ((ImageNode) node).getName());
    }

    @Test
    public void testImageIsALeaf()
    {
        ComponentContext context = this.newContext();
        ImageImpl image = new ImageImpl(context);

        UIComponentRenderer renderer = image.asRenderer();

        assertEquals(0, renderer.getSubComponents(mock(Location.class))
                                .count());
    }

    @Test
    public void testFieldsSurviveTemplating()
    {
        ComponentContext context = this.newContext();
        ImageImpl image = new ImageImpl(context);
        image.withImage("photo.png");

        ImageImpl templated = (ImageImpl) image.asTemplateProvider()
                                               .get();

        Node node = templated.asRenderer()
                             .render(mock(RenderingProcessor.class), mock(Location.class), Optional.empty());

        assertEquals("photo.png", ((ImageNode) node).getImage());
    }
}
