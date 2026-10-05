package org.omnaest.react4j.service.internal.service.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.omnaest.react4j.EnableReactUI;
import org.omnaest.react4j.service.ReactUIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * plan-283 S1: a markdown list element kind that {@link MarkdownServiceImpl} does not map is dropped without a trace. CommonsMarkdown parses
 * {@code 1. one} into an ordered list and flattens a nested list into a sibling element right after its parent item; the interpreter used to have no branch
 * for the first and kept only paragraphs of the second, so the content never reached the frontend.
 * <p>
 * The markdown goes through the real ui pipeline (no mocked collaborator) and every assertion is anchored on a node found in the rendered tree, so a text
 * that merely occurs somewhere on the page cannot satisfy it.
 *
 * @author omnaest
 */
@SpringBootTest(classes = MarkdownListRenderingTest.TestApplication.class, webEnvironment = WebEnvironment.MOCK)
@AutoConfigureMockMvc
public class MarkdownListRenderingTest
{
    @SpringBootApplication
    @EnableReactUI
    public static class TestApplication
    {
    }

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private MockMvc                   mockMvc;

    @Autowired
    private ReactUIService            reactUIService;

    @Test
    public void testOrderedListIsRenderedAsOneOrderedListNodeWithItsEntriesInOrder() throws Exception
    {
        JsonNode ui = this.render("intro\n\n1. one\n2. two\n3. three\n");

        assertTrue(this.findAll(ui, "PARAGRAPH").stream().anyMatch(paragraph -> List.of("intro").equals(this.textsOf(paragraph))),
                   "control: the pipeline ran and kept the paragraph before the list: " + ui);
        List<JsonNode> orderedLists = this.findAll(ui, "ORDEREDLIST");
        assertEquals(1, orderedLists.size(), ui.toString());
        JsonNode orderedList = orderedLists.get(0);
        assertEquals(1, orderedList.get("startNumber")
                                   .asInt());
        assertEquals(List.of("one", "two", "three"), this.entryTexts(orderedList), ui.toString());
    }

    @Test
    public void testOrderedListStartingAtThreeCarriesItsStartNumber() throws Exception
    {
        JsonNode ui = this.render("3. three\n4. four\n");

        List<JsonNode> orderedLists = this.findAll(ui, "ORDEREDLIST");
        assertEquals(1, orderedLists.size(), ui.toString());
        assertEquals(3, orderedLists.get(0)
                                    .get("startNumber")
                                    .asInt());
        assertEquals(List.of("three", "four"), this.entryTexts(orderedLists.get(0)), ui.toString());
    }

    @Test
    public void testNestedUnorderedListIsNestedIntoTheItemItFollows() throws Exception
    {
        JsonNode ui = this.render("- a\n  - b\n- c\n");

        List<JsonNode> unorderedLists = this.findAll(ui, "UNORDEREDLIST");
        assertEquals(2, unorderedLists.size(), "the outer and the nested list: " + ui);
        JsonNode outer = unorderedLists.get(0);
        List<JsonNode> entries = this.elementsOf(outer);
        assertEquals(2, entries.size(), "the nested list must not become an entry of its own: " + ui);

        JsonNode firstEntry = entries.get(0);
        assertEquals(List.of("a", "b"), this.textsOf(firstEntry), "the first entry carries its own text and the nested one: " + ui);
        List<JsonNode> nestedLists = this.findAll(firstEntry, "UNORDEREDLIST");
        assertEquals(1, nestedLists.size(), ui.toString());
        assertEquals(List.of("b"), this.entryTexts(nestedLists.get(0)));

        JsonNode secondEntry = entries.get(1);
        assertEquals(List.of("c"), this.textsOf(secondEntry));
        assertTrue(this.findAll(secondEntry, "UNORDEREDLIST")
                       .isEmpty());
    }

    @Test
    public void testNestedOrderedListInsideAnUnorderedListIsNestedIntoTheItemItFollows() throws Exception
    {
        JsonNode ui = this.render("- a\n  1. b\n  2. c\n- d\n");

        List<JsonNode> unorderedLists = this.findAll(ui, "UNORDEREDLIST");
        assertEquals(1, unorderedLists.size(), ui.toString());
        List<JsonNode> entries = this.elementsOf(unorderedLists.get(0));
        assertEquals(2, entries.size(), ui.toString());

        List<JsonNode> nestedOrderedLists = this.findAll(entries.get(0), "ORDEREDLIST");
        assertEquals(1, nestedOrderedLists.size(), ui.toString());
        assertEquals(List.of("b", "c"), this.entryTexts(nestedOrderedLists.get(0)));
        assertEquals(List.of("a", "b", "c"), this.textsOf(entries.get(0)));
        assertEquals(List.of("d"), this.textsOf(entries.get(1)));
    }

    @Test
    public void testNestedUnorderedListInsideAnOrderedListIsNestedIntoTheItemItFollows() throws Exception
    {
        JsonNode ui = this.render("1. a\n   - b\n2. c\n");

        List<JsonNode> orderedLists = this.findAll(ui, "ORDEREDLIST");
        assertEquals(1, orderedLists.size(), ui.toString());
        List<JsonNode> entries = this.elementsOf(orderedLists.get(0));
        assertEquals(2, entries.size(), ui.toString());

        List<JsonNode> nestedLists = this.findAll(entries.get(0), "UNORDEREDLIST");
        assertEquals(1, nestedLists.size(), ui.toString());
        assertEquals(List.of("b"), this.entryTexts(nestedLists.get(0)));
        assertEquals(List.of("c"), this.textsOf(entries.get(1)));
    }

    @Test
    public void testNestedListWithoutAPrecedingItemBecomesAnEntryOfItsOwn() throws Exception
    {
        // an empty first item makes the nested list the first element CommonsMarkdown hands over, with no item before it to nest into
        JsonNode ui = this.render("-\n  - b\n");

        List<JsonNode> unorderedLists = this.findAll(ui, "UNORDEREDLIST");
        assertEquals(2, unorderedLists.size(), ui.toString());
        List<JsonNode> outerEntries = this.elementsOf(unorderedLists.get(0));
        assertEquals(1, outerEntries.size(), "the nested list is the one entry of the outer list: " + ui);
        assertEquals("UNORDEREDLIST", outerEntries.get(0)
                                                  .get("type")
                                                  .asText(),
                     ui.toString());
        assertEquals(List.of("b"), this.textsOf(outerEntries.get(0)), "the text of the nested list must survive: " + ui);
    }

    /**
     * Pinned BEFORE the list mapper changed (AC-S1-3): a flat list is node-identical to what it always rendered as.
     */
    @Test
    public void testFlatUnorderedListRendersAsBeforeWithOneParagraphPerEntry() throws Exception
    {
        JsonNode ui = this.render("- x\n- y\n");

        List<JsonNode> unorderedLists = this.findAll(ui, "UNORDEREDLIST");
        assertEquals(1, unorderedLists.size(), ui.toString());
        assertEquals("UNORDEREDLIST{bullets=true}[PARAGRAPH[COMPOSITE[TEXT(x)]],PARAGRAPH[COMPOSITE[TEXT(y)]]]", this.shape(unorderedLists.get(0)), ui.toString());
    }

    /**
     * Pinned BEFORE the list mapper changed: the leading pipe convention of the unordered list mapper (no bullets, the pipe removed) is kept.
     */
    @Test
    public void testLeadingPipeConventionOfTheUnorderedListIsKept() throws Exception
    {
        JsonNode ui = this.render("- |x\n- |y\n");

        List<JsonNode> unorderedLists = this.findAll(ui, "UNORDEREDLIST");
        assertEquals(1, unorderedLists.size(), ui.toString());
        assertEquals("UNORDEREDLIST{bullets=false}[PARAGRAPH[COMPOSITE[TEXT(x)]],PARAGRAPH[COMPOSITE[TEXT(y)]]]", this.shape(unorderedLists.get(0)), ui.toString());
    }

    /**
     * Pins today's blockquote behaviour (AC-S1-6): CommonsMarkdown has no block quote element, so {@code > q} arrives as a plain paragraph and renders as one.
     * The quote styling is lost, which is a separate finding (kanban, filed by the orchestrator) that needs a CommonsMarkdown change. When that lands this
     * test has to change deliberately.
     */
    @Test
    public void testBlockQuoteRendersAsAPlainParagraphUntilCommonsMarkdownExposesIt() throws Exception
    {
        JsonNode ui = this.render("> q");

        assertTrue(this.findAll(ui, "BLOCKQUOTE")
                       .isEmpty(),
                   ui.toString());
        List<JsonNode> paragraphs = this.findAll(ui, "PARAGRAPH");
        assertEquals(1, paragraphs.size(), ui.toString());
        assertEquals("PARAGRAPH[COMPOSITE[TEXT(q)]]", this.shape(paragraphs.get(0)), ui.toString());
    }

    /**
     * Registers the given markdown as the ui and returns the node tree the frontend receives for it.
     */
    private JsonNode render(String markdown) throws Exception
    {
        this.reactUIService.createDefaultRoot(reactUI -> reactUI.addNewComponent(factory -> factory.newComposite()
                                                                                                   .addComponents(factory.newMarkdown()
                                                                                                                         .texts()
                                                                                                                         .from(markdown))));
        String json = this.mockMvc.perform(get("/ui"))
                                  .andExpect(status().isOk())
                                  .andReturn()
                                  .getResponse()
                                  .getContentAsString();
        return OBJECT_MAPPER.readTree(json);
    }

    /**
     * All nodes of the given type within the subtree, the subtree root included, parents before their descendants. The walk is field name agnostic.
     */
    private List<JsonNode> findAll(JsonNode subtree, String type)
    {
        List<JsonNode> found = new ArrayList<>();
        if (subtree.isObject() && subtree.hasNonNull("type") && type.equals(subtree.get("type")
                                                                                   .asText()))
        {
            found.add(subtree);
        }
        subtree.elements()
               .forEachRemaining(child -> found.addAll(this.findAll(child, type)));
        return found;
    }

    private List<JsonNode> elementsOf(JsonNode node)
    {
        List<JsonNode> elements = new ArrayList<>();
        node.get("elements")
            .elements()
            .forEachRemaining(elements::add);
        return elements;
    }

    /**
     * The texts of every entry of a list node, one string per entry (the texts of the entry joined by a blank).
     */
    private List<String> entryTexts(JsonNode listNode)
    {
        return this.elementsOf(listNode)
                   .stream()
                   .map(entry -> String.join(" ", this.textsOf(entry)))
                   .collect(Collectors.toList());
    }

    /**
     * Every text value within the subtree, in document order.
     */
    private List<String> textsOf(JsonNode subtree)
    {
        List<String> texts = new ArrayList<>();
        if (subtree.isObject() && "TEXT".equals(subtree.path("type")
                                                       .asText()))
        {
            subtree.get("texts")
                   .elements()
                   .forEachRemaining(text -> texts.add(text.elements()
                                                           .next()
                                                           .asText()));
        }
        else
        {
            subtree.elements()
                   .forEachRemaining(child -> texts.addAll(this.textsOf(child)));
        }
        return texts;
    }

    /**
     * A compact structural projection of a node that ignores the generated ids and targets: {@code TYPE{flag}[child,child]} and {@code TEXT(value)}.
     */
    private String shape(JsonNode node)
    {
        String type = node.path("type")
                          .asText();
        if ("TEXT".equals(type))
        {
            return "TEXT(" + String.join("|", this.textsOf(node)) + ")";
        }
        StringBuilder shape = new StringBuilder(type);
        if ("UNORDEREDLIST".equals(type))
        {
            shape.append("{bullets=")
                 .append(node.path("enableBulletPoints")
                             .asBoolean())
                 .append("}");
        }
        if ("ORDEREDLIST".equals(type))
        {
            shape.append("{start=")
                 .append(node.path("startNumber")
                             .asInt())
                 .append("}");
        }
        if (node.path("elements")
                .isArray())
        {
            Iterator<JsonNode> children = node.get("elements")
                                              .elements();
            List<String> childShapes = new ArrayList<>();
            children.forEachRemaining(child -> childShapes.add(this.shape(child)));
            shape.append("[")
                 .append(String.join(",", childShapes))
                 .append("]");
        }
        return shape.toString();
    }

}
