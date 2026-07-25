package org.omnaest.react4j.service.internal.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.omnaest.react4j.component.anker.Anker;
import org.omnaest.react4j.component.ankerbutton.AnkerButton;
import org.omnaest.react4j.domain.Composite;
import org.omnaest.react4j.domain.Heading;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.UIComponentFactory;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.RenderableUIComponent;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.service.internal.nodes.ParagraphNode;
import org.omnaest.react4j.service.internal.service.LocalizedTextResolverService;

/**
 * Goal-1 contract-fidelity test: asserts the {@link ParagraphImpl} builder API ({@code addText}/{@code addHeading}/
 * {@code addLineBreak}/{@code addImage}/{@code addLink}/{@code addLinkButton}/{@code addComponent}/
 * {@code withBoldStyle}) maps onto the produced {@link ParagraphNode} elements, in order, each addressable via
 * {@code getSubComponents}.
 *
 * @see ParagraphImpl
 * @author omnaest
 */
public class ParagraphImplTest
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
            if (component instanceof RenderableUIComponent)
            {
                RenderableUIComponent<?> renderable = (RenderableUIComponent<?>) component;
                return renderable.asRenderer()
                                 .render(this.selfRenderingProcessor(), location, Optional.empty());
            }
            return mock(Node.class);
        });
        return renderingProcessor;
    }

    @Test
    public void testDefaultsHaveEmptyElementsAndRegularStyle()
    {
        UIComponentFactory factory = mock(UIComponentFactory.class);
        ComponentContext context = this.newContext(factory);
        ParagraphImpl paragraph = new ParagraphImpl(context);

        Node node = paragraph.asRenderer()
                             .render(mock(RenderingProcessor.class), mock(Location.class), Optional.empty());

        ParagraphNode paragraphNode = (ParagraphNode) node;
        assertTrue(paragraphNode.getElements()
                                .isEmpty());
        assertFalse(paragraphNode.isBold());
    }

    @Test
    public void testWithBoldStyleMapsToNode()
    {
        UIComponentFactory factory = mock(UIComponentFactory.class);
        ComponentContext context = this.newContext(factory);
        ParagraphImpl paragraph = new ParagraphImpl(context);

        paragraph.withBoldStyle();

        Node node = paragraph.asRenderer()
                             .render(mock(RenderingProcessor.class), mock(Location.class), Optional.empty());

        assertTrue(((ParagraphNode) node).isBold());
    }

    @Test
    public void testAddTextBuildsCompositeOfTextViaUiComponentFactory()
    {
        UIComponentFactory factory = mock(UIComponentFactory.class);
        Composite composite = mock(Composite.class);
        when(factory.newComposite()).thenReturn(composite);
        when(composite.addComponent(any(UIComponent.class))).thenReturn(composite);
        org.omnaest.react4j.domain.Text text = mock(org.omnaest.react4j.domain.Text.class);
        when(factory.newText()).thenReturn(text);
        when(text.addText(org.mockito.ArgumentMatchers.anyString())).thenReturn(text);

        ComponentContext context = this.newContext(factory);
        ParagraphImpl paragraph = new ParagraphImpl(context);

        paragraph.addText("Hello");

        Node node = paragraph.asRenderer()
                             .render(this.selfRenderingProcessor(), Location.of("root"), Optional.empty());

        assertEquals(1, ((ParagraphNode) node).getElements()
                                              .size());
    }

    @Test
    public void testAddHeadingLineBreakAndImageAppendInOrder()
    {
        UIComponentFactory factory = mock(UIComponentFactory.class);
        Heading heading = mock(Heading.class);
        when(factory.newHeading()).thenReturn(heading);
        when(heading.withLevel(org.mockito.ArgumentMatchers.anyInt())).thenReturn(heading);
        when(heading.withText(org.mockito.ArgumentMatchers.anyString())).thenReturn(heading);

        org.omnaest.react4j.domain.LineBreak lineBreak = mock(org.omnaest.react4j.domain.LineBreak.class);
        when(factory.newLineBreak()).thenReturn(lineBreak);

        org.omnaest.react4j.domain.Image image = mock(org.omnaest.react4j.domain.Image.class);
        when(factory.newImage()).thenReturn(image);
        when(image.withName(org.mockito.ArgumentMatchers.anyString())).thenReturn(image);
        when(image.withImage(org.mockito.ArgumentMatchers.anyString())).thenReturn(image);

        ComponentContext context = this.newContext(factory);
        ParagraphImpl paragraph = new ParagraphImpl(context);

        paragraph.addHeading("Title", 2);
        paragraph.addLineBreak();
        paragraph.addImage("alt", "photo.png");

        Node node = paragraph.asRenderer()
                             .render(this.selfRenderingProcessor(), Location.of("root"), Optional.empty());

        List<Node> elements = ((ParagraphNode) node).getElements();
        assertEquals(3, elements.size());
    }

    @Test
    public void testAddLinkAndAddLinkButtonBuildViaUiComponentFactory()
    {
        UIComponentFactory factory = mock(UIComponentFactory.class);
        Anker anker = mock(Anker.class);
        when(factory.newAnker()).thenReturn(anker);
        AnkerButton ankerButton = mock(AnkerButton.class);
        when(factory.newAnkerButton()).thenReturn(ankerButton);

        ComponentContext context = this.newContext(factory);
        ParagraphImpl paragraph = new ParagraphImpl(context);

        paragraph.addLink(link -> link.withLink("https://example.org"));
        paragraph.addLinkButton(button -> button.withLink("https://example.org/button"));

        org.mockito.Mockito.verify(anker)
                           .withLink("https://example.org");
        org.mockito.Mockito.verify(ankerButton)
                           .withLink("https://example.org/button");

        Node node = paragraph.asRenderer()
                             .render(this.selfRenderingProcessor(), Location.of("root"), Optional.empty());

        assertEquals(2, ((ParagraphNode) node).getElements()
                                              .size());
    }

    @Test
    public void testAddComponentAppendsDirectlyAndIsAddressableAsSubComponent()
    {
        UIComponentFactory factory = mock(UIComponentFactory.class);
        ComponentContext context = this.newContext(factory);
        ParagraphImpl paragraph = new ParagraphImpl(context);

        UIComponent<?> component = mock(UIComponent.class);
        paragraph.addComponent(component);

        Location rootLocation = Location.of("root");
        long subComponentCount = paragraph.asRenderer()
                                          .getSubComponents(rootLocation)
                                          .count();
        assertEquals(1, subComponentCount);
    }

    @Test
    public void testElementsSurviveTemplating()
    {
        UIComponentFactory factory = mock(UIComponentFactory.class);
        ComponentContext context = this.newContext(factory);
        ParagraphImpl paragraph = new ParagraphImpl(context);
        paragraph.addComponent(mock(UIComponent.class));
        paragraph.withBoldStyle();

        ParagraphImpl templated = (ParagraphImpl) paragraph.asTemplateProvider()
                                                           .get();

        Node node = templated.asRenderer()
                             .render(this.selfRenderingProcessor(), Location.of("root"), Optional.empty());

        ParagraphNode paragraphNode = (ParagraphNode) node;
        assertEquals(1, paragraphNode.getElements()
                                     .size());
        assertTrue(paragraphNode.isBold());
    }
}
