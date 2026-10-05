package org.omnaest.react4j.service.internal.service.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.omnaest.react4j.service.internal.service.internal.MarkdownPipelineTestSupport.elementsOf;
import static org.omnaest.react4j.service.internal.service.internal.MarkdownPipelineTestSupport.emphasisOf;
import static org.omnaest.react4j.service.internal.service.internal.MarkdownPipelineTestSupport.findAll;
import static org.omnaest.react4j.service.internal.service.internal.MarkdownPipelineTestSupport.stringValuesOf;
import static org.omnaest.react4j.service.internal.service.internal.MarkdownPipelineTestSupport.textsOf;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.omnaest.react4j.service.ReactUIService;
import org.omnaest.utils.markdown.MarkdownUtils;
import org.omnaest.utils.markdown.MarkdownUtils.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Completeness guard (kanban f6014ffd, plan-284 AC-S2-5) for the dispatch of {@link MarkdownServiceImpl} over the element vocabulary of CommonsMarkdown.
 * <p>
 * The failure class: {@code MarkdownServiceImpl} maps the kinds it knows and yields nothing for the others, and since the vocabulary is the set of
 * {@code as*} accessors of {@link Element} (plus the style flags of {@link MarkdownUtils.Text}) the compiler cannot make the dispatch exhaustive. It
 * happened five times (strikethrough, ordered list, nested list, block quote, list item children), each found by a user. This test makes the next one fail
 * by name instead:
 * <ol>
 * <li>the vocabulary is read REFLECTIVELY, so a new kind added to CommonsMarkdown is a member nobody has accounted for and fails here;</li>
 * <li>every member has a fixture (markdown plus the evidence that proves the kind was rendered) or an allowlist entry with a reason;</li>
 * <li>every fixture is first shown to produce its kind when parsed with the SAME options the interpreter uses ({@link MarkdownServiceImpl#PARSE_OPTIONS});</li>
 * <li>then it goes through the real ui pipeline in three contexts, at top level, as a list item child and as a block quote child, and its evidence has to
 * be found in the node tree - the contexts matter, three of the five occurrences were a kind that rendered at top level and vanished one level down;</li>
 * <li>an allowlisted kind has to leave NO evidence, so a kind that starts being rendered turns its allowlist entry stale and fails.</li>
 * </ol>
 * Every one of the 21 members is expressible in all three contexts (item 3 proves it per context), so no per-context exemption exists.
 *
 * @author omnaest
 */
@SpringBootTest(classes = MarkdownListRenderingTest.TestApplication.class, webEnvironment = WebEnvironment.MOCK)
@AutoConfigureMockMvc
public class MarkdownCompletenessGuardTest
{
    private static final String ELEMENT_PREFIX   = "element:";
    private static final String FLAG_PREFIX      = "flag:";
    private static final String CONTROL_SENTINEL = "zqcontrol";
    private static final String UNCHECKED_GLYPH  = "\u2610";

    @Autowired
    private MockMvc             mockMvc;

    @Autowired
    private ReactUIService      reactUIService;

    /**
     * (a) A kind CommonsMarkdown has but nobody accounted for fails by name. This is the check that catches a NEW kind.
     */
    @Test
    public void testEveryVocabularyMemberHasAFixtureOrAnAllowlistEntryAndNoKeyIsStale()
    {
        Set<String> vocabulary = vocabulary();

        assertTrue(vocabulary.size() >= 21, "control: the reflective read found the vocabulary, " + vocabulary);
        assertEquals(List.of(), unaccountedFor(vocabulary, fixtures().keySet()));
    }

    /**
     * (a) can it fail: a member added to the vocabulary and a key that is no member are both named.
     */
    @Test
    public void testAnUnaccountedNewKindAndAStaleKeyAreEachNamedByTheCheck()
    {
        Set<String> vocabulary = vocabulary();
        Set<String> withNewKind = new LinkedHashSet<>(vocabulary);
        withNewKind.add(ELEMENT_PREFIX + "Callout");
        Set<String> withStaleKey = new LinkedHashSet<>(fixtures().keySet());
        withStaleKey.add(ELEMENT_PREFIX + "Gone");

        List<String> newKindProblems = unaccountedFor(withNewKind, fixtures().keySet());
        List<String> staleKeyProblems = unaccountedFor(vocabulary, withStaleKey);

        assertEquals(1, newKindProblems.size(), newKindProblems.toString());
        assertTrue(newKindProblems.get(0)
                                  .contains("element:Callout"),
                   newKindProblems.toString());
        assertEquals(1, staleKeyProblems.size(), staleKeyProblems.toString());
        assertTrue(staleKeyProblems.get(0)
                                   .contains("element:Gone"),
                   staleKeyProblems.toString());
    }

    /**
     * (d) An allowlisted kind states why it is not rendered.
     */
    @Test
    public void testEveryAllowlistEntryStatesItsReason()
    {
        List<String> allowlisted = fixtures().entrySet()
                                             .stream()
                                             .filter(entry -> entry.getValue()
                                                                   .isAllowlisted())
                                             .map(Map.Entry::getKey)
                                             .collect(Collectors.toList());

        assertEquals(List.of(ELEMENT_PREFIX + "Html", ELEMENT_PREFIX + "HtmlBlock", ELEMENT_PREFIX + "CustomIdentifier"), allowlisted);
        fixtures().entrySet()
                  .stream()
                  .filter(entry -> entry.getValue()
                                        .isAllowlisted())
                  .forEach(entry -> assertFalse(entry.getValue().allowlistReason.isBlank(), entry.getKey() + " needs a reason"));
    }

    /**
     * (b) A fixture that does not produce its kind, in every context, with the options the interpreter parses with, proves nothing, so it fails.
     */
    @Test
    public void testEveryFixtureProducesItsKindInEveryContextWhenParsedWithTheInterpretersOptions() throws Exception
    {
        List<String> failures = new ArrayList<>();
        for (Map.Entry<String, Fixture> entry : fixtures().entrySet())
        {
            for (Context context : Context.values())
            {
                String document = context.place(entry.getValue().markdown);
                List<Element> parsed = MarkdownUtils.parse(document, MarkdownServiceImpl.PARSE_OPTIONS)
                                                    .get()
                                                    .collect(Collectors.toList());
                if (!this.anyElementProducesKind(parsed, entry.getKey()))
                {
                    failures.add("[" + entry.getKey() + " @ " + context + "] the fixture does not parse into its kind with MarkdownServiceImpl.PARSE_OPTIONS: "
                                 + document.replace("\n", "\\n"));
                }
            }
        }

        assertEquals(List.of(), failures);
    }

    /**
     * (c) and (d) Every fixture, in every context, through the real ui pipeline: a rendered kind leaves its evidence in the node tree, an allowlisted kind
     * leaves none, and the control text proves the pipeline ran at all, so an absence is not a vacuous one.
     */
    @Test
    public void testEveryKindLeavesItsEvidenceInEveryContextAndAnAllowlistedKindLeavesNone() throws Exception
    {
        List<String> failures = new ArrayList<>();
        for (Map.Entry<String, Fixture> entry : fixtures().entrySet())
        {
            Fixture fixture = entry.getValue();
            for (Context context : Context.values())
            {
                String document = CONTROL_SENTINEL + "\n\n" + context.place(fixture.markdown);
                JsonNode ui = MarkdownPipelineTestSupport.render(this.mockMvc, this.reactUIService, document);

                String where = "[" + entry.getKey() + " @ " + context + "]";
                if (!hasTextContaining(ui, CONTROL_SENTINEL))
                {
                    failures.add(where + " the control text is missing, so the pipeline rendered nothing: " + ui);
                }
                else if (fixture.isAllowlisted() && fixture.evidence.test(ui))
                {
                    failures.add(where + " is allowlisted (" + fixture.allowlistReason + ") but " + fixture.evidenceDescription + " is now rendered: the entry is stale");
                }
                else if (!fixture.isAllowlisted() && !fixture.evidence.test(ui))
                {
                    failures.add(where + " rendered no content: expected " + fixture.evidenceDescription + " for " + document.replace("\n", "\\n"));
                }
            }
        }

        assertEquals(List.of(), failures);
    }

    private boolean anyElementProducesKind(Collection<Element> elements, String member) throws Exception
    {
        for (Element element : elements)
        {
            if (this.producesKind(element, member))
            {
                return true;
            }
            Optional<MarkdownUtils.ElementWithChildren> withChildren = element.asElementWithChildren();
            if (withChildren.isPresent() && this.anyElementProducesKind(withChildren.get()
                                                                                    .getChildren(),
                                                                        member))
            {
                return true;
            }
        }
        return false;
    }

    private boolean producesKind(Element element, String member) throws Exception
    {
        if (member.startsWith(ELEMENT_PREFIX))
        {
            Method accessor = Element.class.getMethod("as" + member.substring(ELEMENT_PREFIX.length()));
            return ((Optional<?>) accessor.invoke(element)).isPresent();
        }
        Method flag = MarkdownUtils.Text.class.getMethod("is" + member.substring(FLAG_PREFIX.length()));
        Optional<MarkdownUtils.Text> text = element.asText();
        return text.isPresent() && (Boolean) flag.invoke(text.get());
    }

    /**
     * Where a fixture is placed: the document is the fixture's markdown as it is, wrapped into a list item or wrapped into a block quote.
     */
    private static enum Context
    {
        TOP_LEVEL {
            @Override
            String place(String markdown)
            {
                return markdown;
            }
        },
        LIST_ITEM_CHILD {
            @Override
            String place(String markdown)
            {
                return "- zqlead\n\n" + prefixLines(markdown, "  ");
            }
        },
        BLOCK_QUOTE_CHILD {
            @Override
            String place(String markdown)
            {
                return markdown.lines()
                               .map(line -> line.isEmpty() ? ">" : "> " + line)
                               .collect(Collectors.joining("\n"));
            }
        };

        abstract String place(String markdown);

        private static String prefixLines(String markdown, String prefix)
        {
            return markdown.lines()
                           .map(line -> line.isEmpty() ? line : prefix + line)
                           .collect(Collectors.joining("\n"));
        }
    }

    /**
     * What a vocabulary member is rendered from and how a rendering is recognised. A fixture with an allowlist reason is a kind that is deliberately NOT
     * rendered, so its evidence has to be absent.
     */
    private static final class Fixture
    {
        private final String              markdown;
        private final String              evidenceDescription;
        private final Predicate<JsonNode> evidence;
        private final String              allowlistReason;

        private Fixture(String markdown, String evidenceDescription, Predicate<JsonNode> evidence, String allowlistReason)
        {
            this.markdown = markdown;
            this.evidenceDescription = evidenceDescription;
            this.evidence = evidence;
            this.allowlistReason = allowlistReason;
        }

        boolean isAllowlisted()
        {
            return this.allowlistReason != null;
        }
    }

    private static Fixture rendered(String markdown, String evidenceDescription, Predicate<JsonNode> evidence)
    {
        return new Fixture(markdown, evidenceDescription, evidence, null);
    }

    private static Fixture notRendered(String markdown, String evidenceDescription, Predicate<JsonNode> evidence, String reason)
    {
        return new Fixture(markdown, evidenceDescription, evidence, reason);
    }

    private static boolean hasTextContaining(JsonNode ui, String fragment)
    {
        return findAll(ui, "TEXT").stream()
                                  .flatMap(text -> textsOf(text).stream())
                                  .anyMatch(value -> value.contains(fragment));
    }

    private static boolean hasNodeContaining(JsonNode ui, String type, String fragment)
    {
        return findAll(ui, type).stream()
                                .anyMatch(node -> stringValuesOf(node).stream()
                                                                      .anyMatch(value -> value.contains(fragment)));
    }

    private static boolean hasAnyStringContaining(JsonNode ui, String fragment)
    {
        return stringValuesOf(ui).stream()
                                 .anyMatch(value -> value.contains(fragment));
    }

    private static boolean hasEmphasisText(JsonNode ui, String value, String emphasis)
    {
        return findAll(ui, "TEXT").stream()
                                  .anyMatch(text -> List.of(value)
                                                        .equals(textsOf(text))
                                                    && emphasisOf(text).contains(emphasis));
    }

    private static boolean hasListEntryWithAllTexts(JsonNode ui, List<String> texts)
    {
        return Stream.concat(findAll(ui, "UNORDEREDLIST").stream(), findAll(ui, "ORDEREDLIST").stream())
                     .anyMatch(list -> elementsOf(list).stream()
                                                       .anyMatch(entry -> textsOf(entry).containsAll(texts)));
    }

    /**
     * One fixture per vocabulary member, keyed like {@link #vocabulary()}. A new CommonsMarkdown kind is added here by whoever adds it.
     */
    private static Map<String, Fixture> fixtures()
    {
        Map<String, Fixture> fixtures = new LinkedHashMap<>();
        fixtures.put(ELEMENT_PREFIX + "Text", rendered("textsentinel", "a TEXT holding it", ui -> hasTextContaining(ui, "textsentinel")));
        fixtures.put(ELEMENT_PREFIX + "Code", rendered("`codesentinel`", "a TEXT holding it", ui -> hasTextContaining(ui, "codesentinel")));
        fixtures.put(ELEMENT_PREFIX + "CodeBlock", rendered("```\ncodeblocksentinel\n```", "a NATIVEHTML pre block holding it",
                                                            ui -> findAll(ui, "NATIVEHTML").stream()
                                                                                           .map(node -> node.path("source")
                                                                                                            .asText())
                                                                                           .anyMatch(source -> source.startsWith("<pre><code")
                                                                                                               && source.contains("codeblocksentinel"))));
        fixtures.put(ELEMENT_PREFIX + "Html",
                     notRendered("before <u>htmlsentinel</u> after", "any value holding the raw tag <u>", ui -> hasAnyStringContaining(ui, "<u>"),
                                 "raw html of user supplied markdown (kanban card descriptions) is not rendered: it is a script injection path, and showing it escaped is not what the author meant"));
        fixtures.put(ELEMENT_PREFIX + "HtmlBlock",
                     notRendered("<div>htmlblocksentinel</div>", "any value holding the raw tag <div", ui -> hasAnyStringContaining(ui, "<div"),
                                 "raw html of user supplied markdown (kanban card descriptions) is not rendered: it is a script injection path, and showing it escaped is not what the author meant"));
        fixtures.put(ELEMENT_PREFIX + "ThematicBreak",
                     rendered("---", "a NATIVEHTML holding <hr", ui -> findAll(ui, "NATIVEHTML").stream()
                                                                                                .anyMatch(node -> node.path("source")
                                                                                                                      .asText()
                                                                                                                      .contains("<hr"))));
        fixtures.put(ELEMENT_PREFIX + "TaskListMarker", rendered("- [ ] tasksentinel", "a TEXT holding the unchecked glyph", ui -> hasTextContaining(ui, UNCHECKED_GLYPH)));
        fixtures.put(ELEMENT_PREFIX + "CustomIdentifier",
                     notRendered("before {ctokensentinel} after", "any value holding the token", ui -> hasAnyStringContaining(ui, "ctokensentinel"),
                                 "a parse-time token consumed by the table GRID mapping; it carries no content of its own"));
        fixtures.put(ELEMENT_PREFIX + "Heading", rendered("## headingsentinel", "a HEADING holding it", ui -> hasNodeContaining(ui, "HEADING", "headingsentinel")));
        fixtures.put(ELEMENT_PREFIX + "LineBreak", rendered("linebefore\\\nlineafter", "a LINEBREAK node", ui -> !findAll(ui, "LINEBREAK").isEmpty()));
        fixtures.put(ELEMENT_PREFIX + "Link", rendered("[linklabel](https://example.org/linksentinel)", "an ANKER linking it",
                                                       ui -> findAll(ui, "ANKER").stream()
                                                                                 .anyMatch(node -> "https://example.org/linksentinel".equals(node.path("link")
                                                                                                                                                 .asText()))));
        fixtures.put(ELEMENT_PREFIX + "Image", rendered("![imagelabel](https://example.org/imagesentinel.png)", "an IMAGE showing it",
                                                        ui -> findAll(ui, "IMAGE").stream()
                                                                                  .anyMatch(node -> "https://example.org/imagesentinel.png".equals(node.path("image")
                                                                                                                                                       .asText()))));
        fixtures.put(ELEMENT_PREFIX + "Paragraph", rendered("paragraphsentinel", "a PARAGRAPH holding it", ui -> hasNodeContaining(ui, "PARAGRAPH", "paragraphsentinel")));
        fixtures.put(ELEMENT_PREFIX + "UnorderedList",
                     rendered("- ulsentinel", "an UNORDEREDLIST holding it", ui -> hasNodeContaining(ui, "UNORDEREDLIST", "ulsentinel")));
        fixtures.put(ELEMENT_PREFIX + "OrderedList", rendered("7. olsentinel", "an ORDEREDLIST starting at 7 holding it",
                                                              ui -> findAll(ui, "ORDEREDLIST").stream()
                                                                                              .anyMatch(node -> node.path("startNumber")
                                                                                                                    .asInt() == 7
                                                                                                                && stringValuesOf(node).stream()
                                                                                                                                       .anyMatch(value -> value.contains("olsentinel")))));
        fixtures.put(ELEMENT_PREFIX + "Table", rendered("| ta | tb |\n| --- | --- |\n| tablesentinel | x |", "a TABLE holding it",
                                                        ui -> hasNodeContaining(ui, "TABLE", "tablesentinel")));
        fixtures.put(ELEMENT_PREFIX + "BlockQuote",
                     rendered("> blockquotesentinel", "a BLOCKQUOTE holding it", ui -> hasNodeContaining(ui, "BLOCKQUOTE", "blockquotesentinel")));
        fixtures.put(ELEMENT_PREFIX + "ListItem", rendered("- itemlead\n\n  itemtail", "one list entry holding both paragraphs",
                                                           ui -> hasListEntryWithAllTexts(ui, List.of("itemlead", "itemtail"))));
        fixtures.put(FLAG_PREFIX + "Bold", rendered("**boldsentinel**", "a TEXT with the emphasis BOLD", ui -> hasEmphasisText(ui, "boldsentinel", "BOLD")));
        fixtures.put(FLAG_PREFIX + "Italic", rendered("*italicsentinel*", "a TEXT with the emphasis ITALIC", ui -> hasEmphasisText(ui, "italicsentinel", "ITALIC")));
        fixtures.put(FLAG_PREFIX + "Strikethrough", rendered("~~strikesentinel~~", "a TEXT with the emphasis STRIKETHROUGH",
                                                             ui -> hasEmphasisText(ui, "strikesentinel", "STRIKETHROUGH")));
        return fixtures;
    }

    /**
     * The vocabulary, read from CommonsMarkdown itself: every public parameterless {@code as<Kind>()} of {@link Element} answering an {@link Optional} of an
     * {@link Element} subtype (the generic {@code as(Class)} and {@code asElementWithChildren()} are access paths, not kinds), and every public parameterless
     * boolean {@code is<Flag>()} of {@link MarkdownUtils.Text}.
     */
    private static Set<String> vocabulary()
    {
        Set<String> members = new LinkedHashSet<>();
        for (Method method : Element.class.getMethods())
        {
            if (method.getParameterCount() == 0 && method.getName()
                                                         .matches("as[A-Z]\\w*")
                && !"asElementWithChildren".equals(method.getName()) && Optional.class.equals(method.getReturnType()) && isElementSubtype(method))
            {
                members.add(ELEMENT_PREFIX + method.getName()
                                                   .substring(2));
            }
        }
        for (Method method : MarkdownUtils.Text.class.getMethods())
        {
            if (method.getParameterCount() == 0 && boolean.class.equals(method.getReturnType()) && method.getName()
                                                                                                         .matches("is[A-Z]\\w*"))
            {
                members.add(FLAG_PREFIX + method.getName()
                                                .substring(2));
            }
        }
        return members;
    }

    private static boolean isElementSubtype(Method method)
    {
        Type returnType = method.getGenericReturnType();
        if (returnType instanceof ParameterizedType)
        {
            Type argument = ((ParameterizedType) returnType).getActualTypeArguments()[0];
            return argument instanceof Class && Element.class.isAssignableFrom((Class<?>) argument);
        }
        return false;
    }

    /**
     * The messages of everything the fixture table does not account for: a vocabulary member with neither fixture nor allowlist entry (an unmapped new kind)
     * and a fixture key that matches no member (a stale entry).
     */
    private static List<String> unaccountedFor(Collection<String> vocabulary, Collection<String> fixtureKeys)
    {
        List<String> problems = new ArrayList<>();
        vocabulary.stream()
                  .filter(member -> !fixtureKeys.contains(member))
                  .forEach(member -> problems.add("vocabulary member " + member + " has neither a fixture nor an allowlist entry: add one in fixtures()"));
        fixtureKeys.stream()
                   .filter(key -> !vocabulary.contains(key))
                   .forEach(key -> problems.add("fixture key " + key + " matches no vocabulary member of CommonsMarkdown: remove the stale entry"));
        return problems;
    }
}
