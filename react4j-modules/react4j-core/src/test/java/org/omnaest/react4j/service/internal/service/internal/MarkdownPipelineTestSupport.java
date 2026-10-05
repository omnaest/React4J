package org.omnaest.react4j.service.internal.service.internal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.omnaest.react4j.service.ReactUIService;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * The pipeline helpers shared by the markdown rendering tests: markdown goes through the real {@code /ui} endpoint (no mocked collaborator) and the node tree
 * the frontend receives is inspected by walks that are field name agnostic, so a text that merely occurs somewhere on the page cannot satisfy an assertion
 * that is anchored on a node.
 *
 * @see MarkdownListRenderingTest
 * @see MarkdownCompletenessGuardTest
 * @author omnaest
 */
final class MarkdownPipelineTestSupport
{
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private MarkdownPipelineTestSupport()
    {
    }

    /**
     * Registers the given markdown as the ui and returns the node tree the frontend receives for it.
     */
    static JsonNode render(MockMvc mockMvc, ReactUIService reactUIService, String markdown) throws Exception
    {
        reactUIService.createDefaultRoot(reactUI -> reactUI.addNewComponent(factory -> factory.newComposite()
                                                                                              .addComponents(factory.newMarkdown()
                                                                                                                    .texts()
                                                                                                                    .from(markdown))));
        // the response declares no charset, so MockMvc would decode the UTF-8 body as ISO-8859-1 and mangle every non-ASCII glyph; a browser reads UTF-8
        String json = mockMvc.perform(get("/ui"))
                             .andExpect(status().isOk())
                             .andReturn()
                             .getResponse()
                             .getContentAsString(StandardCharsets.UTF_8);
        return OBJECT_MAPPER.readTree(json);
    }

    /**
     * All nodes of the given type within the subtree, the subtree root included, parents before their descendants. The walk is field name agnostic.
     */
    static List<JsonNode> findAll(JsonNode subtree, String type)
    {
        List<JsonNode> found = new ArrayList<>();
        if (subtree.isObject() && subtree.hasNonNull("type") && type.equals(subtree.get("type")
                                                                                   .asText()))
        {
            found.add(subtree);
        }
        subtree.elements()
               .forEachRemaining(child -> found.addAll(findAll(child, type)));
        return found;
    }

    static List<JsonNode> elementsOf(JsonNode node)
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
    static List<String> entryTexts(JsonNode listNode)
    {
        return elementsOf(listNode).stream()
                                   .map(entry -> String.join(" ", textsOf(entry)))
                                   .collect(Collectors.toList());
    }

    /**
     * Every text value within the subtree, in document order.
     */
    static List<String> textsOf(JsonNode subtree)
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
                   .forEachRemaining(child -> texts.addAll(textsOf(child)));
        }
        return texts;
    }

    /**
     * The emphasis values of a TEXT node (e.g. {@code [BOLD, ITALIC]}), empty if it carries none.
     */
    static List<String> emphasisOf(JsonNode textNode)
    {
        List<String> emphasis = new ArrayList<>();
        textNode.path("emphasis")
                .elements()
                .forEachRemaining(value -> emphasis.add(value.asText()));
        return emphasis;
    }

    /**
     * Every string value within the subtree (field names excluded), in document order: the texts, hrefs, image sources and raw html sources alike.
     */
    static List<String> stringValuesOf(JsonNode subtree)
    {
        List<String> values = new ArrayList<>();
        if (subtree.isTextual())
        {
            values.add(subtree.asText());
        }
        subtree.elements()
               .forEachRemaining(child -> values.addAll(stringValuesOf(child)));
        return values;
    }

    /**
     * A compact structural projection of a node that ignores the generated ids and targets: {@code TYPE{flag}[child,child]} and {@code TEXT(value)}, the latter
     * followed by {@code <EMPHASIS,...>} where it carries emphasis.
     */
    static String shape(JsonNode node)
    {
        String type = node.path("type")
                          .asText();
        if ("TEXT".equals(type))
        {
            List<String> emphasis = emphasisOf(node);
            return "TEXT(" + String.join("|", textsOf(node)) + ")" + (emphasis.isEmpty() ? "" : "<" + String.join(",", emphasis) + ">");
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
            children.forEachRemaining(child -> childShapes.add(shape(child)));
            shape.append("[")
                 .append(String.join(",", childShapes))
                 .append("]");
        }
        return shape.toString();
    }
}
