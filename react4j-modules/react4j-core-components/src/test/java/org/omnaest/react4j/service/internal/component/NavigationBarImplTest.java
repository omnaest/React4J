package org.omnaest.react4j.service.internal.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.i18n.I18nText;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.service.internal.nodes.NavigationBarNode;
import org.omnaest.react4j.service.internal.nodes.NavigationBarNode.Entry;
import org.omnaest.react4j.service.internal.nodes.i18n.I18nTextValue;
import org.omnaest.react4j.service.internal.service.LocalizedTextResolverService;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * Goal-1 contract-fidelity test: asserts the {@link NavigationBarImpl} builder API ({@code addEntry} with
 * text/link/linkedLocator/activeState/disabledState) maps onto the produced {@link NavigationBarNode} entries.
 *
 * @see NavigationBarImpl
 * @author omnaest
 */
public class NavigationBarImplTest
{
    private ComponentContext newContext()
    {
        ComponentContext context = mock(ComponentContext.class);
        when(context.getTextResolver()).thenReturn(mock(LocalizedTextResolverService.class));
        return context;
    }

    @Test
    public void testDefaultEntriesAreEmpty()
    {
        ComponentContext context = this.newContext();
        NavigationBarImpl navigationBar = new NavigationBarImpl(context);

        Node node = navigationBar.asRenderer()
                                 .render(mock(RenderingProcessor.class), mock(Location.class), Optional.empty());

        assertTrue(((NavigationBarNode) node).getEntries()
                                             .isEmpty());
    }

    @Test
    public void testAddEntryWithTextLinkActiveAndDisabledMapsToNode()
    {
        ComponentContext context = this.newContext();
        LocalizedTextResolverService textResolver = context.getTextResolver();
        Location location = mock(Location.class);
        I18nTextValue resolvedText = new I18nTextValue(Map.of("DEFAULT", "Home"));
        when(textResolver.apply(any(I18nText.class), eq(location))).thenReturn(resolvedText);

        NavigationBarImpl navigationBar = new NavigationBarImpl(context);
        navigationBar.addEntry(entry -> entry.withText("Home")
                                             .withLink("/home")
                                             .withActiveState(true)
                                             .withDisabledState(false));

        Node node = navigationBar.asRenderer()
                                 .render(mock(RenderingProcessor.class), location, Optional.empty());

        List<Entry> entries = ((NavigationBarNode) node).getEntries();
        assertEquals(1, entries.size());
        assertEquals("/home", entries.get(0)
                                     .getLink());
        assertEquals(resolvedText, entries.get(0)
                                          .getText());
        assertTrue(entries.get(0)
                          .isActive());
        assertFalse(entries.get(0)
                           .isDisabled());
    }

    @Test
    public void testAddEntryWithLinkedLocatorUsesComponentId()
    {
        ComponentContext context = this.newContext();
        NavigationBarImpl navigationBar = new NavigationBarImpl(context);

        UIComponent<?> linkedComponent = mock(UIComponent.class);
        when(linkedComponent.getId()).thenReturn("some-section");

        navigationBar.addEntry(entry -> entry.withLinked(linkedComponent));

        Node node = navigationBar.asRenderer()
                                 .render(mock(RenderingProcessor.class), mock(Location.class), Optional.empty());

        assertEquals("some-section", ((NavigationBarNode) node).getEntries()
                                                               .get(0)
                                                               .getLinkedId());
    }

    @Test
    public void testMultipleEntriesPreserveOrder()
    {
        ComponentContext context = this.newContext();
        NavigationBarImpl navigationBar = new NavigationBarImpl(context);

        navigationBar.addEntry(entry -> entry.withLink("/first"));
        navigationBar.addEntry(entry -> entry.withLink("/second"));

        Node node = navigationBar.asRenderer()
                                 .render(mock(RenderingProcessor.class), mock(Location.class), Optional.empty());

        List<Entry> entries = ((NavigationBarNode) node).getEntries();
        assertEquals(2, entries.size());
        assertEquals("/first", entries.get(0)
                                      .getLink());
        assertEquals("/second", entries.get(1)
                                       .getLink());
    }

    // ---- dropdowns --------------------------------------------------------------------------------------------------------------

    /** A context whose text resolver turns an {@link I18nText} into a node text carrying its default text, so texts are observable on the node */
    private ComponentContext newContextResolvingTexts()
    {
        ComponentContext context = this.newContext();
        when(context.getTextResolver()
                    .apply(any(I18nText.class), any(Location.class))).thenAnswer(
                                                                                 invocation -> new I18nTextValue(Map.of("DEFAULT", invocation.<I18nText>getArgument(0)
                                                                                                                                             .getDefaultText())));
        return context;
    }

    private NavigationBarNode render(NavigationBarImpl navigationBar)
    {
        return (NavigationBarNode) navigationBar.asRenderer()
                                                .render(mock(RenderingProcessor.class), mock(Location.class), Optional.empty());
    }

    private static String textOf(Entry entry)
    {
        return entry.getText()
                    .getLocaleToText()
                    .get("DEFAULT");
    }

    @Test
    public void testDropdownIsMappedToAnEntryWithItsItemsAndStaysBetweenItsNeighboursInInsertionOrder()
    {
        NavigationBarImpl navigationBar = new NavigationBarImpl(this.newContextResolvingTexts());
        UIComponent<?> linkedComponent = mock(UIComponent.class);
        when(linkedComponent.getId()).thenReturn("section-id");

        navigationBar.addEntry(entry -> entry.withText("First")
                                             .withLink("/first"));
        navigationBar.addDropdown(dropdown -> dropdown.withText("More")
                                                      .withActiveState(true)
                                                      .withDisabledState(false)
                                                      .addEntry(item -> item.withText("Reports")
                                                                            .withLink("/reports")
                                                                            .withActiveState(true))
                                                      .addEntry(item -> item.withText("Section")
                                                                            .withLinked(linkedComponent)
                                                                            .withDisabledState(true)));
        navigationBar.addEntry(entry -> entry.withText("Last")
                                             .withLink("/last"));

        List<Entry> entries = this.render(navigationBar)
                                  .getEntries();

        assertEquals(List.of("First", "More", "Last"), entries.stream()
                                                              .map(NavigationBarImplTest::textOf)
                                                              .collect(java.util.stream.Collectors.toList()),
                     "entries and dropdowns share one list in insertion order");

        Entry dropdown = entries.get(1);
        assertEquals(2, dropdown.getDropdownEntries()
                                .size());
        assertTrue(dropdown.isActive(), "active describes the toggle");
        assertFalse(dropdown.isDisabled(), "disabled describes the toggle");
        assertNull(dropdown.getLink(), "a dropdown toggle has no link");
        assertNull(dropdown.getLinkedId(), "a dropdown toggle has no linked id");

        Entry reports = dropdown.getDropdownEntries()
                                .get(0);
        assertEquals("Reports", textOf(reports));
        assertEquals("/reports", reports.getLink());
        assertNull(reports.getLinkedId());
        assertTrue(reports.isActive(), "an item is an entry: its active state behaves as on a top-level entry");
        assertNull(reports.getDropdownEntries(), "an item is a plain entry, a dropdown is one level deep");

        Entry section = dropdown.getDropdownEntries()
                                .get(1);
        assertEquals("Section", textOf(section));
        assertEquals("section-id", section.getLinkedId(), "an item linked to a component carries that component's id like a top-level entry");
        assertNull(section.getLink());
        assertTrue(section.isDisabled(), "an item is an entry: its disabled state behaves as on a top-level entry");
    }

    @Test
    public void testPlainEntriesCarryNoDropdownEntriesAndAnEmptyDropdownCarriesAnEmptyList()
    {
        NavigationBarImpl navigationBar = new NavigationBarImpl(this.newContextResolvingTexts());
        navigationBar.addEntry(entry -> entry.withText("Plain")
                                             .withLink("/plain"));
        navigationBar.addDropdown(dropdown -> dropdown.withText("Empty"));

        List<Entry> entries = this.render(navigationBar)
                                  .getEntries();

        assertNull(entries.get(0)
                          .getDropdownEntries(),
                   "null, not an empty list, marks a plain entry");
        assertNotNull(entries.get(1)
                             .getDropdownEntries(),
                      "a dropdown without items is still a dropdown");
        assertTrue(entries.get(1)
                          .getDropdownEntries()
                          .isEmpty());
    }

    /**
     * Serializes a node field by field as Spring's mapper does. The base node holds an {@code Optional}, which a plain mapper cannot write and the JDK 8
     * module (not on this module's test classpath) normally handles.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private ObjectMapper mapperWritingOptionalsAsTheirContent()
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
        return new ObjectMapper().registerModule(module);
    }

    @Test
    public void testNodeJsonCarriesDropdownEntriesAsNullOnPlainEntriesAndAsAnArrayOnDropdowns() throws Exception
    {
        NavigationBarImpl navigationBar = new NavigationBarImpl(this.newContextResolvingTexts());
        navigationBar.addEntry(entry -> entry.withText("Plain")
                                             .withLink("/plain"));
        navigationBar.addDropdown(dropdown -> dropdown.withText("More")
                                                      .addEntry(item -> item.withText("Reports")
                                                                            .withLink("/reports")));

        JsonNode json = this.mapperWritingOptionalsAsTheirContent()
                            .valueToTree(this.render(navigationBar));

        JsonNode plain = json.get("entries")
                             .get(0);
        assertTrue(plain.has("dropdownEntries"), "the field is always serialized, named dropdownEntries");
        assertTrue(plain.get("dropdownEntries")
                        .isNull(),
                   "null on a plain entry");

        JsonNode dropdown = json.get("entries")
                                .get(1);
        assertTrue(dropdown.get("dropdownEntries")
                           .isArray());
        assertEquals(1, dropdown.get("dropdownEntries")
                                .size());
        assertTrue(dropdown.get("link")
                           .isNull());
        assertTrue(dropdown.get("linkedId")
                           .isNull());
        assertEquals("/reports", dropdown.get("dropdownEntries")
                                         .get(0)
                                         .get("link")
                                         .asText());
        assertTrue(dropdown.get("dropdownEntries")
                           .get(0)
                           .get("dropdownEntries")
                           .isNull(),
                   "an item is a plain entry");
    }

    @Test
    public void testDropdownsSurviveTemplating()
    {
        NavigationBarImpl navigationBar = new NavigationBarImpl(this.newContextResolvingTexts());
        navigationBar.addDropdown(dropdown -> dropdown.withText("More")
                                                      .addEntry(item -> item.withText("Reports")
                                                                            .withLink("/reports")));

        NavigationBarImpl templated = (NavigationBarImpl) navigationBar.asTemplateProvider()
                                                                       .get();

        Entry dropdown = this.render(templated)
                             .getEntries()
                             .get(0);
        assertEquals(1, dropdown.getDropdownEntries()
                                .size());
        assertEquals("/reports", dropdown.getDropdownEntries()
                                         .get(0)
                                         .getLink());
    }

    @Test
    public void testEntriesSurviveTemplating()
    {
        ComponentContext context = this.newContext();
        NavigationBarImpl navigationBar = new NavigationBarImpl(context);
        navigationBar.addEntry(entry -> entry.withLink("/home"));

        NavigationBarImpl templated = (NavigationBarImpl) navigationBar.asTemplateProvider()
                                                                       .get();

        Node node = templated.asRenderer()
                             .render(mock(RenderingProcessor.class), mock(Location.class), Optional.empty());

        assertEquals(1, ((NavigationBarNode) node).getEntries()
                                                  .size());
    }
}
