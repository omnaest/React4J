package org.omnaest.react4j.service.internal.service.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.omnaest.react4j.domain.Button.Style;
import org.omnaest.react4j.domain.Card;
import org.omnaest.react4j.domain.Icon.StandardIcon;
import org.omnaest.react4j.domain.OrderedList;
import org.omnaest.react4j.domain.Paragraph;
import org.omnaest.react4j.domain.RatioContainer.Ratio;
import org.omnaest.react4j.domain.Text;
import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.UIComponentFactory;
import org.omnaest.react4j.domain.UnsortedList;
import org.omnaest.react4j.domain.markdown.MarkdownIssueHandler;
import org.omnaest.react4j.service.internal.service.ContentService;
import org.omnaest.react4j.service.internal.service.ContentService.ContentImage;
import org.omnaest.react4j.service.internal.service.MarkdownDirectiveResolver;
import org.omnaest.react4j.service.internal.service.MarkdownService;
import org.omnaest.utils.ConsumerUtils;
import org.omnaest.utils.EnumUtils;
import org.omnaest.utils.ListUtils;
import org.omnaest.utils.MapperUtils;
import org.omnaest.utils.MatcherUtils;
import org.omnaest.utils.MatcherUtils.Match;
import org.omnaest.utils.PredicateUtils;
import org.omnaest.utils.StreamUtils;
import org.omnaest.utils.element.bi.BiElement;
import org.omnaest.utils.markdown.MarkdownUtils;
import org.omnaest.utils.markdown.MarkdownUtils.Element;
import org.omnaest.utils.markdown.MarkdownUtils.Image;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Interprets markdown content into {@link UIComponent}s.<br>
 * <br>
 * Beside standard markdown a set of directives like <code>[BUTTON:SUCCESS:Label](link)</code> is understood. The grammar and the vocabularies of those
 * directives are documented within the README.md of the react4j-core-markdown module. A directive that cannot be interpreted falls back to a default and is
 * reported as a {@link org.omnaest.react4j.domain.markdown.MarkdownIssue} to the {@link MarkdownIssueHandler}.
 *
 * @see MarkdownDirectiveResolver
 * @see MarkdownIssueHandler
 * @author omnaest
 */
@Service
public class MarkdownServiceImpl implements MarkdownService
{
    /**
     * The options every markdown is parsed with, kept in this one place so that the completeness guard
     * ({@code MarkdownCompletenessGuardTest}) parses its fixtures with exactly the options the interpreter uses.
     */
    static final Consumer<MarkdownUtils.MarkdownParseOptions> PARSE_OPTIONS        = options -> options.enableWrapIntoParagraphs()
                                                                                                       .enableParseCustomIdTokens()
                                                                                                       .enableBlockQuotes()
                                                                                                       .enableListItems();

    /**
     * The markup of a thematic break. A constant on purpose: no user supplied text may reach a raw html source, so the rule is built from nothing but this.
     */
    private static final String                               THEMATIC_BREAK_HTML  = "<hr>";

    /**
     * The glyphs of the unchecked and the checked marker of a task list item (ballot box, ballot box with check), written as escapes so that the source stays ASCII.
     */
    private static final String                               UNCHECKED_TASK_GLYPH = "\u2610";
    private static final String                               CHECKED_TASK_GLYPH   = "\u2611";

    @Autowired
    private ContentService                                    contentService;

    /**
     * Optional application provided {@link MarkdownIssueHandler}. Without such a bean the issues are written to the log.
     */
    @Autowired(required = false)
    private MarkdownIssueHandler                              issueHandler;

    @Override
    public FactoryLoadedMarkdownInterpreter interpreterWith(UIComponentFactory uiComponentFactory)
    {
        ContentService contentService = this.contentService;
        MarkdownIssueHandler defaultIssueHandler = Optional.ofNullable(this.issueHandler)
                                                           .orElseGet(MarkdownIssueHandler::logging);

        return new FactoryLoadedMarkdownInterpreter() {
            /**
             * The interpreter is created per interpretation, so the origin and the handler of a single interpretation can be declared on it.
             */
            private MarkdownIssueHandler issueHandler = defaultIssueHandler;
            private String               source       = null;

            @Override
            public FactoryLoadedMarkdownInterpreter withSource(String source)
            {
                this.source = source;
                return this;
            }

            @Override
            public FactoryLoadedMarkdownInterpreter withIssueHandler(MarkdownIssueHandler issueHandler)
            {
                this.issueHandler = issueHandler;
                return this;
            }

            private MarkdownDirectiveResolver newDirectiveResolver(Optional<Integer> sourceLine)
            {
                return new MarkdownDirectiveResolver(this.issueHandler, this.source, sourceLine.orElse(null));
            }

            @Override
            public List<Card> newMarkdownCards(String markdown)
            {
                return StreamUtils.aggregateByStart(this.parseRawMarkdown(markdown), element -> element.asHeading()
                                                                                                       .map(heading -> heading.getStrength() <= 1)
                                                                                                       .orElse(false),
                                                    group ->
                                                    {
                                                        //
                                                        Card card = uiComponentFactory.newCard();

                                                        //
                                                        BiElement<Optional<Element>, Stream<Element>> titleAndText = StreamUtils.splitOne(group);
                                                        Optional<String> title = titleAndText.getFirst()
                                                                                             .map(Element::asHeading)
                                                                                             .filter(Optional::isPresent)
                                                                                             .map(Optional::get)
                                                                                             .map(MarkdownUtils.Heading::getText)
                                                                                             .filter(StringUtils::isNotBlank);
                                                        String locator = titleAndText.getFirst()
                                                                                     .map(Element::asHeading)
                                                                                     .filter(Optional::isPresent)
                                                                                     .map(Optional::get)
                                                                                     .map(MarkdownUtils.Heading::getCustomIds)
                                                                                     .orElse(Collections.emptyList())
                                                                                     .stream()
                                                                                     .findFirst()
                                                                                     .orElse(RegExUtils.replaceAll(StringUtils.lowerCase(title.orElse(null)),
                                                                                                                   "[^a-zA-Z]+", "_"));
                                                        Stream<Image> images = titleAndText.getFirst()
                                                                                           .map(Element::asHeading)
                                                                                           .filter(Optional::isPresent)
                                                                                           .map(Optional::get)
                                                                                           .map(MarkdownUtils.Heading::getImages)
                                                                                           .map(List::stream)
                                                                                           .orElse(Stream.empty());
                                                        this.createImageConfigurer(images, title, locator)
                                                            .ifPresent(card::withImage);

                                                        //
                                                        Predicate<Element> firstImageFilter = this.createFirstImageAsCardImageFilter(card);

                                                        return Stream.of(card.withTitle(title.orElse(null))
                                                                             .withLinkLocator(locator)
                                                                             .withContent(uiComponentFactory.newComposite()
                                                                                                            .addComponents(this.parseMarkdownElements(titleAndText.getSecond()
                                                                                                                                                                  .filter(firstImageFilter)))));
                                                    })
                                  .collect(Collectors.toList());
            }

            private Optional<Consumer<org.omnaest.react4j.domain.Image>> createImageConfigurer(Image image)
            {
                return this.createImageConfigurer(Stream.of(image), Optional.ofNullable(image.getLabel()), "");
            }

            private Optional<Consumer<org.omnaest.react4j.domain.Image>> createImageConfigurer(Stream<Image> images, Optional<String> title, String locator)
            {
                List<String> imageNamesAndLinks = Optional.ofNullable(images)
                                                          .orElse(Stream.empty())
                                                          .map(Image::getLink)
                                                          .distinct()
                                                          .collect(Collectors.toList());
                Predicate<String> imageLinkFilter = imageLink -> !contentService.findImageWithStandardSuffixes(imageLink)
                                                                                .isPresent();
                List<String> imageLinks = imageNamesAndLinks.stream()
                                                            .filter(imageLinkFilter)
                                                            .collect(Collectors.toList());
                List<String> imageNames = imageNamesAndLinks.stream()
                                                            .filter(imageLinkFilter.negate())
                                                            .collect(Collectors.toList());

                //
                Optional<Consumer<org.omnaest.react4j.domain.Image>> imageConsumer = Optional.empty();
                if (imageLinks.stream()
                              .findFirst()
                              .isPresent())
                {
                    String externalImageLink = imageLinks.stream()
                                                         .findFirst()
                                                         .get();
                    imageConsumer = Optional.of(image -> image.withImage(externalImageLink)
                                                              .withName(title.orElse(null)));

                }
                else
                {
                    String imageName = imageNames.stream()
                                                 .findFirst()
                                                 .orElse(locator);
                    Optional<ContentImage> firstImageNameMatch = Optional.ofNullable(imageName)
                                                                         .flatMap(name -> contentService.findImageWithStandardSuffixes(name));
                    if (firstImageNameMatch.isPresent())
                    {
                        imageConsumer = Optional.of(image -> image.withImage(firstImageNameMatch.get()
                                                                                                .getImagePath())
                                                                  .withName(title.orElse(null)));
                    }
                }
                return imageConsumer;
            }

            private Stream<Element> parseRawMarkdown(String markdown)
            {
                return MarkdownUtils.parse(markdown, PARSE_OPTIONS)
                                    .get();
            }

            private Predicate<Element> createFirstImageAsCardImageFilter(Card card)
            {
                return StreamUtils.filterConsumer(PredicateUtils.<Element>firstElement()
                                                                .and(element -> element.asParagraph()
                                                                                       .map(org.omnaest.utils.markdown.MarkdownUtils.Paragraph::getElements)
                                                                                       .filter(PredicateUtils.listNotEmpty())
                                                                                       .map(ListUtils::first)
                                                                                       .flatMap(Element::asImage)
                                                                                       .isPresent()),
                                                  element -> Optional.ofNullable(element)
                                                                     .flatMap(Element::asParagraph)
                                                                     .map(org.omnaest.utils.markdown.MarkdownUtils.Paragraph::getElements)
                                                                     .map(ListUtils::first)
                                                                     .flatMap(Element::asImage)
                                                                     .ifPresent(imageElement -> card.withImage(image -> image.withName(imageElement.getLabel())
                                                                                                                             .withImage(imageElement.getLink()))));
            }

            @Override
            public List<UIComponent<?>> parseMarkdownElements(String markdown)
            {
                return this.parseMarkdownElements(this.parseRawMarkdown(markdown));
            }

            private List<UIComponent<?>> parseMarkdownElements(Stream<Element> elements)
            {
                return this.parseMarkdownElements(elements, new AtomicInteger(1), false);
            }

            /**
             * The one dispatch over the block level element kinds, used for the document, for table cells and for the children of block quotes and list
             * items alike. A kind that is missing here is silently dropped, which is why {@code MarkdownCompletenessGuardTest} enumerates the vocabulary of
             * CommonsMarkdown and fails for a kind without a fixture. The leading pipe is only stripped from paragraphs that are direct elements of the stream,
             * not from anything below a block quote or a nested list (the unordered list mapper decides that for its own items).
             */
            private List<UIComponent<?>> parseMarkdownElements(Stream<Element> elements, AtomicInteger referenceLinkCounter, boolean removeLeadingPipe)
            {
                Function<Element, Stream<UIComponent<?>>> mapper = StreamUtils.redundantFlattener(element -> Stream.of(element)
                                                                                                                   .map(Element::asParagraph)
                                                                                                                   .filter(Optional::isPresent)
                                                                                                                   .map(Optional::get)
                                                                                                                   .map(this.createMarkdownParagraphMapper(referenceLinkCounter,
                                                                                                                                                           removeLeadingPipe))
                                                                                                                   .filter(PredicateUtils.notNull())
                                                                                                                   .map(MapperUtils.identity()),
                                                                                                  element -> Stream.of(element)
                                                                                                                   .map(Element::asBlockQuote)
                                                                                                                   .filter(Optional::isPresent)
                                                                                                                   .map(Optional::get)
                                                                                                                   .map(blockQuote -> uiComponentFactory.newBlockQuote()
                                                                                                                                                        .addComponents(this.parseMarkdownElements(blockQuote.getElements()
                                                                                                                                                                                                            .stream(),
                                                                                                                                                                                                  referenceLinkCounter,
                                                                                                                                                                                                  false)))
                                                                                                                   .map(MapperUtils.identity()),
                                                                                                  element -> Stream.of(element)
                                                                                                                   .map(Element::asThematicBreak)
                                                                                                                   .filter(Optional::isPresent)
                                                                                                                   .map(thematicBreak -> uiComponentFactory.newNativeHtml()
                                                                                                                                                           .withSource(THEMATIC_BREAK_HTML))
                                                                                                                   .map(MapperUtils.identity()),
                                                                                                  element -> Stream.of(element)
                                                                                                                   .map(Element::asHeading)
                                                                                                                   .filter(Optional::isPresent)
                                                                                                                   .map(Optional::get)
                                                                                                                   .map(heading -> uiComponentFactory.newParagraph()
                                                                                                                                                     .addHeading(heading.getText(),
                                                                                                                                                                 heading.getStrength()))
                                                                                                                   .filter(PredicateUtils.notNull())
                                                                                                                   .map(MapperUtils.identity()),
                                                                                                  element -> Stream.of(element)
                                                                                                                   .map(Element::asImage)
                                                                                                                   .filter(Optional::isPresent)
                                                                                                                   .map(Optional::get)
                                                                                                                   .map(image -> ConsumerUtils.consumeWithAndGet(uiComponentFactory.newImage(),
                                                                                                                                                                 this.createImageConfigurer(image)))
                                                                                                                   .filter(PredicateUtils.notNull())
                                                                                                                   .map(MapperUtils.identity()),
                                                                                                  element -> Stream.of(element)
                                                                                                                   .map(Element::asLink)
                                                                                                                   .filter(Optional::isPresent)
                                                                                                                   .map(Optional::get)
                                                                                                                   .map(link -> uiComponentFactory.newAnker()
                                                                                                                                                  .withLink(link.getLink())
                                                                                                                                                  .withText(link.getLabel())
                                                                                                                                                  .withTitle(link.getTooltip()))
                                                                                                                   .filter(PredicateUtils.notNull())
                                                                                                                   .map(MapperUtils.identity()),
                                                                                                  element -> Stream.of(element)
                                                                                                                   .map(Element::asCodeBlock)
                                                                                                                   .filter(Optional::isPresent)
                                                                                                                   .map(Optional::get)
                                                                                                                   .map(this::createCodeBlockComponent)
                                                                                                                   .filter(PredicateUtils.notNull())
                                                                                                                   .map(MapperUtils.identity()),
                                                                                                  element -> Stream.of(element)
                                                                                                                   .map(Element::asCode)
                                                                                                                   .filter(Optional::isPresent)
                                                                                                                   .map(Optional::get)
                                                                                                                   .map(code -> uiComponentFactory.newText()
                                                                                                                                                  .addNonTranslatedText(code.getValue()))
                                                                                                                   .filter(PredicateUtils.notNull())
                                                                                                                   .map(MapperUtils.identity()),
                                                                                                  element -> Stream.of(element)
                                                                                                                   .map(Element::asLineBreak)
                                                                                                                   .filter(Optional::isPresent)
                                                                                                                   .map(Optional::get)
                                                                                                                   .map(link -> uiComponentFactory.newLineBreak())
                                                                                                                   .filter(PredicateUtils.notNull())
                                                                                                                   .map(MapperUtils.identity()),
                                                                                                  element -> Stream.of(element)
                                                                                                                   .map(Element::asUnorderedList)
                                                                                                                   .filter(Optional::isPresent)
                                                                                                                   .map(Optional::get)
                                                                                                                   .map(this.createMarkdownUnorderedListMapper(referenceLinkCounter))
                                                                                                                   .filter(PredicateUtils.notNull())
                                                                                                                   .map(MapperUtils.identity()),
                                                                                                  element -> Stream.of(element)
                                                                                                                   .map(Element::asOrderedList)
                                                                                                                   .filter(Optional::isPresent)
                                                                                                                   .map(Optional::get)
                                                                                                                   .map(this.createMarkdownOrderedListMapper(referenceLinkCounter))
                                                                                                                   .filter(PredicateUtils.notNull())
                                                                                                                   .map(MapperUtils.identity()),
                                                                                                  element -> Stream.of(element)
                                                                                                                   .map(Element::asTable)
                                                                                                                   .filter(Optional::isPresent)
                                                                                                                   .map(Optional::get)
                                                                                                                   .map(this.createMarkdownTableMapper(referenceLinkCounter))
                                                                                                                   .filter(PredicateUtils.notNull())
                                                                                                                   .map(MapperUtils.identity()),
                                                                                                  element -> Stream.of(element)
                                                                                                                   .map(Element::asText)
                                                                                                                   .filter(Optional::isPresent)
                                                                                                                   .map(Optional::get)
                                                                                                                   .map(this.createMarkdownTextMapper(referenceLinkCounter))
                                                                                                                   .filter(PredicateUtils.notNull())
                                                                                                                   .map(MapperUtils.identity()));
                return elements.flatMap(mapper)
                               .collect(Collectors.toList());
            }

            private Function<MarkdownUtils.UnorderedList, UnsortedList> createMarkdownUnorderedListMapper(AtomicInteger referenceLinkCounter)
            {
                return markdownList ->
                {
                    boolean enableBulletPoints = markdownList.getElements()
                                                             .stream()
                                                             .findFirst()
                                                             .flatMap(Element::asListItem)
                                                             .map(MarkdownUtils.ListItem::getElements)
                                                             .map(List::stream)
                                                             .flatMap(Stream::findFirst)
                                                             .flatMap(Element::asParagraph)
                                                             .map(MarkdownUtils.Paragraph::getElements)
                                                             .map(List::stream)
                                                             .flatMap(Stream::findFirst)
                                                             .flatMap(Element::asText)
                                                             .map(text -> !StringUtils.startsWith(text.getValue(), "|"))
                                                             .orElse(true);
                    boolean removeLeadingPipe = true;
                    return uiComponentFactory.newUnsortedList()
                                             .enableBulletPoints(enableBulletPoints)
                                             .addEntries(this.mapListEntries(markdownList.getElements(), referenceLinkCounter, removeLeadingPipe));
                };
            }

            private Function<MarkdownUtils.OrderedList, OrderedList> createMarkdownOrderedListMapper(AtomicInteger referenceLinkCounter)
            {
                return markdownList ->
                {
                    boolean removeLeadingPipe = false;
                    return uiComponentFactory.newOrderedList()
                                             .withStartNumber(markdownList.getStartNumber())
                                             .addEntries(this.mapListEntries(markdownList.getElements(), referenceLinkCounter, removeLeadingPipe));
                };
            }

            /**
             * Maps the items of a list into the entries of the list component: one entry per {@link MarkdownUtils.ListItem}, built by running the children of
             * the item through the same dispatch as every other block ({@link #parseMarkdownElements(Stream, AtomicInteger, boolean)}), so a code block, a
             * heading, an image, a table or a nested list inside an item is rendered inside its entry. An item without children adds no entry.
             */
            private List<UIComponent<?>> mapListEntries(List<Element> listElements, AtomicInteger referenceLinkCounter, boolean removeLeadingPipe)
            {
                return listElements.stream()
                                   .map(Element::asListItem)
                                   .filter(Optional::isPresent)
                                   .map(Optional::get)
                                   .map(item -> this.mapListItem(item, referenceLinkCounter, removeLeadingPipe))
                                   .filter(Optional::isPresent)
                                   .map(Optional::get)
                                   .collect(Collectors.toList());
            }

            /**
             * One list item as one entry: a single resulting component is used as it is (so a plain item stays one paragraph), several are grouped into a
             * {@link org.omnaest.react4j.domain.Composite}, which adds no element of its own on the client. The marker of a task item is the first child of the
             * item, before its paragraph; it is rendered as a glyph at the start of that paragraph, or as a text of its own where no paragraph follows.
             */
            private Optional<UIComponent<?>> mapListItem(MarkdownUtils.ListItem item, AtomicInteger referenceLinkCounter, boolean removeLeadingPipe)
            {
                List<Element> children = new ArrayList<>(item.getElements());
                Optional<String> taskGlyph = children.stream()
                                                     .findFirst()
                                                     .flatMap(Element::asTaskListMarker)
                                                     .map(marker -> marker.isChecked() ? CHECKED_TASK_GLYPH : UNCHECKED_TASK_GLYPH);
                if (taskGlyph.isPresent())
                {
                    children.remove(0);
                }

                List<UIComponent<?>> components = new ArrayList<>();
                if (taskGlyph.isPresent())
                {
                    Optional<MarkdownUtils.Paragraph> firstParagraph = children.stream()
                                                                               .findFirst()
                                                                               .flatMap(Element::asParagraph);
                    if (firstParagraph.isPresent())
                    {
                        components.add(this.createMarkdownParagraphMapper(referenceLinkCounter, removeLeadingPipe, taskGlyph.get() + " ")
                                           .apply(firstParagraph.get()));
                        children.remove(0);
                    }
                    else
                    {
                        components.add(uiComponentFactory.newText()
                                                         .addNonTranslatedText(taskGlyph.get()));
                    }
                }
                components.addAll(this.parseMarkdownElements(children.stream(), referenceLinkCounter, removeLeadingPipe));

                if (components.isEmpty())
                {
                    return Optional.empty();
                }
                return Optional.of(components.size() == 1 ? components.get(0)
                        : uiComponentFactory.newComposite()
                                            .addComponents(components));
            }
            private Function<MarkdownUtils.Paragraph, Paragraph> createMarkdownParagraphMapper(AtomicInteger referenceLinkCounter)
            {
                boolean removeLeadingPipe = false;
                return this.createMarkdownParagraphMapper(referenceLinkCounter, removeLeadingPipe);
            }

            private Function<MarkdownUtils.Paragraph, Paragraph> createMarkdownParagraphMapper(AtomicInteger referenceLinkCounter, boolean removeLeadingPipe)
            {
                return this.createMarkdownParagraphMapper(referenceLinkCounter, removeLeadingPipe, null);
            }

            /**
             * @param leadingText
             *            a text added in front of everything else of the paragraph (the glyph of a task list item), or {@code null}
             */
            private Function<MarkdownUtils.Paragraph, Paragraph> createMarkdownParagraphMapper(AtomicInteger referenceLinkCounter, boolean removeLeadingPipe, String leadingText)
            {
                return markdownParagraph ->
                {
                    Paragraph paragraph = uiComponentFactory.newParagraph();
                    if (leadingText != null)
                    {
                        paragraph.addNonTranslatedText(leadingText);
                    }
                    markdownParagraph.getElements()
                                     .forEach(element ->
                                     {
                                         element.asText()
                                                .ifPresent(text ->
                                                {
                                                    String value = removeLeadingPipe ? StringUtils.removeStart(text.getValue(), "|") : text.getValue();
                                                    Optional<Match> iconMatch = MatcherUtils.matcher()
                                                                                            .ofRegEx("^\\[ICON\\:([a-zA-Z\\_]+)\\](.*)")
                                                                                            .findInAnd(value)
                                                                                            .getFirst();

                                                    if (iconMatch.isPresent())
                                                    {
                                                        String rawIcon = iconMatch.get()
                                                                                  .getSubGroup(1)
                                                                                  .orElse(null);
                                                        paragraph.addText(this.newDirectiveResolver(text.getSourceLine())
                                                                              .resolveIcon(rawIcon, "[ICON:" + rawIcon + "]")
                                                                              .orElse(null),
                                                                          iconMatch.get()
                                                                                   .getSubGroup(2)
                                                                                   .orElse(""));
                                                    }
                                                    else
                                                    {
                                                        Text.Emphasis[] emphasis = this.emphasisOf(text);
                                                        if (emphasis.length == 0)
                                                        {
                                                            paragraph.addText(value);
                                                        }
                                                        else
                                                        {
                                                            paragraph.addComponent(uiComponentFactory.newText()
                                                                                                     .addText(value)
                                                                                                     .withEmphasis(emphasis));
                                                        }
                                                    }
                                                });
                                         element.asHeading()
                                                .filter(heading -> StringUtils.isNotBlank(heading.getText()))
                                                .ifPresent(heading -> paragraph.addHeading(heading.getText(), heading.getStrength()));
                                         element.asImage()
                                                .ifPresent(image ->
                                                {
                                                    Optional<ContentImage> contentImage = contentService.findImage(image.getLink());
                                                    if (contentImage.isPresent())
                                                    {
                                                        paragraph.addImage(contentImage.get()
                                                                                       .getImageName(),
                                                                           contentImage.get()
                                                                                       .getImagePath());
                                                    }
                                                    else
                                                    {
                                                        paragraph.addImage(image.getLabel(), image.getLink());
                                                    }
                                                });
                                         element.asCode()
                                                .ifPresent(code -> paragraph.addNonTranslatedText(code.getValue()));
                                         element.asCodeBlock()
                                                .ifPresent(codeBlock -> paragraph.addComponent(this.createCodeBlockComponent(codeBlock)));
                                         element.asLineBreak()
                                                .ifPresent(lineBreak -> paragraph.addLineBreak());
                                         element.asLink()
                                                .ifPresent(link ->
                                                {
                                                    MatcherUtils.interpreter()
                                                                .ifContainsRegEx("^BUTTON(\\:([a-zA-Z]+))?\\:(.*)", match ->
                                                                {
                                                                    paragraph.addLinkButton(anker ->
                                                                    {
                                                                        MarkdownDirectiveResolver directiveResolver = this.newDirectiveResolver(link.getSourceLine());
                                                                        String rawToken = match.getMatchRegion();
                                                                        String text = match.getSubGroup(3)
                                                                                           .orElse("");
                                                                        String style = match.getSubGroup(2)
                                                                                            .orElse(null);
                                                                        anker.withText(directiveResolver.resolveButtonText(text, rawToken))
                                                                             .withLink(link.getLink())
                                                                             .withStyle(directiveResolver.resolveButtonStyle(style, rawToken));
                                                                    });
                                                                })
                                                                .ifContainsRegEx("^IFRAME\\:(VIDEO(\\_[x0-9]+)?\\:)?(.*)", match ->
                                                                {
                                                                    boolean hasVideo = match.getSubGroup(1)
                                                                                            .isPresent();
                                                                    String ratio = match.getSubGroup(2)
                                                                                        .orElse("");
                                                                    String title = match.getSubGroup(3)
                                                                                        .orElse("");
                                                                    if (hasVideo)
                                                                    {
                                                                        paragraph.addComponent(uiComponentFactory.newSizedContainer()
                                                                                                                 .withFullWidth()
                                                                                                                 .withHeightInViewPortRatio(0.8)
                                                                                                                 .withContent(uiComponentFactory.newRatioContainer()
                                                                                                                                                .withRatio(this.newDirectiveResolver(link.getSourceLine())
                                                                                                                                                               .resolveVideoRatio(ratio,
                                                                                                                                                                                  match.getMatchRegion()))
                                                                                                                                                .withContent(uiComponentFactory.newIFrame()
                                                                                                                                                                               .withSourceLink(link.getLink())
                                                                                                                                                                               .withTitle(title)
                                                                                                                                                                               .allowFullScreen())));

                                                                    }
                                                                    else
                                                                    {
                                                                        paragraph.addComponent(uiComponentFactory.newIFrame()
                                                                                                                 .withTitle(title)
                                                                                                                 .withSourceLink(link.getLink()));
                                                                    }

                                                                })
                                                                .ifContainsRegEx("\\[\\?\\]", match ->
                                                                {
                                                                    paragraph.addLink(anker -> anker.withText("[" + referenceLinkCounter.getAndIncrement()
                                                                                                              + "]")
                                                                                                    .withLink(link.getLink()));
                                                                })
                                                                .orElse(() ->
                                                                {
                                                                    paragraph.addLink(anker -> anker.withText(link.getLabel())
                                                                                                    .withLink(link.getLink()));
                                                                })
                                                                .accept(link.getLabel());
                                                });
                                     });
                    return paragraph;
                };
            }

            /**
             * Renders a code block as preformatted html. The content is escaped and never translated, since code is not prose.
             */
            private UIComponent<?> createCodeBlockComponent(MarkdownUtils.CodeBlock codeBlock)
            {
                String languageClass = codeBlock.getLanguage()
                                                .map(language -> " class=\"language-" + StringEscapeUtils.escapeHtml4(language) + "\"")
                                                .orElse("");
                return uiComponentFactory.newNativeHtml()
                                         .withSource("<pre><code" + languageClass + ">" + StringEscapeUtils.escapeHtml4(codeBlock.getValue()) + "</code></pre>");
            }

            private Function<MarkdownUtils.Table, UIComponent<?>> createMarkdownTableMapper(AtomicInteger referenceLinkCounter)
            {
                return markdownTable ->
                {
                    boolean isGrid = markdownTable.getCustomIds()
                                                  .anyMatch(token -> StringUtils.equalsIgnoreCase(token, "GRID"));
                    if (isGrid)
                    {
                        return uiComponentFactory.newGridContainer()
                                                 .addRow(uiRow ->
                                                 {
                                                     uiRow.addCells(markdownTable.getColumns()
                                                                                 .stream(),
                                                                    (uiCell, markdownTableCell) ->
                                                                    {
                                                                        uiCell.withContent(this.parseMarkdownElements(markdownTableCell.getElements()
                                                                                                                                       .stream()));
                                                                    });
                                                 })
                                                 .addRows(markdownTable.getRows()
                                                                       .stream(),
                                                          (uiRow, markdownTableRow) ->
                                                          {
                                                              uiRow.addCells(markdownTableRow.getCells()
                                                                                             .stream(),
                                                                             (uiCell, markdownTableCell) ->
                                                                             {
                                                                                 uiCell.withContent(this.parseMarkdownElements(markdownTableCell.getElements()
                                                                                                                                                .stream()));
                                                                             });
                                                          });
                    }
                    else
                    {
                        return uiComponentFactory.newTable()
                                                 .addRow(uiRow ->
                                                 {
                                                     uiRow.addCells(markdownTable.getColumns()
                                                                                 .stream(),
                                                                    (uiCell, markdownTableCell) ->
                                                                    {
                                                                        uiCell.withContent(this.parseMarkdownElements(markdownTableCell.getElements()
                                                                                                                                       .stream()));
                                                                    });
                                                 })
                                                 .addRows(markdownTable.getRows()
                                                                       .stream(),
                                                          (uiRow, markdownTableRow) ->
                                                          {
                                                              uiRow.addCells(markdownTableRow.getCells()
                                                                                             .stream(),
                                                                             (uiCell, markdownTableCell) ->
                                                                             {
                                                                                 uiCell.withContent(this.parseMarkdownElements(markdownTableCell.getElements()
                                                                                                                                                .stream()));
                                                                             });
                                                          });
                    }
                };
            }

            private Function<MarkdownUtils.Text, Text> createMarkdownTextMapper(AtomicInteger referenceLinkCounter)
            {
                return markdownText -> uiComponentFactory.newText()
                                                         .addText(markdownText.getValue())
                                                         .withEmphasis(this.emphasisOf(markdownText));
            }

            /**
             * The emphasis of a markdown text run, in enum order. Every flag CommonsMarkdown reports has its member here; the guard test reads the flags
             * reflectively and fails for one that renders no evidence.
             */
            private Text.Emphasis[] emphasisOf(MarkdownUtils.Text markdownText)
            {
                List<Text.Emphasis> emphasis = new ArrayList<>();
                if (markdownText.isBold())
                {
                    emphasis.add(Text.Emphasis.BOLD);
                }
                if (markdownText.isItalic())
                {
                    emphasis.add(Text.Emphasis.ITALIC);
                }
                if (markdownText.isStrikethrough())
                {
                    emphasis.add(Text.Emphasis.STRIKETHROUGH);
                }
                return emphasis.toArray(new Text.Emphasis[0]);
            }
        };
    }

}
