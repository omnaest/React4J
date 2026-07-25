package org.omnaest.react4j.service.internal.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.omnaest.react4j.domain.Image;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.UIComponentFactory;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.service.internal.nodes.CardNode;
import org.omnaest.react4j.service.internal.nodes.ImageNode;
import org.omnaest.react4j.service.internal.service.LocalizedTextResolverService;
import org.omnaest.react4j.service.internal.service.LocalizedTextResolverService.LocationAwareTextResolver;

/**
 * Goal-1 contract-fidelity test: asserts the {@link CardImpl} builder API ({@code withTitle}/{@code withSubTitle}/
 * {@code withImage}/{@code withLinkLocator}/{@code withContent}/{@code withAdjustment}) maps onto the produced
 * {@link CardNode}, including the title-vs-featuredTitle swap that depends on whether an image is present.
 *
 * @see CardImpl
 * @author omnaest
 */
public class CardImplTest
{
    private ComponentContext newContext(UIComponentFactory factory)
    {
        ComponentContext context = mock(ComponentContext.class);
        LocalizedTextResolverService textResolver = mock(LocalizedTextResolverService.class);
        when(context.getTextResolver()).thenReturn(textResolver);
        when(context.getUiComponentFactory()).thenReturn(factory);
        return context;
    }

    private LocationAwareTextResolver stubLocationAwareTextResolver(ComponentContext context, Location location)
    {
        LocationAwareTextResolver locationAwareTextResolver = mock(LocationAwareTextResolver.class);
        when(context.getTextResolver()
                    .apply(location)).thenReturn(locationAwareTextResolver);
        return locationAwareTextResolver;
    }

    @Test
    public void testWithoutImageTitleGoesToFeaturedTitle()
    {
        UIComponentFactory factory = mock(UIComponentFactory.class);
        ComponentContext context = this.newContext(factory);
        Location location = mock(Location.class);
        this.stubLocationAwareTextResolver(context, location);

        CardImpl card = new CardImpl(context);
        card.withTitle("Headline");

        Node node = card.asRenderer()
                        .render(mock(RenderingProcessor.class), location, Optional.empty());

        CardNode cardNode = (CardNode) node;
        assertNotNull(cardNode.getFeaturedTitle());
        assertNull(cardNode.getTitle());
    }

    @Test
    public void testWithImageThenTitleGoesToTitleNotFeaturedTitle()
    {
        UIComponentFactory factory = mock(UIComponentFactory.class);
        Image image = mock(Image.class);
        when(factory.newImage()).thenReturn(image);

        ComponentContext context = this.newContext(factory);
        Location location = mock(Location.class);
        this.stubLocationAwareTextResolver(context, location);

        CardImpl card = new CardImpl(context);
        card.withImage(img -> img.withImage("photo.png"));
        card.withTitle("Headline");

        RenderingProcessor renderingProcessor = mock(RenderingProcessor.class);
        ImageNode imageNode = mock(ImageNode.class);
        when(renderingProcessor.process(image, location)).thenReturn(imageNode);

        Node node = card.asRenderer()
                        .render(renderingProcessor, location, Optional.empty());

        CardNode cardNode = (CardNode) node;
        assertNotNull(cardNode.getTitle());
        assertNull(cardNode.getFeaturedTitle());
        assertSame(imageNode, cardNode.getImage());
    }

    @Test
    public void testTitleSetBeforeImageIsSwappedToTitleWhenImageIsAddedLater()
    {
        UIComponentFactory factory = mock(UIComponentFactory.class);
        Image image = mock(Image.class);
        when(factory.newImage()).thenReturn(image);

        ComponentContext context = this.newContext(factory);
        Location location = mock(Location.class);
        this.stubLocationAwareTextResolver(context, location);

        CardImpl card = new CardImpl(context);
        card.withTitle("Headline");
        card.withImage(img -> img.withImage("photo.png"));

        Node node = card.asRenderer()
                        .render(mock(RenderingProcessor.class), location, Optional.empty());

        CardNode cardNode = (CardNode) node;
        assertNotNull(cardNode.getTitle());
        assertNull(cardNode.getFeaturedTitle());
    }

    @Test
    public void testWithLinkLocatorAndAdjustmentMapToNode()
    {
        UIComponentFactory factory = mock(UIComponentFactory.class);
        ComponentContext context = this.newContext(factory);

        CardImpl card = new CardImpl(context);
        card.withLinkLocator("card-locator");
        card.withAdjustment(true);

        Node node = card.asRenderer()
                        .render(mock(RenderingProcessor.class), mock(Location.class), Optional.empty());

        CardNode cardNode = (CardNode) node;
        assertEquals("card-locator", cardNode.getLocator());
        assertTrue(cardNode.isAdjust());
    }

    @Test
    public void testWithContentIsRenderedAndExposedAsSubComponent()
    {
        UIComponentFactory factory = mock(UIComponentFactory.class);
        ComponentContext context = this.newContext(factory);

        UIComponent<?> content = mock(UIComponent.class);
        CardImpl card = new CardImpl(context);
        card.withContent(content);

        RenderingProcessor renderingProcessor = mock(RenderingProcessor.class);
        Location location = mock(Location.class);
        Node contentNode = mock(Node.class);
        when(renderingProcessor.process(content, location)).thenReturn(contentNode);

        Node node = card.asRenderer()
                        .render(renderingProcessor, location, Optional.empty());

        assertSame(contentNode, ((CardNode) node).getContent());
        assertSame(content, card.asRenderer()
                                .getSubComponents(location)
                                .findFirst()
                                .orElseThrow()
                                .getComponent());
    }

    @Test
    public void testDefaultsHaveNoImageAndNoAdjustment()
    {
        UIComponentFactory factory = mock(UIComponentFactory.class);
        ComponentContext context = this.newContext(factory);

        CardImpl card = new CardImpl(context);

        Node node = card.asRenderer()
                        .render(mock(RenderingProcessor.class), mock(Location.class), Optional.empty());

        CardNode cardNode = (CardNode) node;
        assertNull(cardNode.getImage());
        assertFalse(cardNode.isAdjust());
    }
}
