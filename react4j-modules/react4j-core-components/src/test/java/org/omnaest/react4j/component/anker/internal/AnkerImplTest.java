package org.omnaest.react4j.component.anker.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.omnaest.react4j.component.anker.internal.renderer.node.AnkerNode;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.i18n.I18nText;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.service.internal.component.ComponentContext;
import org.omnaest.react4j.service.internal.nodes.i18n.I18nTextValue;
import org.omnaest.react4j.service.internal.service.LocalizedTextResolverService;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * Contract test of the {@link AnkerImpl} builder API: what its text, title, link and page become on the produced {@link AnkerNode}, and (plan-286 S1) how the
 * child components that carry a formatted label are rendered, located and templated.
 *
 * @see AnkerImpl
 */
public class AnkerImplTest
{
    private static final Location LOCATION = Location.of("root");

    private ComponentContext newContext()
    {
        ComponentContext context = mock(ComponentContext.class);
        LocalizedTextResolverService textResolver = mock(LocalizedTextResolverService.class);
        when(textResolver.apply(org.mockito.ArgumentMatchers.<I18nText>any(), eq(LOCATION))).thenAnswer(invocation ->
        {
            I18nText text = invocation.getArgument(0);
            return new I18nTextValue(Map.of("DEFAULT", text == null ? "" : text.getDefaultText()));
        });
        when(context.getTextResolver()).thenReturn(textResolver);
        return context;
    }

    private AnkerNode render(AnkerImpl anker)
    {
        return (AnkerNode) anker.asRenderer()
                                .render(mock(RenderingProcessor.class), LOCATION, Optional.empty());
    }

    /**
     * Pinned BEFORE the children existed (plan-261 policy criterion 2a/2b, green on the old code): an anker with text, title, link and no children produces
     * exactly this node, and the node has no elements value at all, so no renderer can see one.
     */
    @Test
    public void testAnkerWithoutChildrenProducesTheNodeItAlwaysDid()
    {
        AnkerImpl anker = new AnkerImpl(this.newContext());
        anker.withText("label")
             .withTitle("tip")
             .withLink("https://example.org/a");

        AnkerNode node = this.render(anker);
        JsonNode json = this.toJson(node);

        assertEquals("ANKER", node.getType());
        assertEquals(Map.of("DEFAULT", "label"), node.getText()
                                                     .getLocaleToText());
        assertEquals(Map.of("DEFAULT", "tip"), node.getTitle()
                                                   .getLocaleToText());
        assertEquals("https://example.org/a", node.getLink());
        assertEquals(AnkerNode.Page.BLANK, node.getPage());
        assertTrue(json.path("elements")
                       .isMissingNode()
                   || json.path("elements")
                          .isNull(),
                   json.toString());
    }

    @Test
    public void testAnkerOpeningOnTheSamePageHasTheSelfPage()
    {
        AnkerImpl anker = new AnkerImpl(this.newContext());
        anker.withLocator("section");

        AnkerNode node = this.render(anker);

        assertEquals("#section", node.getLink());
        assertEquals(AnkerNode.Page.SELF, node.getPage());
    }

    @Test
    public void testAnkerWithoutChildrenHasNoSubComponents()
    {
        AnkerImpl anker = new AnkerImpl(this.newContext());

        assertEquals(0, anker.asRenderer()
                             .getSubComponents(LOCATION)
                             .count());
    }

    @Test
    public void testTextTitleAndLinkSurviveTemplating()
    {
        AnkerImpl anker = new AnkerImpl(this.newContext());
        anker.withText("label")
             .withTitle("tip")
             .withLink("https://example.org/a");

        AnkerNode node = this.render((AnkerImpl) anker.asTemplateProvider()
                                                      .get());

        assertEquals(Map.of("DEFAULT", "label"), node.getText()
                                                     .getLocaleToText());
        assertEquals(Map.of("DEFAULT", "tip"), node.getTitle()
                                                   .getLocaleToText());
        assertEquals("https://example.org/a", node.getLink());
    }

    /**
     * The children are rendered in order through the rendering processor, each at the indexed child location of the anker's own, they are the elements of the
     * node next to the text, and they are reachable as sub components at the same locations (so traversals such as the event handler registration find
     * them), exactly like for the sibling containers.
     */
    @Test
    public void testAddedComponentsAreRenderedInOrderAtIndexedLocationsAndReachableAsSubComponents()
    {
        UIComponent<?> first = mock(UIComponent.class);
        UIComponent<?> second = mock(UIComponent.class);
        Node firstNode = mock(Node.class);
        Node secondNode = mock(Node.class);
        RenderingProcessor renderingProcessor = mock(RenderingProcessor.class);
        when(renderingProcessor.process(eq(first), any())).thenReturn(firstNode);
        when(renderingProcessor.process(eq(second), any())).thenReturn(secondNode);
        AnkerImpl anker = new AnkerImpl(this.newContext());
        anker.withText("label")
             .withLink("https://example.org/a");

        anker.addComponent(first)
             .addComponents(List.of(second));
        AnkerNode node = (AnkerNode) anker.asRenderer()
                                          .render(renderingProcessor, LOCATION, Optional.empty());

        assertEquals(List.of(firstNode, secondNode), node.getElements());
        assertEquals(Map.of("DEFAULT", "label"), node.getText()
                                                     .getLocaleToText());
        ArgumentCaptor<Location> childLocations = ArgumentCaptor.forClass(Location.class);
        verify(renderingProcessor).process(eq(first), childLocations.capture());
        verify(renderingProcessor).process(eq(second), childLocations.capture());
        assertEquals(List.of(List.of("root", "component0"), List.of("root", "component1")), childLocations.getAllValues()
                                                                                                          .stream()
                                                                                                          .map(Location::get)
                                                                                                          .collect(Collectors.toList()));
        List<UIComponentRenderer.ParentLocationAndComponent> subComponents = anker.asRenderer()
                                                                                  .getSubComponents(LOCATION)
                                                                                  .collect(Collectors.toList());
        assertEquals(List.of(first, second), subComponents.stream()
                                                          .map(UIComponentRenderer.ParentLocationAndComponent::getComponent)
                                                          .collect(Collectors.toList()));
        assertEquals(List.of(List.of("root", "component0"), List.of("root", "component1")), subComponents.stream()
                                                                                                         .map(UIComponentRenderer.ParentLocationAndComponent::getParentLocation)
                                                                                                         .map(Location::get)
                                                                                                         .collect(Collectors.toList()));
    }

    @Test
    public void testANullListAndANullMemberAreIgnored()
    {
        UIComponent<?> child = mock(UIComponent.class);
        Node childNode = mock(Node.class);
        RenderingProcessor renderingProcessor = mock(RenderingProcessor.class);
        when(renderingProcessor.process(eq(child), any())).thenReturn(childNode);
        AnkerImpl anker = new AnkerImpl(this.newContext());

        anker.addComponents(null)
             .addComponent(null)
             .addComponents(Arrays.asList(null, child, null));
        AnkerNode node = (AnkerNode) anker.asRenderer()
                                          .render(renderingProcessor, LOCATION, Optional.empty());

        assertEquals(List.of(childNode), node.getElements());
        assertEquals(1, anker.asRenderer()
                             .getSubComponents(LOCATION)
                             .count());
    }

    @Test
    public void testAnAnkerThatOnlyHadANullListAddedStillHasNoElementsValue()
    {
        AnkerImpl anker = new AnkerImpl(this.newContext());
        anker.withText("label");

        anker.addComponents(null);
        AnkerNode node = this.render(anker);

        assertNull(node.getElements());
    }

    /**
     * The template provider hands out a copy that has the children but does not share the list with the original, so a component added to a templated
     * instance is not added to the template (the builder of the text is shared, a list of children must not be).
     */
    @Test
    public void testChildrenSurviveTemplatingWithoutSharingTheListWithTheTemplate()
    {
        AnkerImpl anker = new AnkerImpl(this.newContext());
        anker.addComponent(mock(UIComponent.class));

        AnkerImpl templated = (AnkerImpl) anker.asTemplateProvider()
                                               .get();
        templated.addComponent(mock(UIComponent.class));

        assertEquals(2, templated.asRenderer()
                                 .getSubComponents(LOCATION)
                                 .count());
        assertEquals(1, anker.asRenderer()
                             .getSubComponents(LOCATION)
                             .count());
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
}
