package org.omnaest.react4j.service.internal.service.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Autowired
    private MockMvc        mockMvc;

    @Autowired
    private ReactUIService reactUIService;

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
     * Kanban card 79ce393f (plan-284 AC-S2-1), replacing the plan-283 pin that kept a quote a plain paragraph "until CommonsMarkdown exposes it": a block quote
     * renders as one BLOCKQUOTE node whose elements are the quoted blocks, so the emphasis and the link inside the quote survive, and it carries no footer.
     */
    @Test
    public void testBlockQuoteRendersAsABlockQuoteNodeHoldingItsFormattedBlocks_card79ce393f() throws Exception
    {
        JsonNode ui = this.render("> q **b** [l](https://x)");

        List<JsonNode> blockQuotes = this.findAll(ui, "BLOCKQUOTE");
        assertEquals(1, blockQuotes.size(), ui.toString());
        JsonNode blockQuote = blockQuotes.get(0);
        assertTrue(blockQuote.path("footer")
                             .isMissingNode()
                   || blockQuote.path("footer")
                                .isNull(),
                   "no footer value: " + ui);

        List<JsonNode> quotedBlocks = this.elementsOf(blockQuote);
        assertEquals(1, quotedBlocks.size(), ui.toString());
        assertEquals("PARAGRAPH", quotedBlocks.get(0)
                                              .path("type")
                                              .asText(),
                     ui.toString());
        List<JsonNode> texts = this.findAll(quotedBlocks.get(0), "TEXT")
                                   .stream()
                                   .filter(text -> !String.join("", this.textsOf(text))
                                                          .isBlank())
                                   .collect(Collectors.toList());
        assertEquals(2, texts.size(), "the plain run and the bold run (the blank between bold run and link aside): " + ui);
        assertEquals(List.of("q"), this.textsOf(texts.get(0))
                                       .stream()
                                       .map(String::trim)
                                       .collect(Collectors.toList()));
        assertEquals(List.of(), MarkdownPipelineTestSupport.emphasisOf(texts.get(0)));
        assertEquals(List.of("b"), this.textsOf(texts.get(1)));
        assertEquals(List.of("BOLD"), MarkdownPipelineTestSupport.emphasisOf(texts.get(1)));
        List<JsonNode> ankers = this.findAll(quotedBlocks.get(0), "ANKER");
        assertEquals(1, ankers.size(), ui.toString());
        assertEquals("https://x", ankers.get(0)
                                        .path("link")
                                        .asText());
    }

    /**
     * Registers the given markdown as the ui and returns the node tree the frontend receives for it.
     */
    private JsonNode render(String markdown) throws Exception
    {
        return MarkdownPipelineTestSupport.render(this.mockMvc, this.reactUIService, markdown);
    }

    private List<JsonNode> findAll(JsonNode subtree, String type)
    {
        return MarkdownPipelineTestSupport.findAll(subtree, type);
    }

    private List<JsonNode> elementsOf(JsonNode node)
    {
        return MarkdownPipelineTestSupport.elementsOf(node);
    }

    private List<String> entryTexts(JsonNode listNode)
    {
        return MarkdownPipelineTestSupport.entryTexts(listNode);
    }

    private List<String> textsOf(JsonNode subtree)
    {
        return MarkdownPipelineTestSupport.textsOf(subtree);
    }

    private String shape(JsonNode node)
    {
        return MarkdownPipelineTestSupport.shape(node);
    }

}
