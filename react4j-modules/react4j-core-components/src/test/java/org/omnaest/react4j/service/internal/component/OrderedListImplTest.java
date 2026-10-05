package org.omnaest.react4j.service.internal.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.UIComponentFactory;
import org.omnaest.react4j.domain.i18n.I18nText;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.RenderableUIComponent;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.service.internal.nodes.OrderedListNode;
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
 * Contract-fidelity test for {@link OrderedListImpl} (plan-283 S1): the builder API ({@code addText}/{@code addEntry}/{@code addEntries}/
 * {@code withStartNumber}) maps onto the produced {@link OrderedListNode} and its JSON, preserving entry order, the way {@link UnsortedListImplTest} does for
 * the unsorted list.
 *
 * @see OrderedListImpl
 */
public class OrderedListImplTest
{
    private ComponentContext newContext()
    {
        ComponentContext context = mock(ComponentContext.class);
        when(context.getTextResolver()).thenReturn(mock(LocalizedTextResolverService.class));
        return context;
    }

    @Test
    public void testDefaultsHaveNoEntriesAndStartAtOne()
    {
        OrderedListImpl orderedList = new OrderedListImpl(this.newContext());

        OrderedListNode node = this.render(orderedList, mock(RenderingProcessor.class));

        assertTrue(node.getElements()
                       .isEmpty());
        assertEquals(1, node.getStartNumber());
    }

    @Test
    public void testWithStartNumberIsCarriedToTheNode()
    {
        OrderedListImpl orderedList = new OrderedListImpl(this.newContext());

        orderedList.withStartNumber(3);

        assertEquals(3, this.render(orderedList, mock(RenderingProcessor.class)).getStartNumber());
    }

    @Test
    public void testNodeSerialisesAsOrderedListWithItsElementsAndStartNumber() throws Exception
    {
        OrderedListImpl orderedList = new OrderedListImpl(this.newContext());
        orderedList.withStartNumber(7)
                   .addEntry(mock(UIComponent.class));

        RenderingProcessor renderingProcessor = mock(RenderingProcessor.class);
        when(renderingProcessor.process(any(UIComponent.class), any(Location.class))).thenAnswer(invocation -> new TextNode().setTexts(List.of()));
        JsonNode json = this.toJson(this.render(orderedList, renderingProcessor));

        assertEquals("ORDEREDLIST", json.get("type")
                                        .asText());
        assertEquals(7, json.get("startNumber")
                            .asInt());
        assertEquals(1, json.get("elements")
                            .size());
    }

    @Test
    public void testAddEntryAndAddEntriesPreserveTheOrderOfTheEntries()
    {
        OrderedListImpl orderedList = new OrderedListImpl(this.newContext());
        UIComponent<?> first = mock(UIComponent.class);
        UIComponent<?> second = mock(UIComponent.class);
        UIComponent<?> third = mock(UIComponent.class);

        orderedList.addEntry(first)
                   .addEntries(Arrays.asList(second, third));

        Map<UIComponent<?>, Node> nodePerComponent = new IdentityHashMap<>();
        OrderedListNode node = this.render(orderedList, this.processorAnsweringOneNodePerComponent(nodePerComponent));

        assertEquals(3, node.getElements()
                            .size());
        assertSame(nodePerComponent.get(first), node.getElements()
                                                    .get(0));
        assertSame(nodePerComponent.get(second), node.getElements()
                                                     .get(1));
        assertSame(nodePerComponent.get(third), node.getElements()
                                                    .get(2));
    }

    @Test
    public void testAddEntriesOfNullAddsNothing()
    {
        OrderedListImpl orderedList = new OrderedListImpl(this.newContext());

        orderedList.addEntries(null);

        assertTrue(this.render(orderedList, mock(RenderingProcessor.class)).getElements()
                       .isEmpty());
    }

    @Test
    public void testAddTextAddsOneTextEntryCarryingTheText()
    {
        ComponentContext context = this.newContext();
        when(context.getTextResolver()
                    .apply(any(I18nText.class), any(Location.class))).thenAnswer(
                                                                                 invocation -> new I18nTextValue(Map.of("DEFAULT",
                                                                                                                        ((I18nText) invocation.getArgument(0)).getDefaultText())));
        UIComponentFactory factory = mock(UIComponentFactory.class);
        when(factory.newText()).thenAnswer(invocation -> new TextImpl(context));
        when(context.getUiComponentFactory()).thenReturn(factory);
        OrderedListImpl orderedList = new OrderedListImpl(context);

        orderedList.addText("Item");

        List<UIComponent<?>> entries = orderedList.asRenderer()
                                                  .getSubComponents(Location.of("root"))
                                                  .map(parentLocationAndComponent -> parentLocationAndComponent.getComponent())
                                                  .collect(Collectors.toList());
        assertEquals(1, entries.size());
        TextNode entryNode = (TextNode) ((RenderableUIComponent<?>) entries.get(0)).asRenderer()
                                                                                   .render(mock(RenderingProcessor.class), Location.of("root"), Optional.empty());
        assertEquals(List.of("Item"), entryNode.getTexts()
                                               .stream()
                                               .map(text -> text.getLocaleToText()
                                                                .get("DEFAULT"))
                                               .collect(Collectors.toList()));
    }

    @Test
    public void testEntriesAndStartNumberSurviveTemplating()
    {
        OrderedListImpl orderedList = new OrderedListImpl(this.newContext());
        orderedList.addEntry(mock(UIComponent.class))
                   .withStartNumber(5);

        OrderedListImpl templated = (OrderedListImpl) orderedList.asTemplateProvider()
                                                                 .get();
        OrderedListNode node = this.render(templated, this.processorAnsweringOneNodePerComponent(new IdentityHashMap<>()));

        assertEquals(1, node.getElements()
                            .size());
        assertEquals(5, node.getStartNumber());
    }

    private OrderedListNode render(OrderedListImpl orderedList, RenderingProcessor renderingProcessor)
    {
        return (OrderedListNode) orderedList.asRenderer()
                                            .render(renderingProcessor, Location.of("root"), Optional.empty());
    }

    private RenderingProcessor processorAnsweringOneNodePerComponent(Map<UIComponent<?>, Node> nodePerComponent)
    {
        RenderingProcessor renderingProcessor = mock(RenderingProcessor.class);
        when(renderingProcessor.process(any(UIComponent.class), any(Location.class))).thenAnswer(invocation ->
        {
            Node node = mock(Node.class);
            nodePerComponent.put(invocation.getArgument(0), node);
            return node;
        });
        return renderingProcessor;
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
