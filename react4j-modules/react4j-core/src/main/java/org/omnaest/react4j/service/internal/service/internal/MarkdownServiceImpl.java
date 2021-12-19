package org.omnaest.react4j.service.internal.service.internal;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;
import org.omnaest.react4j.domain.Button.Style;
import org.omnaest.react4j.domain.Card;
import org.omnaest.react4j.domain.Icon.StandardIcon;
import org.omnaest.react4j.domain.Paragraph;
import org.omnaest.react4j.domain.RatioContainer.Ratio;
import org.omnaest.react4j.domain.Table;
import org.omnaest.react4j.domain.Text;
import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.UIComponentFactory;
import org.omnaest.react4j.domain.UnsortedList;
import org.omnaest.react4j.service.internal.service.ContentService;
import org.omnaest.react4j.service.internal.service.ContentService.ContentImage;
import org.omnaest.react4j.service.internal.service.MarkdownService;
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
import org.omnaest.utils.markdown.MarkdownUtils.Link;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MarkdownServiceImpl implements MarkdownService
{
    @Autowired
    private ContentService contentService;

    @Override
    public FactoryLoadedMarkdownInterpreter interpreterWith(UIComponentFactory uiComponentFactory)
    {
        ContentService contentService = this.contentService;

        return new FactoryLoadedMarkdownInterpreter()
        {
            @Override
            public List<Card> newMarkdownCards(String markdown)
            {
                return StreamUtils.aggregateByStart(MarkdownUtils.parse(markdown, options -> options.enableWrapIntoParagraphs())
                                                                 .get(),
                                                    element -> element.asHeading()
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

                                                        Predicate<String> imageNameOrLinkFilter = link -> StringUtils.endsWithAny(link, ".png", ".jpg", ".jpeg",
                                                                                                                                  ".svg", ".bmp");
                                                        List<String> imageNamesAndLinks = titleAndText.getFirst()
                                                                                                      .map(Element::asHeading)
                                                                                                      .filter(Optional::isPresent)
                                                                                                      .map(Optional::get)
                                                                                                      .map(MarkdownUtils.Heading::getImages)
                                                                                                      .orElse(Collections.emptyList())
                                                                                                      .stream()
                                                                                                      .map(Image::getLink)
                                                                                                      .filter(imageNameOrLinkFilter)
                                                                                                      .distinct()
                                                                                                      .collect(Collectors.toList());
                                                        Predicate<String> imageLinkFilter = imageLink -> !contentService.findImage(imageLink)
                                                                                                                        .isPresent();
                                                        List<String> imageLinks = imageNamesAndLinks.stream()
                                                                                                    .filter(imageLinkFilter)
                                                                                                    .collect(Collectors.toList());
                                                        List<String> imageNames = imageNamesAndLinks.stream()
                                                                                                    .filter(imageLinkFilter.negate())
                                                                                                    .collect(Collectors.toList());
                                                        List<String> links = titleAndText.getFirst()
                                                                                         .map(Element::asHeading)
                                                                                         .filter(Optional::isPresent)
                                                                                         .map(Optional::get)
                                                                                         .map(MarkdownUtils.Heading::getLinks)
                                                                                         .orElse(Collections.emptyList())
                                                                                         .stream()
                                                                                         .map(Link::getLink)
                                                                                         .filter(imageNameOrLinkFilter.negate())
                                                                                         .distinct()
                                                                                         .collect(Collectors.toList());
                                                        String locator = links.stream()
                                                                              .findFirst()
                                                                              .orElse(RegExUtils.replaceAll(StringUtils.lowerCase(title.orElse(null)),
                                                                                                            "[^a-zA-Z]+", "_"));

                                                        //
                                                        if (imageLinks.stream()
                                                                      .findFirst()
                                                                      .isPresent())
                                                        {
                                                            String externalImageLink = imageLinks.stream()
                                                                                                 .findFirst()
                                                                                                 .get();
                                                            card.withImage(image -> image.withImage(externalImageLink)
                                                                                         .withName(title.orElse(null)));
                                                        }
                                                        else
                                                        {
                                                            String imageName = imageNames.stream()
                                                                                         .findFirst()
                                                                                         .orElse(locator);
                                                            Optional<ContentImage> firstImageNameMatch = Optional.ofNullable(imageName)
                                                                                                                 .flatMap(name -> contentService.findImages(name
                                                                                                                         + "\\.(jpg)|(png)|(svg)")
                                                                                                                                                .findFirst());
                                                            if (firstImageNameMatch.isPresent())
                                                            {
                                                                card.withImage(image -> image.withImage(firstImageNameMatch.get()
                                                                                                                           .getImagePath())
                                                                                             .withName(title.orElse(null)));
                                                            }
                                                        }

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
            public List<UIComponent<?>> parseMarkdownElements(Stream<Element> elements)
            {
                AtomicInteger referenceLinkCounter = new AtomicInteger(1);
                Function<Element, Stream<UIComponent<?>>> mapper = StreamUtils.redundantFlattener(element -> Stream.of(element)
                                                                                                                   .map(Element::asParagraph)
                                                                                                                   .filter(Optional::isPresent)
                                                                                                                   .map(Optional::get)
                                                                                                                   .map(this.createMarkdownParagraphMapper(referenceLinkCounter))
                                                                                                                   .filter(PredicateUtils.notNull())
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
                                                                                                                   .map(Element::asUnorderedList)
                                                                                                                   .filter(Optional::isPresent)
                                                                                                                   .map(Optional::get)
                                                                                                                   .map(this.createMarkdownUnorderedListMapper(referenceLinkCounter))
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
                                                             .flatMap(Element::asParagraph)
                                                             .map(MarkdownUtils.Paragraph::getElements)
                                                             .map(List::stream)
                                                             .flatMap(Stream::findFirst)
                                                             .flatMap(Element::asText)
                                                             .map(text -> !StringUtils.startsWith(text.getValue(), "|"))
                                                             .orElse(true);
                    return uiComponentFactory.newUnsortedList()
                                             .enableBulletPoints(enableBulletPoints)
                                             .addEntries(markdownList.getElements()
                                                                     .stream()
                                                                     .map(Element::asParagraph)
                                                                     .filter(Optional::isPresent)
                                                                     .map(Optional::get)
                                                                     .map(this.createMarkdownParagraphMapper(referenceLinkCounter, true))
                                                                     .filter(PredicateUtils.notNull())
                                                                     .collect(Collectors.toList()));
                };
            }

            private Function<MarkdownUtils.Paragraph, Paragraph> createMarkdownParagraphMapper(AtomicInteger referenceLinkCounter)
            {
                boolean removeLeadingPipe = false;
                return this.createMarkdownParagraphMapper(referenceLinkCounter, removeLeadingPipe);
            }

            private Function<MarkdownUtils.Paragraph, Paragraph> createMarkdownParagraphMapper(AtomicInteger referenceLinkCounter, boolean removeLeadingPipe)
            {
                return markdownParagraph ->
                {
                    Paragraph paragraph = uiComponentFactory.newParagraph();
                    markdownParagraph.getElements()
                                     .forEach(element ->
                                     {
                                         element.asText()
                                                .ifPresent(text ->
                                                {
                                                    //
                                                    boolean bold = text.isBold();
                                                    if (bold)
                                                    {
                                                        paragraph.withBoldStyle();
                                                    }

                                                    //
                                                    String value = removeLeadingPipe ? StringUtils.removeStart(text.getValue(), "|") : text.getValue();
                                                    Optional<Match> iconMatch = MatcherUtils.matcher()
                                                                                            .ofRegEx("^\\[ICON\\:([a-zA-Z\\_]+)\\](.*)")
                                                                                            .findInAnd(value)
                                                                                            .getFirst();

                                                    if (iconMatch.isPresent())
                                                    {
                                                        paragraph.addText(iconMatch.get()
                                                                                   .getSubGroup(1)
                                                                                   .flatMap(StandardIcon::of)
                                                                                   .orElse(null),
                                                                          iconMatch.get()
                                                                                   .getSubGroup(2)
                                                                                   .orElse(""));
                                                    }
                                                    else
                                                    {
                                                        paragraph.addText(value);
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
                                                                        String text = match.getSubGroup(3)
                                                                                           .orElse("");
                                                                        String style = match.getSubGroup(2)
                                                                                            .orElse(null);
                                                                        anker.withText(text)
                                                                             .withLink(link.getLink())
                                                                             .withStyle(Style.of(style)
                                                                                             .orElse(Style.PRIMARY));
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
                                                                                                                                                .withRatio(EnumUtils.toEnumValue(ratio,
                                                                                                                                                                                 Ratio.class)
                                                                                                                                                                    .orElse(Ratio._16x9))
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

            private Function<MarkdownUtils.Table, Table> createMarkdownTableMapper(AtomicInteger referenceLinkCounter)
            {
                return markdownTable -> uiComponentFactory.newTable()
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

            private Function<MarkdownUtils.Text, Text> createMarkdownTextMapper(AtomicInteger referenceLinkCounter)
            {
                return markdownText -> uiComponentFactory.newText()
                                                         .addText(markdownText.getValue());
            }
        };
    }

}
