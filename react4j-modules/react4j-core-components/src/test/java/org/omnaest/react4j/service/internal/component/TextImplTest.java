package org.omnaest.react4j.service.internal.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import org.omnaest.react4j.domain.Text;
import org.omnaest.react4j.domain.i18n.I18nText;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer.EventHandlerRegistrationSupport;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.service.internal.nodes.TextNode;
import org.omnaest.react4j.service.internal.nodes.i18n.I18nTextValue;
import org.omnaest.react4j.service.internal.service.LocalizedTextResolverService;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;

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

    /**
     * Pins the default of the style (plan-283 S1, policy criterion 2b): a text nobody styled serialises with no style value at all, so no renderer can see one.
     */
    @Test
    public void testTextWithoutAStyleSerialisesWithoutAStyleValue()
    {
        TextImpl text = new TextImpl(this.newContext());
        text.addText("Hi");

        TextNode node = this.render(text);
        JsonNode json = this.toJson(node);

        assertNull(node.getStyle());
        assertTrue(json.path("style")
                       .isMissingNode()
                   || json.path("style")
                          .isNull(),
                   json.toString());
    }

    /**
     * Pins the default of the emphasis (plan-284 S2, policy criterion 2b) BEFORE the field exists: a text nobody emphasised serialises with no emphasis value at
     * all, so no renderer can see one.
     */
    @Test
    public void testTextWithoutEmphasisSerialisesWithoutAnEmphasisValue()
    {
        TextImpl text = new TextImpl(this.newContext());
        text.addText("Hi");

        JsonNode json = this.toJson(this.render(text));

        assertTrue(json.path("emphasis")
                       .isMissingNode()
                   || json.path("emphasis")
                          .isNull(),
                   json.toString());
    }

    /**
     * The emphasis reaches the node in enum order whatever the order of the calls, as the names the client compares exactly.
     */
    @Test
    public void testWithEmphasisCarriesTheEmphasisToTheNodeInEnumOrder()
    {
        TextImpl text = new TextImpl(this.newContext());
        text.addText("Hi")
            .withEmphasis(Text.Emphasis.STRIKETHROUGH, Text.Emphasis.BOLD);

        TextNode node = this.render(text);
        JsonNode json = this.toJson(node);

        assertEquals(List.of(Text.Emphasis.BOLD, Text.Emphasis.STRIKETHROUGH), node.getEmphasis());
        assertEquals("[\"BOLD\",\"STRIKETHROUGH\"]", json.path("emphasis")
                                                         .toString());
    }

    @Test
    public void testWithEmphasisIsCumulativeAndIdempotentPerMemberAndIgnoresNull()
    {
        TextImpl text = new TextImpl(this.newContext());
        text.withEmphasis(Text.Emphasis.ITALIC)
            .withEmphasis(Text.Emphasis.ITALIC, Text.Emphasis.BOLD)
            .withEmphasis((Text.Emphasis[]) null)
            .withEmphasis(Text.Emphasis.BOLD, null)
            .withEmphasis();

        assertEquals(List.of(Text.Emphasis.BOLD, Text.Emphasis.ITALIC), this.render(text)
                                                                            .getEmphasis());
    }

    @Test
    public void testNoWithEmphasisCallLeavesTheNodeWithoutEmphasis()
    {
        TextImpl text = new TextImpl(this.newContext());
        text.addText("Hi")
            .withEmphasis();

        assertNull(this.render(text)
                       .getEmphasis());
    }

    @Test
    public void testEmphasisSurvivesTemplatingWithoutSharingStateWithTheTemplate()
    {
        TextImpl text = new TextImpl(this.newContext());
        text.addText("Hi")
            .withEmphasis(Text.Emphasis.ITALIC);

        TextImpl templated = (TextImpl) text.asTemplateProvider()
                                            .get();
        templated.withEmphasis(Text.Emphasis.BOLD);

        assertEquals(List.of(Text.Emphasis.BOLD, Text.Emphasis.ITALIC), this.render(templated)
                                                                            .getEmphasis());
        assertEquals(List.of(Text.Emphasis.ITALIC), this.render(text)
                                                        .getEmphasis());
    }

    @Test
    public void testEveryEmphasisMapsToItsSemanticElement()
    {
        for (Text.Emphasis emphasis : Text.Emphasis.values())
        {
            assertFalse(emphasis.toElementName()
                                .isBlank(),
                        emphasis.name());
        }
        assertEquals("strong", Text.Emphasis.BOLD.toElementName());
        assertEquals("em", Text.Emphasis.ITALIC.toElementName());
        assertEquals("del", Text.Emphasis.STRIKETHROUGH.toElementName());
    }

    @Test
    public void testWithStyleCarriesTheStyleToTheNodeAsItsName()
    {
        TextImpl text = new TextImpl(this.newContext());
        text.addText("Hi")
            .withStyle(Text.Style.MUTED);

        TextNode node = this.render(text);
        JsonNode json = this.toJson(node);

        assertEquals(Text.Style.MUTED, node.getStyle());
        assertEquals("MUTED", json.path("style")
                                  .asText(),
                     json.toString());
        assertEquals(1, node.getTexts()
                            .size());
    }

    @Test
    public void testWithStyleOfNullRemovesTheStyle()
    {
        TextImpl text = new TextImpl(this.newContext());
        text.withStyle(Text.Style.MUTED)
            .withStyle(null);

        assertNull(this.render(text)
                       .getStyle());
    }

    @Test
    public void testStyleSurvivesTemplating()
    {
        TextImpl text = new TextImpl(this.newContext());
        text.addText("Hi")
            .withStyle(Text.Style.MUTED);

        TextImpl templated = (TextImpl) text.asTemplateProvider()
                                            .get();

        assertEquals(Text.Style.MUTED, this.render(templated)
                                           .getStyle());
    }

    @Test
    public void testEveryStyleMapsToANonBlankThemeClassAndMutedToTheBootstrapSecondaryColourUtility()
    {
        for (Text.Style style : Text.Style.values())
        {
            assertFalse(style.toCssClass()
                             .isBlank(),
                        style.name());
        }
        assertEquals("text-body-secondary", Text.Style.MUTED.toCssClass());
    }

    private TextNode render(TextImpl text)
    {
        return (TextNode) text.asRenderer()
                              .render(mock(RenderingProcessor.class), mock(Location.class), Optional.empty());
    }

    /**
     * The JSON a node is serialised to, field by field as Spring's mapper does. The base node holds an {@code Optional}, which a plain mapper cannot write
     * and the JDK 8 module (not on this module's test classpath) normally handles, so a minimal serializer writes it as its content.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private JsonNode toJson(Object node)
    {
        SimpleModule module = new SimpleModule();
        module.addSerializer(Optional.class, new JsonSerializer<Optional>() {
            @Override
            public void serialize(Optional value, JsonGenerator generator, SerializerProvider provider) throws java.io.IOException
            {
                if (value.isPresent())
                {
                    provider.defaultSerializeValue(value.get(), generator);
                }
                else
                {
                    generator.writeNull();
                }
            }
        });
        return new ObjectMapper().registerModule(module)
                                 .valueToTree(node);
    }
}
