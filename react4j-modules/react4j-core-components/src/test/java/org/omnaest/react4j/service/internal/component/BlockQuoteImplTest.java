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
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.i18n.I18nText;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.service.internal.nodes.BlockQuoteNode;
import org.omnaest.react4j.service.internal.nodes.i18n.I18nTextValue;
import org.omnaest.react4j.service.internal.service.LocalizedTextResolverService;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;

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

    /**
     * Pins the default of the children (plan-284 S2, policy criterion 2b) BEFORE the field exists: a quote nobody added a component to serialises with no
     * elements value at all, so no renderer can see one.
     */
    @Test
    public void testQuoteWithoutChildComponentsSerialisesWithoutAnElementsValue()
    {
        ComponentContext context = this.newContext();
        Location location = mock(Location.class);
        when(context.getTextResolver()
                    .apply(anyListOfI18nText(), eq(location))).thenReturn(List.of());
        BlockQuoteImpl blockQuote = new BlockQuoteImpl(context);
        blockQuote.addText("Quote");

        Node node = blockQuote.asRenderer()
                              .render(mock(RenderingProcessor.class), location, Optional.empty());
        JsonNode json = this.toJson(node);

        assertTrue(json.path("elements")
                       .isMissingNode()
                   || json.path("elements")
                          .isNull(),
                   json.toString());
    }

    /**
     * The real text resolver answers an empty text for an unset (null) one, which would make an unset footer indistinguishable from an empty footer; the
     * node keeps it null (plan-284: that is what lets the renderers omit the footer element).
     */
    @Test
    public void testUnsetFooterStaysNullEvenWhenTheResolverWouldAnswerAnEmptyText()
    {
        ComponentContext context = this.newContext();
        LocalizedTextResolverService textResolver = context.getTextResolver();
        Location location = mock(Location.class);
        when(textResolver.apply(anyListOfI18nText(), eq(location))).thenReturn(List.of());
        when(textResolver.apply((I18nText) any(), eq(location))).thenReturn(new I18nTextValue(Map.of("DEFAULT", "")));
        BlockQuoteImpl blockQuote = new BlockQuoteImpl(context);
        blockQuote.addText("Quote");

        BlockQuoteNode node = (BlockQuoteNode) blockQuote.asRenderer()
                                                         .render(mock(RenderingProcessor.class), location, Optional.empty());

        assertNull(node.getFooter());
    }

    /**
     * Child components are rendered in order through the rendering processor, appear as the elements of the node and are reachable as sub components (so
     * traversals such as the event handler registration find them), like for the sibling containers.
     */
    @Test
    public void testAddedComponentsAreRenderedInOrderAndReachableAsSubComponents()
    {
        ComponentContext context = this.newContext();
        Location location = mock(Location.class);
        when(context.getTextResolver()
                    .apply(anyListOfI18nText(), eq(location))).thenReturn(List.of());
        UIComponent<?> first = mock(UIComponent.class);
        UIComponent<?> second = mock(UIComponent.class);
        Node firstNode = mock(Node.class);
        Node secondNode = mock(Node.class);
        RenderingProcessor renderingProcessor = mock(RenderingProcessor.class);
        when(renderingProcessor.process(eq(first), any())).thenReturn(firstNode);
        when(renderingProcessor.process(eq(second), any())).thenReturn(secondNode);

        BlockQuoteImpl blockQuote = new BlockQuoteImpl(context);
        blockQuote.addComponent(first)
                  .addComponents(List.of(second));
        BlockQuoteNode node = (BlockQuoteNode) blockQuote.asRenderer()
                                                         .render(renderingProcessor, location, Optional.empty());

        assertEquals(List.of(firstNode, secondNode), node.getElements());
        assertEquals(List.of(first, second), blockQuote.asRenderer()
                                                       .getSubComponents(location)
                                                       .map(UIComponentRenderer.ParentLocationAndComponent::getComponent)
                                                       .collect(Collectors.toList()));
    }

    @Test
    public void testAddComponentsOfNullIsIgnored()
    {
        ComponentContext context = this.newContext();
        Location location = mock(Location.class);
        when(context.getTextResolver()
                    .apply(anyListOfI18nText(), eq(location))).thenReturn(List.of());
        BlockQuoteImpl blockQuote = new BlockQuoteImpl(context);

        blockQuote.addComponents(null);
        BlockQuoteNode node = (BlockQuoteNode) blockQuote.asRenderer()
                                                         .render(mock(RenderingProcessor.class), location, Optional.empty());

        assertNull(node.getElements());
    }

    @Test
    public void testChildComponentsSurviveTemplatingWithoutSharingTheListWithTheTemplate()
    {
        ComponentContext context = this.newContext();
        Location location = mock(Location.class);
        when(context.getTextResolver()
                    .apply(anyListOfI18nText(), eq(location))).thenReturn(List.of());
        UIComponent<?> child = mock(UIComponent.class);
        BlockQuoteImpl blockQuote = new BlockQuoteImpl(context);
        blockQuote.addComponent(child);

        BlockQuoteImpl templated = (BlockQuoteImpl) blockQuote.asTemplateProvider()
                                                              .get();
        templated.addComponent(mock(UIComponent.class));

        assertEquals(2, templated.asRenderer()
                                 .getSubComponents(location)
                                 .count());
        assertEquals(1, blockQuote.asRenderer()
                                  .getSubComponents(location)
                                  .count());
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

    /**
     * The JSON a node is serialised to, field by field as Spring's mapper does. The base node holds an {@code Optional}, which a plain mapper cannot write,
     * so a minimal serializer writes it as its content.
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

    @SuppressWarnings("unchecked")
    private static List<I18nText> anyListOfI18nText()
    {
        return any(List.class);
    }
}
