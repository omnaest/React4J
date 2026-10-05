package org.omnaest.react4j.service.internal.service.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.omnaest.react4j.service.internal.service.internal.MarkdownPipelineTestSupport.elementsOf;
import static org.omnaest.react4j.service.internal.service.internal.MarkdownPipelineTestSupport.emphasisOf;
import static org.omnaest.react4j.service.internal.service.internal.MarkdownPipelineTestSupport.findAll;
import static org.omnaest.react4j.service.internal.service.internal.MarkdownPipelineTestSupport.shape;
import static org.omnaest.react4j.service.internal.service.internal.MarkdownPipelineTestSupport.stringValuesOf;
import static org.omnaest.react4j.service.internal.service.internal.MarkdownPipelineTestSupport.textsOf;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.omnaest.react4j.service.ReactUIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * plan-284 S2: the markdown element kinds {@link MarkdownServiceImpl} used to drop or flatten without a trace (block quote, list item children other than
 * paragraphs and lists, inline emphasis, thematic break, task list marker), asserted through the real ui pipeline on nodes found in the rendered tree.
 *
 * @see MarkdownListRenderingTest
 * @see MarkdownCompletenessGuardTest
 * @author omnaest
 */
@SpringBootTest(classes = MarkdownListRenderingTest.TestApplication.class, webEnvironment = WebEnvironment.MOCK)
@AutoConfigureMockMvc
public class MarkdownBlockAndEmphasisRenderingTest
{
    private static final String UNCHECKED_GLYPH = "\u2610";
    private static final String CHECKED_GLYPH   = "\u2611";

    @Autowired
    private MockMvc             mockMvc;

    @Autowired
    private ReactUIService      reactUIService;

    private JsonNode render(String markdown) throws Exception
    {
        return MarkdownPipelineTestSupport.render(this.mockMvc, this.reactUIService, markdown);
    }

    /**
     * The TEXT node whose single text value is the given one, asserted to be the only one of its value.
     */
    private JsonNode textNodeOf(JsonNode ui, String value)
    {
        List<JsonNode> matches = findAll(ui, "TEXT").stream()
                                                    .filter(text -> List.of(value)
                                                                        .equals(textsOf(text)))
                                                    .collect(Collectors.toList());
        assertEquals(1, matches.size(), "exactly one TEXT node '" + value + "': " + ui);
        return matches.get(0);
    }

    @Test
    public void testEveryInlineEmphasisKindCarriesItsEmphasisValueOnTheTextNode() throws Exception
    {
        JsonNode ui = this.render("~~s~~ *i* **b** ***bi***");

        assertEquals(List.of("STRIKETHROUGH"), emphasisOf(this.textNodeOf(ui, "s")), ui.toString());
        assertEquals(List.of("ITALIC"), emphasisOf(this.textNodeOf(ui, "i")), ui.toString());
        assertEquals(List.of("BOLD"), emphasisOf(this.textNodeOf(ui, "b")), ui.toString());
        assertEquals(List.of("BOLD", "ITALIC"), emphasisOf(this.textNodeOf(ui, "bi")), "enum order: " + ui);
    }

    @Test
    public void testMarkdownBoldNoLongerBoldsTheWholeParagraph() throws Exception
    {
        JsonNode ui = this.render("plain **b** plain");

        List<JsonNode> paragraphs = findAll(ui, "PARAGRAPH");
        assertEquals(1, paragraphs.size(), ui.toString());
        assertFalse(paragraphs.get(0)
                              .path("bold")
                              .asBoolean(),
                    "the paragraph itself must not be bold: " + ui);
        assertEquals(List.of("BOLD"), emphasisOf(this.textNodeOf(ui, "b")), ui.toString());
    }

    @Test
    public void testAnUnflaggedRunKeepsTheParagraphCompositeTextShape() throws Exception
    {
        JsonNode ui = this.render("x");

        List<JsonNode> paragraphs = findAll(ui, "PARAGRAPH");
        assertEquals(1, paragraphs.size(), ui.toString());
        assertEquals("PARAGRAPH[COMPOSITE[TEXT(x)]]", shape(paragraphs.get(0)), ui.toString());
    }

    @Test
    public void testEmphasisInsideATableCellIsKept() throws Exception
    {
        JsonNode ui = this.render("| **h** | x |\n| --- | --- |\n| *i* | y |\n");

        List<JsonNode> tables = findAll(ui, "TABLE");
        assertEquals(1, tables.size(), ui.toString());
        assertEquals(List.of("BOLD"), emphasisOf(this.textNodeOf(tables.get(0), "h")), ui.toString());
        assertEquals(List.of("ITALIC"), emphasisOf(this.textNodeOf(tables.get(0), "i")), ui.toString());
    }

    @Test
    public void testNestedQuotesAreNestedBlockQuoteNodesAndAQuotedListStaysAList() throws Exception
    {
        JsonNode ui = this.render("> x\n>> y\n>\n> - d\n");

        List<JsonNode> blockQuotes = findAll(ui, "BLOCKQUOTE");
        assertEquals(2, blockQuotes.size(), "the outer and the nested quote: " + ui);
        JsonNode outer = blockQuotes.get(0);
        assertEquals(1, findAll(outer, "BLOCKQUOTE").size() - 1, "one quote inside the outer one: " + ui);
        assertEquals(List.of("y"), textsOf(blockQuotes.get(1))
                                                              .stream()
                                                              .map(String::trim)
                                                              .collect(Collectors.toList()),
                     ui.toString());
        List<JsonNode> lists = findAll(outer, "UNORDEREDLIST");
        assertEquals(1, lists.size(), ui.toString());
        assertEquals(List.of("d"), MarkdownPipelineTestSupport.entryTexts(lists.get(0)), ui.toString());
    }

    @Test
    public void testTwoQuotesSeparatedByAParagraphAreTwoBlockQuoteNodes() throws Exception
    {
        JsonNode ui = this.render("> one\n\nbetween\n\n> two\n");

        assertEquals(2, findAll(ui, "BLOCKQUOTE").size(), ui.toString());
    }

    @Test
    public void testListItemWithACodeBlockRendersTheCodeInsideItsEntry() throws Exception
    {
        JsonNode ui = this.render("- lead\n\n  ```java\n  int codesentinel = 1;\n  ```\n");

        JsonNode entry = this.singleEntryOf(ui, "UNORDEREDLIST");
        List<JsonNode> codeBlocks = findAll(entry, "NATIVEHTML");
        assertEquals(1, codeBlocks.size(), ui.toString());
        assertTrue(codeBlocks.get(0)
                             .path("source")
                             .asText()
                             .contains("int codesentinel = 1;"),
                   ui.toString());
        assertEquals(List.of("lead"), textsOf(entry), "the lead paragraph survives next to the code: " + ui);
    }

    @Test
    public void testListItemWithAHeadingRendersTheHeadingInsideItsEntry() throws Exception
    {
        JsonNode ui = this.render("- lead\n\n  ## headsentinel\n");

        JsonNode entry = this.singleEntryOf(ui, "UNORDEREDLIST");
        List<JsonNode> headings = findAll(entry, "HEADING");
        assertEquals(1, headings.size(), ui.toString());
        assertTrue(stringValuesOf(headings.get(0)).contains("headsentinel"), ui.toString());
    }

    @Test
    public void testListItemWithAnImageRendersTheImageInsideItsEntry() throws Exception
    {
        JsonNode ui = this.render("1. lead\n\n   ![alt](https://example.org/pic.png)\n");

        JsonNode entry = this.singleEntryOf(ui, "ORDEREDLIST");
        List<JsonNode> images = findAll(entry, "IMAGE");
        assertEquals(1, images.size(), ui.toString());
        assertEquals("https://example.org/pic.png", images.get(0)
                                                          .path("image")
                                                          .asText());
    }

    @Test
    public void testListItemWithATableRendersTheTableInsideItsEntry() throws Exception
    {
        JsonNode ui = this.render("- lead\n\n  | a | b |\n  | --- | --- |\n  | tablesentinel | 2 |\n");

        JsonNode entry = this.singleEntryOf(ui, "UNORDEREDLIST");
        List<JsonNode> tables = findAll(entry, "TABLE");
        assertEquals(1, tables.size(), ui.toString());
        assertTrue(textsOf(tables.get(0)).contains("tablesentinel"), ui.toString());
    }

    @Test
    public void testALooseItemWithTwoParagraphsIsOneEntryAndTheNextItemIsAnother() throws Exception
    {
        JsonNode ui = this.render("- one\n\n  two\n- three\n");

        List<JsonNode> lists = findAll(ui, "UNORDEREDLIST");
        assertEquals(1, lists.size(), ui.toString());
        List<JsonNode> entries = elementsOf(lists.get(0));
        assertEquals(2, entries.size(), "two items are two entries: " + ui);
        assertEquals(List.of("one", "two"), textsOf(entries.get(0)), ui.toString());
        assertEquals(List.of("three"), textsOf(entries.get(1)), ui.toString());
    }

    @Test
    public void testTaskListMarkersRenderAsGlyphsAtTheStartOfTheItem() throws Exception
    {
        JsonNode ui = this.render("- [ ] todo\n- [x] done\n");

        List<JsonNode> lists = findAll(ui, "UNORDEREDLIST");
        assertEquals(1, lists.size(), ui.toString());
        List<JsonNode> entries = elementsOf(lists.get(0));
        assertEquals(2, entries.size(), ui.toString());
        assertEquals(List.of(UNCHECKED_GLYPH, "todo"), this.trimmed(textsOf(entries.get(0))), ui.toString());
        assertEquals(List.of(CHECKED_GLYPH, "done"), this.trimmed(textsOf(entries.get(1))), ui.toString());
    }

    @Test
    public void testThematicBreakRendersAsAConstantHorizontalRuleBetweenItsNeighbours() throws Exception
    {
        JsonNode ui = this.render("above\n\n---\n\nbelow\n");

        List<JsonNode> rules = findAll(ui, "NATIVEHTML");
        assertEquals(1, rules.size(), ui.toString());
        assertEquals("<hr>", rules.get(0)
                                  .path("source")
                                  .asText());
        assertEquals(List.of("above", "below"), textsOf(ui), ui.toString());
    }

    /**
     * Trust boundary, pinned before the change: markdown is user supplied (kanban card descriptions), so raw html is never rendered and the only raw html
     * sources are the constant rule and a code block whose content is escaped.
     */
    @Test
    public void testRawHtmlOfTheMarkdownNeverReachesARawHtmlSource() throws Exception
    {
        JsonNode ui = this.render("before <b onclick=\"x()\">inline</b> after\n\n<script>alert(1)</script>\n\n```\n<script>alert(2)</script>\n```\n\n---\n");

        for (JsonNode source : findAll(ui, "NATIVEHTML"))
        {
            String html = source.path("source")
                                .asText();
            assertTrue("<hr>".equals(html) || (html.startsWith("<pre><code") && html.contains("&lt;script&gt;alert(2)")), "unexpected raw html: " + html);
            assertFalse(html.contains("<script"), html);
        }
        assertFalse(stringValuesOf(ui).stream()
                                      .anyMatch(value -> value.contains("onclick") || value.contains("alert(1)")),
                    "raw html of the markdown must not be rendered at all: " + ui);
    }

    private JsonNode singleEntryOf(JsonNode ui, String listType)
    {
        List<JsonNode> lists = findAll(ui, listType);
        assertEquals(1, lists.size(), ui.toString());
        List<JsonNode> entries = elementsOf(lists.get(0));
        assertEquals(1, entries.size(), "the one item is one entry: " + ui);
        return entries.get(0);
    }

    private List<String> trimmed(List<String> values)
    {
        return values.stream()
                     .map(String::trim)
                     .collect(Collectors.toList());
    }
}
