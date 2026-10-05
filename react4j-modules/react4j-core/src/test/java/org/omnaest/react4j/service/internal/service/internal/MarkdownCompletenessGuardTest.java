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
 * <li>then it goes through the real ui pipeline in four contexts, at top level, as a list item child, as a block quote child and as (part of) the label of
 * a link, and its evidence has to be found in the node tree - the contexts matter, three of the five occurrences were a kind that rendered at top level
 * and vanished one level down, and the sixth (plan-286, kanban 698a6daa) was emphasis, code and an image that vanished inside a link label;</li>
 * <li>an allowlisted kind has to leave NO evidence, so a kind that starts being rendered turns its allowlist entry stale and fails.</li>
 * </ol>
 * The first three contexts hold every one of the 21 members (item 3 proves it per context). The link label does not: a block construct cannot sit in it, so
 * its applicability is EXPLICIT and complete. {@link #LINK_LABEL_APPLICABLE} lists the kinds CommonMark can place in a label, each of which must leave its
 * evidence UNDER AN ANKER node, and {@link #LINK_LABEL_NOT_APPLICABLE} lists every other kind with the reason it cannot; a kind with neither entry (a new
 * one) or with both fails by name.
 *
 * @author omnaest
 */
@SpringBootTest(classes = MarkdownListRenderingTest.TestApplication.class, webEnvironment = WebEnvironment.MOCK)
@AutoConfigureMockMvc
public class MarkdownCompletenessGuardTest
{
    private static final String              ELEMENT_PREFIX            = "element:";
    private static final String              FLAG_PREFIX               = "flag:";
    private static final String              CONTROL_SENTINEL          = "zqcontrol";
    private static final String              UNCHECKED_GLYPH           = "\u2610";
    private static final String              LINK_LABEL_HREF           = "https://example.org/zqlinklabel";

    /**
     * The kinds CommonMark can place in the label of a link. Each is rendered as a label with the fixture's markdown inside it, and its evidence has to be found
     * under an ANKER node. The three flags are here because emphasis inside a label is the case that was dropped (plan-286); Text, Code, Image and LineBreak
     * are what a label is made of; Html and CustomIdentifier are the allowlisted kinds that CAN occur there and so have to stay unrendered there too.
     */
    private static final Set<String>         LINK_LABEL_APPLICABLE     = new LinkedHashSet<>(List.of(ELEMENT_PREFIX + "Text", ELEMENT_PREFIX + "Code",
                                                                                                     ELEMENT_PREFIX + "Image", ELEMENT_PREFIX + "LineBreak",
                                                                                                     ELEMENT_PREFIX + "Html", ELEMENT_PREFIX + "CustomIdentifier",
                                                                                                     FLAG_PREFIX + "Bold", FLAG_PREFIX + "Italic",
                                                                                                     FLAG_PREFIX + "Strikethrough"));

    /**
     * Every other kind, with the reason it cannot be part of a link label. A reason must be non-blank: it is the only thing that tells a reader this is a
     * considered exclusion and not an oversight.
     */
    private static final Map<String, String> LINK_LABEL_NOT_APPLICABLE = linkLabelNotApplicable();

    @Autowired
    private MockMvc                          mockMvc;

    @Autowired
    private ReactUIService                   reactUIService;

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
     * (a) for the link label: every member of the vocabulary is either applicable in a link label (and then rendered there, which the context loops below
     * prove) or says why it cannot be one. A new kind that nobody decided about fails by name.
     */
    @Test
    public void testEveryVocabularyMemberIsEitherApplicableInALinkLabelOrStatesWhyNot()
    {
        assertEquals(List.of(), linkLabelProblems(vocabulary(), LINK_LABEL_APPLICABLE, LINK_LABEL_NOT_APPLICABLE));
    }

    /**
     * (a) for the link label, can it fail: a new kind, a kind that lost both of its entries, a kind with both, a blank reason and a stale entry are each named.
     */
    @Test
    public void testALinkLabelKindWithNeitherEntryOrBothOrABlankReasonOrAStaleKeyIsNamed()
    {
        Set<String> vocabulary = vocabulary();

        Set<String> withNewKind = new LinkedHashSet<>(vocabulary);
        withNewKind.add(ELEMENT_PREFIX + "Callout");
        Set<String> withoutImage = new LinkedHashSet<>(LINK_LABEL_APPLICABLE);
        withoutImage.remove(ELEMENT_PREFIX + "Image");
        Set<String> withBothForText = new LinkedHashSet<>(LINK_LABEL_APPLICABLE);
        Map<String, String> notApplicableWithText = new LinkedHashMap<>(LINK_LABEL_NOT_APPLICABLE);
        notApplicableWithText.put(ELEMENT_PREFIX + "Text", "not a reason, it IS applicable");
        Map<String, String> blankReason = new LinkedHashMap<>(LINK_LABEL_NOT_APPLICABLE);
        blankReason.put(ELEMENT_PREFIX + "Table", " ");
        Set<String> withStaleKey = new LinkedHashSet<>(LINK_LABEL_APPLICABLE);
        withStaleKey.add(ELEMENT_PREFIX + "Gone");

        List<String> newKind = linkLabelProblems(withNewKind, LINK_LABEL_APPLICABLE, LINK_LABEL_NOT_APPLICABLE);
        List<String> lostBoth = linkLabelProblems(vocabulary, withoutImage, LINK_LABEL_NOT_APPLICABLE);
        List<String> both = linkLabelProblems(vocabulary, withBothForText, notApplicableWithText);
        List<String> blank = linkLabelProblems(vocabulary, LINK_LABEL_APPLICABLE, blankReason);
        List<String> stale = linkLabelProblems(vocabulary, withStaleKey, LINK_LABEL_NOT_APPLICABLE);

        assertEquals(1, newKind.size(), newKind.toString());
        assertTrue(newKind.get(0)
                          .contains("element:Callout"),
                   newKind.toString());
        assertEquals(1, lostBoth.size(), lostBoth.toString());
        assertTrue(lostBoth.get(0)
                           .contains("element:Image"),
                   lostBoth.toString());
        assertEquals(1, both.size(), both.toString());
        assertTrue(both.get(0)
                       .contains("element:Text"),
                   both.toString());
        assertEquals(1, blank.size(), blank.toString());
        assertTrue(blank.get(0)
                        .contains("element:Table"),
                   blank.toString());
        assertEquals(1, stale.size(), stale.toString());
        assertTrue(stale.get(0)
                        .contains("element:Gone"),
                   stale.toString());
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
            for (Context context : contextsOf(entry.getKey()))
            {
                String document = context.place(entry.getValue().markdown);
                List<Element> parsed = MarkdownUtils.parse(document, MarkdownServiceImpl.PARSE_OPTIONS)
                                                    .get()
                                                    .collect(Collectors.toList());
                if (!this.anyElementProducesKind(parsed, entry.getKey(), context == Context.LINK_LABEL))
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
            for (Context context : contextsOf(entry.getKey()))
            {
                String document = CONTROL_SENTINEL + "\n\n" + context.place(fixture.markdown);
                JsonNode ui = MarkdownPipelineTestSupport.render(this.mockMvc, this.reactUIService, document);

                String where = "[" + entry.getKey() + " @ " + context + "]";
                boolean inLinkLabel = context == Context.LINK_LABEL;
                Predicate<JsonNode> evidence = inLinkLabel ? fixture.labelEvidence() : fixture.evidence;
                String evidenceDescription = inLinkLabel ? fixture.labelEvidenceDescription() : fixture.evidenceDescription;
                if (!hasTextContaining(ui, CONTROL_SENTINEL))
                {
                    failures.add(where + " the control text is missing, so the pipeline rendered nothing: " + ui);
                }
                else if (inLinkLabel && ankersLinking(ui, LINK_LABEL_HREF).isEmpty())
                {
                    failures.add(where + " the control anker is missing, so an absence under an ANKER would be vacuous: " + ui);
                }
                else if (fixture.isAllowlisted() && evidence.test(ui))
                {
                    failures.add(where + " is allowlisted (" + fixture.allowlistReason + ") but " + evidenceDescription + " is now rendered: the entry is stale");
                }
                else if (!fixture.isAllowlisted() && !(inLinkLabel ? ankersLinking(ui, LINK_LABEL_HREF).stream()
                                                                                                       .anyMatch(evidence)
                        : evidence.test(ui)))
                {
                    failures.add(where + " rendered no content" + (inLinkLabel ? " under the ANKER" : "") + ": expected " + evidenceDescription + " for "
                                 + document.replace("\n", "\\n"));
                }
            }
        }

        assertEquals(List.of(), failures);
    }

    /**
     * Whether any element of the parsed document (descending into every element with children and into the label of every link) is of the given kind. With
     * {@code onlyInLinkLabels} only the elements inside a link label count, which is what proves a fixture placed as a label really is one.
     */
    private boolean anyElementProducesKind(Collection<Element> elements, String member, boolean onlyInLinkLabels) throws Exception
    {
        return this.anyElementProducesKind(elements, member, onlyInLinkLabels, false);
    }

    private boolean anyElementProducesKind(Collection<Element> elements, String member, boolean onlyInLinkLabels, boolean insideLinkLabel) throws Exception
    {
        for (Element element : elements)
        {
            if ((!onlyInLinkLabels || insideLinkLabel) && this.producesKind(element, member))
            {
                return true;
            }
            Optional<MarkdownUtils.ElementWithChildren> withChildren = element.asElementWithChildren();
            if (withChildren.isPresent() && this.anyElementProducesKind(withChildren.get()
                                                                                    .getChildren(),
                                                                        member, onlyInLinkLabels, insideLinkLabel))
            {
                return true;
            }
            // a Link is not an ElementWithChildren: its label is reached through getElements()
            Optional<MarkdownUtils.Link> link = element.asLink();
            if (link.isPresent() && this.anyElementProducesKind(link.get()
                                                                    .getElements(),
                                                                member, onlyInLinkLabels, true))
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
        },
        /**
         * The fixture's markdown is the label of a link. Not every kind can be one, see {@link MarkdownCompletenessGuardTest#LINK_LABEL_APPLICABLE}.
         */
        LINK_LABEL {
            @Override
            String place(String markdown)
            {
                return "[" + markdown + "](" + LINK_LABEL_HREF + ")";
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
        private final String              labelEvidenceDescription;
        private final Predicate<JsonNode> labelEvidence;

        private Fixture(String markdown, String evidenceDescription, Predicate<JsonNode> evidence, String allowlistReason, String labelEvidenceDescription, Predicate<JsonNode> labelEvidence)
        {
            this.markdown = markdown;
            this.evidenceDescription = evidenceDescription;
            this.evidence = evidence;
            this.allowlistReason = allowlistReason;
            this.labelEvidenceDescription = labelEvidenceDescription;
            this.labelEvidence = labelEvidence;
        }

        boolean isAllowlisted()
        {
            return this.allowlistReason != null;
        }

        /**
         * The evidence of a rendering inside a link label, tried on each ANKER node in turn. It is the fixture's own evidence unless the kind is legitimately
         * carried by the anker itself, like a plain text, which is the label text of the anker and not a TEXT node.
         */
        Predicate<JsonNode> labelEvidence()
        {
            return this.labelEvidence != null ? this.labelEvidence : this.evidence;
        }

        String labelEvidenceDescription()
        {
            return this.labelEvidence != null ? this.labelEvidenceDescription : this.evidenceDescription;
        }

        Fixture withLabelEvidence(String description, Predicate<JsonNode> predicate)
        {
            return new Fixture(this.markdown, this.evidenceDescription, this.evidence, this.allowlistReason, description, predicate);
        }
    }

    private static Fixture rendered(String markdown, String evidenceDescription, Predicate<JsonNode> evidence)
    {
        return new Fixture(markdown, evidenceDescription, evidence, null, null, null);
    }

    private static Fixture notRendered(String markdown, String evidenceDescription, Predicate<JsonNode> evidence, String reason)
    {
        return new Fixture(markdown, evidenceDescription, evidence, reason, null, null);
    }

    /**
     * The ANKER nodes of the ui that link to the given target: the roots of the subtrees a link label's evidence is searched in.
     */
    private static List<JsonNode> ankersLinking(JsonNode ui, String link)
    {
        return findAll(ui, "ANKER").stream()
                                   .filter(anker -> link.equals(anker.path("link")
                                                                     .asText()))
                                   .collect(Collectors.toList());
    }

    /**
     * The contexts a vocabulary member is placed in: the first three for every member, and the link label only for the members that can occur in one.
     */
    private static List<Context> contextsOf(String member)
    {
        return Stream.of(Context.values())
                     .filter(context -> context != Context.LINK_LABEL || LINK_LABEL_APPLICABLE.contains(member))
                     .collect(Collectors.toList());
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
        fixtures.put(ELEMENT_PREFIX + "Text", rendered("textsentinel", "a TEXT holding it", ui -> hasTextContaining(ui, "textsentinel"))
                                                                                                                                        .withLabelEvidence("a string under the ANKER holding it (the label text of a plain label, or a TEXT child)",
                                                                                                                                                           ui -> hasAnyStringContaining(ui,
                                                                                                                                                                                        "textsentinel")));
        fixtures.put(ELEMENT_PREFIX + "Code", rendered("`codesentinel`", "a TEXT holding it", ui -> hasTextContaining(ui, "codesentinel"))
                                                                                                                                          .withLabelEvidence("a string under the ANKER holding it (the label text, or a TEXT child)",
                                                                                                                                                             ui -> hasAnyStringContaining(ui,
                                                                                                                                                                                          "codesentinel")));
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

    /**
     * The kinds that cannot be part of a link label, each with its reason. A block construct needs a line start of its own, and a link may not contain a link
     * (CommonMark gives the inner one precedence).
     */
    private static Map<String, String> linkLabelNotApplicable()
    {
        String block = "a block construct: it needs a line start of its own, so inside the brackets of a link label its marker is literal text";
        Map<String, String> notApplicable = new LinkedHashMap<>();
        notApplicable.put(ELEMENT_PREFIX + "CodeBlock", block + " (a fence or four spaces of indentation)");
        notApplicable.put(ELEMENT_PREFIX + "HtmlBlock", block + " (an html block starts a line)");
        notApplicable.put(ELEMENT_PREFIX + "ThematicBreak", block + " (a rule is a line of its own)");
        notApplicable.put(ELEMENT_PREFIX + "TaskListMarker", block + " (a task marker is the start of a list item)");
        notApplicable.put(ELEMENT_PREFIX + "Heading", block + " (an ATX marker)");
        notApplicable.put(ELEMENT_PREFIX + "Paragraph", block + " (the label is a span within one paragraph)");
        notApplicable.put(ELEMENT_PREFIX + "UnorderedList", block + " (a list marker)");
        notApplicable.put(ELEMENT_PREFIX + "OrderedList", block + " (a list marker)");
        notApplicable.put(ELEMENT_PREFIX + "ListItem", block + " (a list marker)");
        notApplicable.put(ELEMENT_PREFIX + "Table", block + " (a table is rows of lines)");
        notApplicable.put(ELEMENT_PREFIX + "BlockQuote", block + " (a quote marker)");
        notApplicable.put(ELEMENT_PREFIX + "Link", "a link inside a link label: CommonMark forbids it, links may not contain other links");
        return notApplicable;
    }

    /**
     * The messages of everything the two link label tables do not account for: a vocabulary member with neither an applicability entry nor a reason it cannot
     * be a label (a new kind), one with both, a not applicable entry without a reason, and an entry that matches no member (stale).
     */
    private static List<String> linkLabelProblems(Collection<String> vocabulary, Collection<String> applicable, Map<String, String> notApplicable)
    {
        List<String> problems = new ArrayList<>();
        vocabulary.forEach(member ->
        {
            boolean isApplicable = applicable.contains(member);
            boolean hasReason = notApplicable.containsKey(member);
            if (!isApplicable && !hasReason)
            {
                problems.add("vocabulary member " + member + " has neither a LINK_LABEL applicability entry nor a not-applicable reason: decide whether it can be part of a link label");
            }
            if (isApplicable && hasReason)
            {
                problems.add("vocabulary member " + member + " is both applicable in a LINK_LABEL and listed as not applicable: it is one or the other");
            }
        });
        notApplicable.forEach((member, reason) ->
        {
            if (reason == null || reason.isBlank())
            {
                problems.add("LINK_LABEL not-applicable entry " + member + " needs a reason");
            }
        });
        Stream.concat(applicable.stream(), notApplicable.keySet()
                                                        .stream())
              .filter(key -> !vocabulary.contains(key))
              .forEach(key -> problems.add("LINK_LABEL entry " + key + " matches no vocabulary member of CommonsMarkdown: remove the stale entry"));
        return problems;
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
