package org.omnaest.react4j.service.internal.service.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.omnaest.react4j.service.internal.service.internal.MarkdownPipelineTestSupport.findAll;
import static org.omnaest.react4j.service.internal.service.internal.MarkdownPipelineTestSupport.shape;

import java.util.ArrayList;
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
 * plan-286 S1: the label of a markdown link is a run of elements, not a string. A plain label keeps its ANKER node exactly as it always was, a label holding
 * anything a string cannot carry (emphasis, inline code, an image, a line break) becomes the children of the ANKER, and the {@code [ICON:...]} directive keeps
 * the emphasis flags of the run it sits in.
 * <p>
 * The markdown goes through the real ui pipeline (no mocked collaborator) and every assertion is anchored on a node found in the rendered tree.
 *
 * @see MarkdownCompletenessGuardTest
 * @author omnaest
 */
@SpringBootTest(classes = MarkdownListRenderingTest.TestApplication.class, webEnvironment = WebEnvironment.MOCK)
@AutoConfigureMockMvc
public class MarkdownLinkLabelRenderingTest
{
    @Autowired
    private MockMvc        mockMvc;

    @Autowired
    private ReactUIService reactUIService;

    private JsonNode render(String markdown) throws Exception
    {
        return MarkdownPipelineTestSupport.render(this.mockMvc, this.reactUIService, markdown);
    }

    /**
     * The one ANKER node of the given link, asserted to be the only one.
     */
    private JsonNode ankerOf(JsonNode ui, String link)
    {
        List<JsonNode> matches = findAll(ui, "ANKER").stream()
                                                     .filter(anker -> link.equals(anker.path("link")
                                                                                       .asText()))
                                                     .collect(Collectors.toList());
        assertEquals(1, matches.size(), "exactly one ANKER linking " + link + ": " + ui);
        return matches.get(0);
    }

    private static List<String> fieldNamesOf(JsonNode node)
    {
        List<String> names = new ArrayList<>();
        node.fieldNames()
            .forEachRemaining(names::add);
        names.sort(String::compareTo);
        return names;
    }

    /**
     * The node of an anker that carries no children: no {@code elements} value at all (missing or null), the label a text.
     */
    private static void assertPlainAnker(JsonNode anker, String label, String title, String ui)
    {
        assertEquals("ANKER", anker.path("type")
                                   .asText(),
                     ui);
        assertEquals(label, anker.path("text")
                                 .path("DEFAULT")
                                 .asText(),
                     ui);
        assertEquals(title, anker.path("title")
                                 .path("DEFAULT")
                                 .asText(),
                     ui);
        assertEquals("BLANK", anker.path("page")
                                   .asText(),
                     ui);
        assertTrue(anker.path("elements")
                        .isMissingNode()
                   || anker.path("elements")
                           .isNull(),
                   "a plain anker has no elements value: " + ui);
    }

    /**
     * Pinned BEFORE the label became a run of elements (green on the old code): a plain link inside a paragraph keeps its ANKER node.
     */
    @Test
    public void testAPlainLinkInAParagraphKeepsItsAnkerNode() throws Exception
    {
        JsonNode ui = this.render("see [plain label](https://example.org/p) now");

        JsonNode anker = this.ankerOf(ui, "https://example.org/p");

        assertPlainAnker(anker, "plain label", "", ui.toString());
        assertEquals(List.of("link", "page", "target", "text", "title", "type", "uiContextData", "uiContextIds"), fieldNamesOf(anker).stream()
                                                                                                                                     .filter(name -> !"elements".equals(name))
                                                                                                                                     .collect(Collectors.toList()),
                     "the node carries exactly these fields besides a possible null elements: " + ui);
    }

    /**
     * Pinned BEFORE (green on the old code): a plain link that is a direct element of a table cell keeps its ANKER node and its title.
     */
    @Test
    public void testAPlainLinkInATableCellKeepsItsAnkerNodeAndItsTitle() throws Exception
    {
        JsonNode ui = this.render("| h | i |\n| --- | --- |\n| [cell label](https://example.org/t \"cell tip\") | y |\n");

        JsonNode anker = this.ankerOf(ui, "https://example.org/t");

        assertPlainAnker(anker, "cell label", "cell tip", ui.toString());
    }

    /**
     * A label that holds emphasis becomes the children of the ANKER, in order, each run with its own flags, and the text of the anker stays empty, so the label
     * is not shown twice.
     */
    @Test
    public void testEmphasisInsideALinkLabelBecomesChildrenOfTheAnkerAndTheTextIsNotDuplicated() throws Exception
    {
        JsonNode ui = this.render("see [**b** x](https://example.org/e) now");

        JsonNode anker = this.ankerOf(ui, "https://example.org/e");

        assertEquals("ANKER[TEXT(b)<BOLD>,TEXT( x)]", shape(anker), ui.toString());
        assertEquals("", anker.path("text")
                              .path("DEFAULT")
                              .asText(),
                     "the label is carried by the children only: " + ui);
        assertEquals("BLANK", anker.path("page")
                                   .asText(),
                     ui.toString());
    }

    @Test
    public void testItalicAndStrikethroughInsideALinkLabelAreKeptToo() throws Exception
    {
        assertEquals("ANKER[TEXT(i)<ITALIC>]", shape(this.ankerOf(this.render("[*i*](https://example.org/i)"), "https://example.org/i")));
        assertEquals("ANKER[TEXT(s)<STRIKETHROUGH>]", shape(this.ankerOf(this.render("[~~s~~](https://example.org/s)"), "https://example.org/s")));
    }

    /**
     * The emphasis a link inherits from the run around it ({@code **[a](u)**}) is the emphasis of the label.
     */
    @Test
    public void testEmphasisInheritedByTheWholeLinkIsKeptOnTheLabel() throws Exception
    {
        JsonNode ui = this.render("**[a](https://example.org/h)**");

        assertEquals("ANKER[TEXT(a)<BOLD>]", shape(this.ankerOf(ui, "https://example.org/h")), ui.toString());
    }

    @Test
    public void testInlineCodeInsideALinkLabelIsKeptNextToItsText() throws Exception
    {
        JsonNode ui = this.render("[`c` x](https://example.org/c)");

        JsonNode anker = this.ankerOf(ui, "https://example.org/c");

        assertEquals("ANKER[TEXT(c),TEXT( x)]", shape(anker), ui.toString());
        assertEquals("", anker.path("text")
                              .path("DEFAULT")
                              .asText(),
                     ui.toString());
    }

    /**
     * An image inside a link label used to produce an EMPTY anchor, because the flattened label has no text for an image.
     */
    @Test
    public void testAnImageInsideALinkLabelIsTheChildOfTheAnker() throws Exception
    {
        JsonNode ui = this.render("[![alt](https://img.example.org/p.png)](https://example.org/i)");

        JsonNode anker = this.ankerOf(ui, "https://example.org/i");

        assertEquals("ANKER[IMAGE]", shape(anker), ui.toString());
        assertEquals("https://img.example.org/p.png", findAll(anker, "IMAGE").get(0)
                                                                             .path("image")
                                                                             .asText(),
                     ui.toString());
    }

    /**
     * A link that is a direct element of a table cell takes the other branch, which keeps the title next to the children.
     */
    @Test
    public void testEmphasisInsideALinkLabelInATableCellBecomesChildrenAndTheTitleIsKept() throws Exception
    {
        JsonNode ui = this.render("| h | i |\n| --- | --- |\n| [**b** x](https://example.org/tc \"cell tip\") | y |\n");

        JsonNode anker = this.ankerOf(ui, "https://example.org/tc");

        assertEquals("ANKER[TEXT(b)<BOLD>,TEXT( x)]", shape(anker), ui.toString());
        assertEquals("cell tip", anker.path("title")
                                      .path("DEFAULT")
                                      .asText(),
                     ui.toString());
    }

    /**
     * The directives are recognised on the flattened label, as before, whatever the emphasis around it: a button stays a button and the reference marker stays
     * a plain anker with its counter text.
     */
    @Test
    public void testTheDirectiveLinksAreUnchanged() throws Exception
    {
        JsonNode ui = this.render("[BUTTON:SUCCESS:Join](https://example.org/b) and [**BUTTON:DANGER:Bold**](https://example.org/bb) and [[?]](https://example.org/q)");

        assertEquals(List.of("https://example.org/b", "https://example.org/bb"), findAll(ui, "ANKERBUTTON").stream()
                                                                                                           .map(button -> button.path("link")
                                                                                                                                .asText())
                                                                                                           .collect(Collectors.toList()),
                     ui.toString());
        JsonNode reference = this.ankerOf(ui, "https://example.org/q");
        assertPlainAnker(reference, "[1]", "", ui.toString());

        JsonNode iframeUi = this.render("[IFRAME:Embedded](https://example.org/if)");
        assertEquals(1, findAll(iframeUi, "IFRAMECONTAINER").size(), iframeUi.toString());
        assertTrue(findAll(iframeUi, "ANKER").isEmpty(), "an IFRAME directive is not a link: " + iframeUi);
    }

    /**
     * Pinned BEFORE (green on the old code): the node shape an {@code [ICON:...]} run produces without any emphasis flag.
     */
    @Test
    public void testAnUnflaggedIconRunKeepsTheCompositeOfTheIconAndTheText() throws Exception
    {
        JsonNode ui = this.render("[ICON:MICROSCOPE] foo");

        List<JsonNode> paragraphs = findAll(ui, "PARAGRAPH");
        assertEquals(1, paragraphs.size(), ui.toString());
        assertEquals("PARAGRAPH[COMPOSITE[ICON,TEXT( foo)]]", shape(paragraphs.get(0)), ui.toString());
    }

    /**
     * The {@code [ICON:...]} directive used to drop the emphasis of the run it sits in. The text next to the icon carries it now, inside the same composite
     * the unflagged run builds.
     */
    @Test
    public void testAnIconRunKeepsTheEmphasisFlagsOfItsRunOnTheTextNextToTheIcon() throws Exception
    {
        assertEquals("PARAGRAPH[COMPOSITE[ICON,TEXT( foo)<BOLD>]]", shape(findAll(this.render("**[ICON:MICROSCOPE] foo**"), "PARAGRAPH").get(0)));
        assertEquals("PARAGRAPH[COMPOSITE[ICON,TEXT( foo)<ITALIC>]]", shape(findAll(this.render("*[ICON:MICROSCOPE] foo*"), "PARAGRAPH").get(0)));
        assertEquals("PARAGRAPH[COMPOSITE[ICON,TEXT( foo)<STRIKETHROUGH>]]", shape(findAll(this.render("~~[ICON:MICROSCOPE] foo~~"), "PARAGRAPH").get(0)));
    }
}
