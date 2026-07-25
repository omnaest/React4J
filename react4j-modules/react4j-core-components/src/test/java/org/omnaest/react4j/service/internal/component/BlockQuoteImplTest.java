package org.omnaest.react4j.service.internal.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.i18n.I18nText;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.service.internal.nodes.BlockQuoteNode;
import org.omnaest.react4j.service.internal.nodes.i18n.I18nTextValue;
import org.omnaest.react4j.service.internal.service.LocalizedTextResolverService;

/**
 * Goal-1 contract-fidelity test: asserts the {@link BlockQuoteImpl} builder API ({@code addText}/{@code withFooter})
 * maps onto the produced {@link BlockQuoteNode}.
 *
 * @see BlockQuoteImpl
 * @author omnaest
 */
public class BlockQuoteImplTest
{
    private ComponentContext newContext()
    {
        ComponentContext context = mock(ComponentContext.class);
        when(context.getTextResolver()).thenReturn(mock(LocalizedTextResolverService.class));
        return context;
    }

    @Test
    public void testDefaultsHaveEmptyTextsAndNullFooter()
    {
        ComponentContext context = this.newContext();
        LocalizedTextResolverService textResolver = context.getTextResolver();
        Location location = mock(Location.class);
        when(textResolver.apply(anyListOfI18nText(), eq(location))).thenReturn(List.of());

        BlockQuoteImpl blockQuote = new BlockQuoteImpl(context);

        Node node = blockQuote.asRenderer()
                              .render(mock(RenderingProcessor.class), location, Optional.empty());

        assertTrue(((BlockQuoteNode) node).getTexts()
                                          .isEmpty());
        assertNull(((BlockQuoteNode) node).getFooter());
    }

    @Test
    public void testAddTextAndWithFooterAreResolvedAndMapped()
    {
        ComponentContext context = this.newContext();
        LocalizedTextResolverService textResolver = context.getTextResolver();
        Location location = mock(Location.class);
        I18nTextValue resolvedText = new I18nTextValue(Map.of("DEFAULT", "Quote"));
        I18nTextValue resolvedFooter = new I18nTextValue(Map.of("DEFAULT", "- Author"));

        when(textResolver.apply(anyListOfI18nText(), eq(location))).thenReturn(List.of(resolvedText));
        when(textResolver.apply(any(I18nText.class), eq(location))).thenReturn(resolvedFooter);

        BlockQuoteImpl blockQuote = new BlockQuoteImpl(context);
        blockQuote.addText("Quote");
        blockQuote.withFooter("- Author");

        Node node = blockQuote.asRenderer()
                              .render(mock(RenderingProcessor.class), location, Optional.empty());

        assertEquals(1, ((BlockQuoteNode) node).getTexts()
                                               .size());
        assertSame(resolvedText, ((BlockQuoteNode) node).getTexts()
                                                        .get(0));
        assertSame(resolvedFooter, ((BlockQuoteNode) node).getFooter());
    }

    @Test
    public void testFieldsSurviveTemplating()
    {
        ComponentContext context = this.newContext();
        LocalizedTextResolverService textResolver = context.getTextResolver();
        Location location = mock(Location.class);
        when(textResolver.apply(anyListOfI18nText(), eq(location))).thenReturn(List.of(new I18nTextValue(Map.of("DEFAULT", "Quote"))));

        BlockQuoteImpl blockQuote = new BlockQuoteImpl(context);
        blockQuote.addText("Quote");
        blockQuote.withFooter("- Author");

        BlockQuoteImpl templated = (BlockQuoteImpl) blockQuote.asTemplateProvider()
                                                              .get();

        Node node = templated.asRenderer()
                             .render(mock(RenderingProcessor.class), location, Optional.empty());

        assertEquals(1, ((BlockQuoteNode) node).getTexts()
                                               .size());
    }

    @SuppressWarnings("unchecked")
    private static List<I18nText> anyListOfI18nText()
    {
        return any(List.class);
    }
}
