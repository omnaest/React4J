package org.omnaest.react4j.service.internal.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.i18n.I18nText;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer.EventHandlerRegistrationSupport;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.service.internal.nodes.TextNode;
import org.omnaest.react4j.service.internal.nodes.i18n.I18nTextValue;
import org.omnaest.react4j.service.internal.service.LocalizedTextResolverService;

/**
 * Goal-1 contract-fidelity test: asserts the {@link TextImpl} builder API ({@code addText}/{@code addNonTranslatedText})
 * maps onto the produced {@link TextNode} - each added text is resolved via the text resolver, in order, and Text is a
 * leaf (no sub components, no event handler).
 *
 * @see TextImpl
 * @author omnaest
 */
public class TextImplTest
{
    private ComponentContext newContext()
    {
        ComponentContext context = mock(ComponentContext.class);
        when(context.getTextResolver()).thenReturn(mock(LocalizedTextResolverService.class));
        return context;
    }

    @Test
    public void testDefaultTextsAreEmpty()
    {
        ComponentContext context = this.newContext();
        TextImpl text = new TextImpl(context);

        Node node = text.asRenderer()
                        .render(mock(RenderingProcessor.class), mock(Location.class), Optional.empty());

        assertTrue(((TextNode) node).getTexts()
                                    .isEmpty());
    }

    @Test
    public void testAddTextsAreResolvedInOrder()
    {
        ComponentContext context = this.newContext();
        LocalizedTextResolverService textResolver = context.getTextResolver();
        Location location = mock(Location.class);
        I18nTextValue first = new I18nTextValue(Map.of("DEFAULT", "First"));
        I18nTextValue second = new I18nTextValue(Map.of("DEFAULT", "Second"));

        TextImpl text = new TextImpl(context);
        text.addText("First");
        text.addText("Second");

        when(textResolver.apply(any(I18nText.class), eq(location))).thenAnswer(invocation ->
        {
            I18nText i18nText = invocation.getArgument(0);
            return "First".equals(i18nText.getDefaultText()) ? first : second;
        });

        Node node = text.asRenderer()
                        .render(mock(RenderingProcessor.class), location, Optional.empty());

        List<I18nTextValue> texts = ((TextNode) node).getTexts();
        assertEquals(2, texts.size());
        assertSame(first, texts.get(0));
        assertSame(second, texts.get(1));
    }

    @Test
    public void testAddNonTranslatedTextMarksI18nTextAsNonTranslatable()
    {
        ComponentContext context = this.newContext();
        LocalizedTextResolverService textResolver = context.getTextResolver();
        Location location = mock(Location.class);

        TextImpl text = new TextImpl(context);
        text.addNonTranslatedText("Raw");

        text.asRenderer()
            .render(mock(RenderingProcessor.class), location, Optional.empty());

        org.mockito.ArgumentCaptor<I18nText> captor = org.mockito.ArgumentCaptor.forClass(I18nText.class);
        verify(textResolver).apply(captor.capture(), eq(location));
        assertEquals("Raw", captor.getValue()
                                  .getDefaultText());
        assertTrue(captor.getValue()
                         .isNonTranslatable());
    }

    @Test
    public void testTextIsALeafWithNoSubComponentsAndNoEventHandler()
    {
        ComponentContext context = this.newContext();
        TextImpl text = new TextImpl(context);
        text.addText("Hi");

        UIComponentRenderer renderer = text.asRenderer();

        assertEquals(0, renderer.getSubComponents(mock(Location.class))
                                .count());

        EventHandlerRegistrationSupport support = mock(EventHandlerRegistrationSupport.class);
        renderer.manageEventHandler(support);
        verify(support, never()).register(any(org.omnaest.react4j.service.internal.handler.domain.EventHandler.class));
    }

    @Test
    public void testTextsSurviveTemplating()
    {
        ComponentContext context = this.newContext();
        TextImpl text = new TextImpl(context);
        text.addText("Hi");
        text.addText("There");

        TextImpl templated = (TextImpl) text.asTemplateProvider()
                                            .get();

        Node node = templated.asRenderer()
                             .render(mock(RenderingProcessor.class), mock(Location.class), Optional.empty());

        assertEquals(2, ((TextNode) node).getTexts()
                                         .size());
    }
}
