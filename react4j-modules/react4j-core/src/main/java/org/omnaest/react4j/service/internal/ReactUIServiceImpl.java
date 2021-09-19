/*******************************************************************************
 * Copyright 2021 Danny Kunz
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package org.omnaest.react4j.service.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;
import org.omnaest.react4j.domain.Anker;
import org.omnaest.react4j.domain.AnkerButton;
import org.omnaest.react4j.domain.BlockQuote;
import org.omnaest.react4j.domain.Button;
import org.omnaest.react4j.domain.Button.Style;
import org.omnaest.react4j.domain.Card;
import org.omnaest.react4j.domain.Composite;
import org.omnaest.react4j.domain.Form;
import org.omnaest.react4j.domain.GridContainer;
import org.omnaest.react4j.domain.Heading;
import org.omnaest.react4j.domain.IFrame;
import org.omnaest.react4j.domain.Icon;
import org.omnaest.react4j.domain.Icon.StandardIcon;
import org.omnaest.react4j.domain.Image;
import org.omnaest.react4j.domain.ImageIndex;
import org.omnaest.react4j.domain.IntervalRerenderingContainer;
import org.omnaest.react4j.domain.Jumbotron;
import org.omnaest.react4j.domain.LineBreak;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.NavigationBar;
import org.omnaest.react4j.domain.NavigationBar.NavigationBarConsumer;
import org.omnaest.react4j.domain.NavigationBar.NavigationBarProvider;
import org.omnaest.react4j.domain.PaddingContainer;
import org.omnaest.react4j.domain.Paragraph;
import org.omnaest.react4j.domain.ProgressBar;
import org.omnaest.react4j.domain.RatioContainer;
import org.omnaest.react4j.domain.RatioContainer.Ratio;
import org.omnaest.react4j.domain.ReactUI;
import org.omnaest.react4j.domain.RerenderingContainer;
import org.omnaest.react4j.domain.ScrollbarContainer;
import org.omnaest.react4j.domain.SizedContainer;
import org.omnaest.react4j.domain.Table;
import org.omnaest.react4j.domain.Text;
import org.omnaest.react4j.domain.TextAlignmentContainer;
import org.omnaest.react4j.domain.Toaster;
import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.UIComponentFactory;
import org.omnaest.react4j.domain.UIComponentFactory.MarkdownComponentFactory;
import org.omnaest.react4j.domain.UnsortedList;
import org.omnaest.react4j.domain.VerticalContentSwitcher;
import org.omnaest.react4j.domain.configuration.HomePageConfiguration;
import org.omnaest.react4j.domain.i18n.UILocale;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.RenderableUIComponent;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer.EventHandlerRegistrationSupport;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer.ParentLocationAndComponent;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.domain.rendering.node.NodeRenderType;
import org.omnaest.react4j.domain.rendering.node.NodeRendererRegistry;
import org.omnaest.react4j.domain.support.UIComponentFactoryFunction;
import org.omnaest.react4j.domain.support.UIComponentProvider;
import org.omnaest.react4j.service.ReactUIService;
import org.omnaest.react4j.service.internal.component.AnkerButtonImpl;
import org.omnaest.react4j.service.internal.component.AnkerImpl;
import org.omnaest.react4j.service.internal.component.BlockQuoteImpl;
import org.omnaest.react4j.service.internal.component.ButtonImpl;
import org.omnaest.react4j.service.internal.component.CardImpl;
import org.omnaest.react4j.service.internal.component.ComponentContext;
import org.omnaest.react4j.service.internal.component.CompositeImpl;
import org.omnaest.react4j.service.internal.component.FormImpl;
import org.omnaest.react4j.service.internal.component.GridContainerImpl;
import org.omnaest.react4j.service.internal.component.HeadingImpl;
import org.omnaest.react4j.service.internal.component.IFrameImpl;
import org.omnaest.react4j.service.internal.component.IconImpl;
import org.omnaest.react4j.service.internal.component.ImageImpl;
import org.omnaest.react4j.service.internal.component.ImageIndexImpl;
import org.omnaest.react4j.service.internal.component.IntervalRerenderingContainerImpl;
import org.omnaest.react4j.service.internal.component.JumbotronImpl;
import org.omnaest.react4j.service.internal.component.LineBreakImpl;
import org.omnaest.react4j.service.internal.component.NavigationBarImpl;
import org.omnaest.react4j.service.internal.component.PaddingContainerImpl;
import org.omnaest.react4j.service.internal.component.ParagraphImpl;
import org.omnaest.react4j.service.internal.component.ProgressBarImpl;
import org.omnaest.react4j.service.internal.component.RatioContainerImpl;
import org.omnaest.react4j.service.internal.component.RerenderingContainerImpl;
import org.omnaest.react4j.service.internal.component.ScrollbarContainerImpl;
import org.omnaest.react4j.service.internal.component.SizedContainerImpl;
import org.omnaest.react4j.service.internal.component.TableImpl;
import org.omnaest.react4j.service.internal.component.TextAlignmentContainerImpl;
import org.omnaest.react4j.service.internal.component.TextImpl;
import org.omnaest.react4j.service.internal.component.ToasterImpl;
import org.omnaest.react4j.service.internal.component.UnsortedListImpl;
import org.omnaest.react4j.service.internal.component.VerticalContentSwitcherImpl;
import org.omnaest.react4j.service.internal.configuration.ProfileFlagConfiguration.UICacheEnabledFlag;
import org.omnaest.react4j.service.internal.domain.ReactUIInternal;
import org.omnaest.react4j.service.internal.handler.EventHandlerRegistry;
import org.omnaest.react4j.service.internal.handler.domain.DataEventHandler;
import org.omnaest.react4j.service.internal.handler.domain.EventHandler;
import org.omnaest.react4j.service.internal.handler.domain.Target;
import org.omnaest.react4j.service.internal.nodes.CompositeNode;
import org.omnaest.react4j.service.internal.nodes.HomePageNode;
import org.omnaest.react4j.service.internal.nodes.NodeHierarchy;
import org.omnaest.react4j.service.internal.nodes.service.RootNodeResolverService;
import org.omnaest.react4j.service.internal.rerenderer.RerenderingNodeProviderRegistry;
import org.omnaest.react4j.service.internal.rerenderer.RerenderingNodeProviderRegistry.RerenderedNodeProvider;
import org.omnaest.react4j.service.internal.service.ContentService;
import org.omnaest.react4j.service.internal.service.ContentService.ContentFile;
import org.omnaest.react4j.service.internal.service.ContentService.ContentImage;
import org.omnaest.react4j.service.internal.service.ContextFactory;
import org.omnaest.react4j.service.internal.service.HomePageConfigurationService;
import org.omnaest.react4j.service.internal.service.LocalizedTextResolverService;
import org.omnaest.react4j.service.internal.service.NodeHierarchyStaticRenderer;
import org.omnaest.react4j.service.internal.service.NodeHierarchyStaticRenderer.NodeHierarchyRenderingProcessor;
import org.omnaest.react4j.service.internal.service.ReactUIContextManager;
import org.omnaest.react4j.service.internal.service.ReactUIContextManager.ReactUIInternalProvider;
import org.omnaest.react4j.service.internal.service.internal.LocationSupportImpl;
import org.omnaest.react4j.service.internal.service.internal.RenderingProcessorImpl;
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
import org.omnaest.utils.stream.FilterMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ReactUIServiceImpl implements ReactUIService, RootNodeResolverService
{
    private static class EventHandlerRegistrationSupportImpl implements EventHandlerRegistrationSupport
    {
        private final EventHandlerRegistry            eventHandlerRegistry;
        private final RerenderingNodeProviderRegistry rerenderingNodeProviderRegistry;
        private final Location                        location;
        private final Target                          target;
        private final RenderingProcessor              renderingProcessor;
        private final RenderableUIComponent<?>        component;

        private EventHandlerRegistrationSupportImpl(EventHandlerRegistry eventHandlerRegistry, RerenderingNodeProviderRegistry rerenderingNodeProviderRegistry,
                                                    Location location, Target target, RenderingProcessor renderingProcessor, RenderableUIComponent<?> component)
        {
            this.eventHandlerRegistry = eventHandlerRegistry;
            this.rerenderingNodeProviderRegistry = rerenderingNodeProviderRegistry;
            this.location = location;
            this.target = target;
            this.renderingProcessor = renderingProcessor;
            this.component = component;
        }

        @Override
        public EventHandlerRegistrationSupport registerAsRerenderingNode()
        {
            RerenderedNodeProvider rerenderedNodeProvider = data -> this.renderingProcessor.process(this.component, this.location.getParent(), data);
            this.rerenderingNodeProviderRegistry.register(this.target, rerenderedNodeProvider);
            return this;
        }

        @Override
        public EventHandlerRegistrationSupport register(DataEventHandler eventHandler)
        {
            this.eventHandlerRegistry.registerDataEventHandler(this.target, eventHandler);
            return this;
        }

        @Override
        public EventHandlerRegistrationSupport register(EventHandler eventHandler)
        {
            this.eventHandlerRegistry.registerEventHandler(this.target, eventHandler);
            return this;
        }
    }

    @Autowired
    protected LocalizedTextResolverService textResolver;

    @Autowired
    protected EventHandlerRegistry eventHandlerRegistry;

    @Autowired
    protected RerenderingNodeProviderRegistry rerenderingNodeProviderRegistry;

    @Autowired
    protected ContextFactory dataContextFactory;

    @Autowired
    protected HomePageConfigurationService homePageConfigurationService;

    @Autowired
    protected NodeHierarchyStaticRenderer nodeHierarchyStaticRenderer;

    @Autowired
    protected ContentService contentService;

    protected ReactUIContextManager uiManager = new ReactUIContextManager();

    @Value("${ui.cache.duration:10}")
    protected int cacheDurationInSeconds;

    @Autowired
    @UICacheEnabledFlag
    protected boolean cachingEnabled;

    @Override
    public ReactUIService enableCaching(boolean active)
    {
        this.cachingEnabled = active;
        return this;
    }

    @Override
    public ReactUIService withCacheDurationInSeconds(int cacheDurationInSeconds)
    {
        this.cacheDurationInSeconds = cacheDurationInSeconds;
        return this;
    }

    @Override
    public ReactUIService disableCaching()
    {
        return this.enableCaching(false);
    }

    @Override
    public ReactUIService createDefaultRoot(Consumer<ReactUI> reactUIConsumer)
    {
        return this.createRoot(DEFAULT_CONTEXT_PATH, reactUIConsumer);
    }

    @Override
    public ReactUIService getOrCreateDefaultRoot(Consumer<ReactUI> reactUIConsumer)
    {
        return this.getOrCreateRoot(DEFAULT_CONTEXT_PATH, reactUIConsumer);
    }

    @Override
    public ReactUIService getOrCreateRoot(String contextPath, Consumer<ReactUI> reactUIConsumer)
    {
        this.uiManager.computeIfAbsent(contextPath, ReactUIInternalProvider.fromFactory(this.cachingEnabled, this.cacheDurationInSeconds, () ->
        {
            ReactUIInternal reactUIInternal = this.createRootInternal(contextPath);
            reactUIConsumer.accept(reactUIInternal);
            return reactUIInternal;
        }));
        return this;
    }

    @Override
    public ReactUIService createRoot(String contextPath, Consumer<ReactUI> reactUIConsumer)
    {
        this.uiManager.putAndGet(contextPath, ReactUIInternalProvider.fromFactory(this.cachingEnabled, this.cacheDurationInSeconds, () ->
        {
            ReactUIInternal reactUIInternal = this.createRootInternal(contextPath);
            reactUIConsumer.accept(reactUIInternal);
            return reactUIInternal;
        }));
        return this;
    }

    public ReactUIInternal createRootInternal(String contextPath)
    {
        return new ReactUIInternal()
        {
            private UILocale             defaultLocale = UILocale.of(Locale.US);
            private List<UIComponent<?>> components    = new ArrayList<>();
            private NavigationBar        navigationBar = null;

            @Override
            public ReactUI addNewComponent(UIComponentFactoryFunction factoryConsumer)
            {
                return this.addComponent(factoryConsumer.apply(this.componentFactory()));
            }

            @Override
            public ReactUI addComponent(UIComponent<?> component)
            {
                if (component instanceof NavigationBar)
                {
                    return this.withNavigationBar((NavigationBar) component);
                }
                else
                {
                    this.components.add(component);
                    return this;
                }
            }

            @Override
            public ReactUI addComponents(List<? extends UIComponent<?>> components)
            {
                this.components.addAll(components);
                return this;
            }

            @Override
            public ReactUI addComponent(UIComponentProvider<?> componentProvider)
            {
                return this.addComponent(componentProvider.get());
            }

            @Override
            public ReactUI withNavigationBar(NavigationBar navigationBar)
            {
                this.navigationBar = navigationBar;
                return this;
            }

            @Override
            public ReactUI withNavigationBar(NavigationBarConsumer navigationBarConsumer)
            {
                NavigationBar navigationBar = this.componentFactory()
                                                  .newNavigationBar();
                navigationBarConsumer.accept(navigationBar);
                return this.withNavigationBar(navigationBar);
            }

            @Override
            public ReactUI withNavigationBar(NavigationBarProvider navigationBarProvider)
            {
                return this.withNavigationBar(navigationBarProvider.get());
            }

            @Override
            public ReactUI withDefaultLanguage(Locale locale)
            {
                this.defaultLocale = UILocale.of(locale);
                return this;
            }

            @Override
            public UIComponentFactory componentFactory()
            {
                return new UIComponentFactory()
                {
                    private ComponentContext context = new ComponentContext(defaultLocale, ReactUIServiceImpl.this.textResolver,
                                                                            ReactUIServiceImpl.this.eventHandlerRegistry, this,
                                                                            ReactUIServiceImpl.this.dataContextFactory);

                    @Override
                    public Paragraph newParagraph()
                    {
                        return new ParagraphImpl(this.context);
                    }

                    @Override
                    public Button newButton()
                    {
                        return new ButtonImpl(this.context);
                    }

                    @Override
                    public Anker newAnker()
                    {
                        return new AnkerImpl(this.context);
                    }

                    @Override
                    public AnkerButton newAnkerButton()
                    {
                        return new AnkerButtonImpl(this.context);
                    }

                    @Override
                    public BlockQuote newBlockQuote()
                    {
                        return new BlockQuoteImpl(this.context);
                    }

                    @Override
                    public Card newCard()
                    {
                        return new CardImpl(this.context);
                    }

                    @Override
                    public Table newTable()
                    {
                        return new TableImpl(this.context);
                    }

                    @Override
                    public Composite newComposite()
                    {
                        return new CompositeImpl(this.context);
                    }

                    @Override
                    public GridContainer newGridContainer()
                    {
                        return new GridContainerImpl(this.context);
                    }

                    @Override
                    public NavigationBar newNavigationBar()
                    {
                        return new NavigationBarImpl(this.context);
                    }

                    @Override
                    public Form newForm()
                    {
                        return new FormImpl(this.context);
                    }

                    @Override
                    public Image newImage()
                    {
                        return new ImageImpl(this.context);
                    }

                    @Override
                    public Heading newHeading()
                    {
                        return new HeadingImpl(this.context);
                    }

                    @Override
                    public Jumbotron newJumboTron()
                    {
                        return new JumbotronImpl(this.context);
                    }

                    @Override
                    public UnsortedList newUnsortedList()
                    {
                        return new UnsortedListImpl(this.context);
                    }

                    @Override
                    public ImageIndex newImageIndex()
                    {
                        return new ImageIndexImpl(this.context);
                    }

                    @Override
                    public VerticalContentSwitcher newVerticalContentSwitcher()
                    {
                        return new VerticalContentSwitcherImpl(this.context);
                    }

                    @Override
                    public ScrollbarContainer newScrollbarContainer()
                    {
                        return new ScrollbarContainerImpl(this.context);
                    }

                    @Override
                    public Text newText()
                    {
                        return new TextImpl(this.context);
                    }

                    @Override
                    public List<UIComponent<?>> newMarkdownText(String markdown)
                    {
                        return this.parseMarkdownElements(MarkdownUtils.parse(markdown, options -> options.enableWrapIntoParagraphs())
                                                                       .get());
                    }

                    @Override
                    public List<UIComponent<?>> newMarkdownTextFromContent(String identifier)
                    {
                        return this.newMarkdownText(ReactUIServiceImpl.this.contentService.findContentMarkdownFile(identifier)
                                                                                          .map(ContentFile::asString)
                                                                                          .orElse(""));
                    }

                    @Override
                    public List<Card> newMarkdownCardsFromContent(String identifier)
                    {
                        return this.newMarkdownCards(ReactUIServiceImpl.this.contentService.findContentMarkdownFile(identifier)
                                                                                           .map(ContentFile::asString)
                                                                                           .orElse(""));
                    }

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
                                                                Card card = this.newCard();

                                                                //
                                                                BiElement<Optional<Element>, Stream<Element>> titleAndText = StreamUtils.splitOne(group);
                                                                Optional<String> title = titleAndText.getFirst()
                                                                                                     .map(Element::asHeading)
                                                                                                     .filter(Optional::isPresent)
                                                                                                     .map(Optional::get)
                                                                                                     .map(MarkdownUtils.Heading::getText)
                                                                                                     .filter(StringUtils::isNotBlank);

                                                                //
                                                                Optional<String> imageName = title.map(iTitle -> iTitle.replaceAll("[^a-zA-Z0-9]+", "_"))
                                                                                                  .map(String::toLowerCase);
                                                                Optional<ContentImage> firstImageNameMatch = imageName.flatMap(name -> ReactUIServiceImpl.this.contentService.findImages(name
                                                                        + "\\.(jpg)|(png)|(svg)")
                                                                                                                                                                             .findFirst());
                                                                if (firstImageNameMatch.isPresent())
                                                                {
                                                                    card.withImage(image -> image.withImage(firstImageNameMatch.get()
                                                                                                                               .getImagePath())
                                                                                                 .withName(title.orElse(null)));
                                                                }

                                                                //
                                                                Predicate<Element> firstImageFilter = this.createFirstImageAsCardImageFilter(card);
                                                                return Stream.of(card.withTitle(title.orElse(null))
                                                                                     .withLinkLocator(RegExUtils.replaceAll(StringUtils.lowerCase(title.orElse(null)),
                                                                                                                            "[^a-zA-Z]+", "_"))
                                                                                     .withContent(this.newComposite()
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

                    private List<UIComponent<?>> parseMarkdownElements(Stream<Element> elements)
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
                                                                                                                           .map(heading -> this.newParagraph()
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
                            return this.newUnsortedList()
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

                    private Function<MarkdownUtils.Table, Table> createMarkdownTableMapper(AtomicInteger referenceLinkCounter)
                    {
                        return markdownTable -> this.newTable()
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
                        return markdownText -> this.newText()
                                                   .addText(markdownText.getValue());
                    }

                    @Override
                    public Card newMarkdownCard(String markdown)
                    {
                        return ListUtils.first(this.newMarkdownCards(markdown));
                    }

                    @Override
                    public Card newMarkdownCardFromContent(String identifier)
                    {
                        return this.newMarkdownCard(ReactUIServiceImpl.this.contentService.findContentMarkdownFile(identifier)
                                                                                          .map(ContentFile::asString)
                                                                                          .orElse(""));
                    }

                    @Override
                    public MarkdownComponentChoice newMarkdown()
                    {
                        return new MarkdownComponentChoice()
                        {

                            @Override
                            public MarkdownComponentFactory<List<UIComponent<?>>> texts()
                            {
                                return new AbstractMarkdownComponentFactory<List<UIComponent<?>>>(ReactUIServiceImpl.this.contentService)
                                {
                                    @Override
                                    public List<UIComponent<?>> from(String markdown)
                                    {
                                        return newMarkdownText(markdown);
                                    }
                                };
                            }

                            @Override
                            public MarkdownComponentFactory<List<Card>> cards()
                            {
                                return new AbstractMarkdownComponentFactory<List<Card>>(ReactUIServiceImpl.this.contentService)
                                {
                                    @Override
                                    public List<Card> from(String markdown)
                                    {
                                        return newMarkdownCards(markdown);
                                    }
                                };
                            }

                            @Override
                            public MarkdownComponentFactory<Card> card()
                            {
                                return new AbstractMarkdownComponentFactory<Card>(ReactUIServiceImpl.this.contentService)
                                {
                                    @Override
                                    public Card from(String markdown)
                                    {
                                        return newMarkdownCard(markdown);
                                    }
                                };
                            }
                        };
                    }

                    private Function<MarkdownUtils.Paragraph, Paragraph> createMarkdownParagraphMapper(AtomicInteger referenceLinkCounter)
                    {
                        boolean removeLeadingPipe = false;
                        return this.createMarkdownParagraphMapper(referenceLinkCounter, removeLeadingPipe);
                    }

                    private Function<MarkdownUtils.Paragraph, Paragraph> createMarkdownParagraphMapper(AtomicInteger referenceLinkCounter,
                                                                                                       boolean removeLeadingPipe)
                    {
                        return markdownParagraph ->
                        {
                            Paragraph paragraph = this.newParagraph();
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
                                                        .ifPresent(image -> paragraph.addImage(image.getLabel(), image.getLink()));
                                                 element.asLineBreak()
                                                        .ifPresent(lineBreak -> paragraph.addLineBreak());
                                                 element.asLink()
                                                        .ifPresent(link ->
                                                        {
                                                            Optional<Match> buttonMatch = MatcherUtils.matcher()
                                                                                                      .ofRegEx("^BUTTON(\\:([a-zA-Z]+))?\\:(.*)")
                                                                                                      .findInAnd(link.getLabel())
                                                                                                      .getFirst();
                                                            Optional<Match> iframeMatch = MatcherUtils.matcher()
                                                                                                      .ofRegEx("^IFRAME\\:(.*)")
                                                                                                      .findInAnd(link.getLabel())
                                                                                                      .getFirst();
                                                            Optional<Match> iframeVideoMatch = MatcherUtils.matcher()
                                                                                                           .ofRegEx("^IFRAME\\:VIDEO(\\_[x0-9]+)?\\:(.*)")
                                                                                                           .findInAnd(link.getLabel())
                                                                                                           .getFirst();
                                                            Optional<Match> referenceLinkMatch = MatcherUtils.matcher()
                                                                                                             .ofRegEx("\\[\\?\\]")
                                                                                                             .findInAnd(link.getLabel())
                                                                                                             .getFirst();
                                                            if (buttonMatch.isPresent())
                                                            {
                                                                paragraph.addLinkButton(anker ->
                                                                {
                                                                    String text = buttonMatch.get()
                                                                                             .getSubGroup(3)
                                                                                             .orElse("");
                                                                    String style = buttonMatch.get()
                                                                                              .getSubGroup(2)
                                                                                              .orElse(null);
                                                                    anker.withText(text)
                                                                         .withLink(link.getLink())
                                                                         .withStyle(Style.of(style)
                                                                                         .orElse(Style.PRIMARY));
                                                                });
                                                            }
                                                            else if (iframeVideoMatch.isPresent())
                                                            {
                                                                String ratio = iframeVideoMatch.get()
                                                                                               .getSubGroup(1)
                                                                                               .orElse("");
                                                                String title = iframeVideoMatch.get()
                                                                                               .getSubGroup(2)
                                                                                               .orElse("");
                                                                paragraph.addComponent(this.newSizedContainer()
                                                                                           .withFullWidth()
                                                                                           .withHeightInViewPortRatio(0.8)
                                                                                           .withContent(this.newRatioContainer()
                                                                                                            .withRatio(EnumUtils.toEnumValue(ratio, Ratio.class)
                                                                                                                                .orElse(Ratio._16x9))
                                                                                                            .withContent(this.newIFrame()
                                                                                                                             .withSourceLink(link.getLink())
                                                                                                                             .withTitle(title)
                                                                                                                             .allowFullScreen())));
                                                            }
                                                            else if (iframeMatch.isPresent())
                                                            {
                                                                String title = iframeMatch.get()
                                                                                          .getSubGroup(1)
                                                                                          .orElse("");
                                                                paragraph.addComponent(this.newIFrame()
                                                                                           .withTitle(title)
                                                                                           .withSourceLink(link.getLink()));
                                                            }
                                                            else if (referenceLinkMatch.isPresent())
                                                            {
                                                                paragraph.addLink(anker -> anker.withText("[" + referenceLinkCounter.getAndIncrement() + "]")
                                                                                                .withLink(link.getLink()));
                                                            }
                                                            else
                                                            {
                                                                paragraph.addLink(anker -> anker.withText(link.getLabel())
                                                                                                .withLink(link.getLink()));
                                                            }
                                                        });
                                             });
                            return paragraph;
                        };
                    }

                    @Override
                    public LineBreak newLineBreak()
                    {
                        return new LineBreakImpl(this.context);
                    }

                    @Override
                    public Toaster newToaster()
                    {
                        return new ToasterImpl(this.context);
                    }

                    @Override
                    public Icon newIcon()
                    {
                        return new IconImpl(this.context);
                    }

                    @Override
                    public PaddingContainer newPaddingContainer()
                    {
                        return new PaddingContainerImpl(this.context);
                    }

                    @Override
                    public TextAlignmentContainer newTextAlignmentContainer()
                    {
                        return new TextAlignmentContainerImpl(this.context);
                    }

                    @Override
                    public RerenderingContainer newRerenderingContainer()
                    {
                        return new RerenderingContainerImpl(this.context);
                    }

                    @Override
                    public IntervalRerenderingContainer newIntervalRerenderingContainer()
                    {
                        return new IntervalRerenderingContainerImpl(this.context);
                    }

                    @Override
                    public ProgressBar newProgressBar()
                    {
                        return new ProgressBarImpl(this.context);
                    }

                    @Override
                    public IFrame newIFrame()
                    {
                        return new IFrameImpl(this.context);
                    }

                    @Override
                    public RatioContainer newRatioContainer()
                    {
                        return new RatioContainerImpl(this.context);
                    }

                    @Override
                    public SizedContainer newSizedContainer()
                    {
                        return new SizedContainerImpl(this.context);
                    }

                };
            }

            @Override
            public NodeHierarchy asNodeHierarchy()
            {
                RenderingProcessor renderingProcessor = this.createRenderingProcessor();
                List<Node> elements = this.components.stream()
                                                     .map(component -> renderingProcessor.process(component, Location.empty(), Optional.empty()))
                                                     .collect(Collectors.toList());
                return new NodeHierarchy(new HomePageNode().setNavigation(Optional.ofNullable(this.navigationBar)
                                                                                  .map(nb -> renderingProcessor.process(nb, Location.empty(), Optional.empty()))
                                                                                  .orElse(null))
                                                           .setBody(new CompositeNode().setElements(elements)));
            }

            @Override
            public ReactUIInternal collectNodeRenderers(NodeRendererRegistry registry)
            {
                FilterMapper<UIComponent<?>, RenderableUIComponent<?>> filterMapper = StreamUtils.filterMapper(iComponent -> iComponent instanceof RenderableUIComponent,
                                                                                                               iComponent -> (RenderableUIComponent<?>) iComponent);
                StreamUtils.recursiveFlattened(Stream.concat(Stream.of(this.navigationBar), this.components.stream())
                                                     .filter(filterMapper)
                                                     .map(filterMapper),
                                               component -> component.asRenderer()
                                                                     .getSubComponents(null)
                                                                     .map(ParentLocationAndComponent::getComponent)
                                                                     .filter(filterMapper)
                                                                     .map(filterMapper))
                           .forEach(component -> component.asRenderer()
                                                          .manageNodeRenderers(registry));
                return this;
            }

            @SuppressWarnings("rawtypes")
            @Override
            public ReactUIInternal collectEventHandlers(EventHandlerRegistry registry)
            {
                FilterMapper<UIComponent<?>, RenderableUIComponent<?>> filterMapper = StreamUtils.filterMapper(iComponent -> iComponent instanceof RenderableUIComponent,
                                                                                                               iComponent -> (RenderableUIComponent<?>) iComponent);
                StreamUtils.recursiveFlattened(Stream.concat(Stream.of(this.navigationBar), this.components.stream())
                                                     .filter(filterMapper)
                                                     .map(filterMapper)
                                                     .map(component -> BiElement.of((RenderableUIComponent) component, component.asRenderer()
                                                                                                                                .getLocation(new LocationSupportImpl(Location.empty())))),
                                               parentComponentAndLocation -> parentComponentAndLocation.getFirst()
                                                                                                       .asRenderer()
                                                                                                       .getSubComponents(parentComponentAndLocation.getSecond())
                                                                                                       .filter(locationAndComponent -> filterMapper.test(locationAndComponent.getComponent()))
                                                                                                       .map(locationAndComponent -> locationAndComponent.applyToComponent(filterMapper))
                                                                                                       .map(locationAndComponent -> BiElement.of(locationAndComponent.getSecond(),
                                                                                                                                                 locationAndComponent.getSecond()
                                                                                                                                                                     .asRenderer()
                                                                                                                                                                     .getLocation(new LocationSupportImpl(locationAndComponent.getFirst())))))
                           .forEach(componentAndLocation ->
                           {
                               Location location = componentAndLocation.getSecond();
                               RenderableUIComponent component = componentAndLocation.getFirst();
                               component.asRenderer()
                                        .manageEventHandler(new EventHandlerRegistrationSupportImpl(ReactUIServiceImpl.this.eventHandlerRegistry,
                                                                                                    ReactUIServiceImpl.this.rerenderingNodeProviderRegistry,
                                                                                                    location, Target.from(location),
                                                                                                    this.createRenderingProcessor(), component));
                           });
                return this;
            }

            private RenderingProcessor createRenderingProcessor()
            {
                return new RenderingProcessorImpl(this.componentFactory());
            }

            @Override
            public ReactUI configureHomePage(Consumer<HomePageConfiguration> configurationConsumer)
            {
                ReactUIServiceImpl.this.configureHomePage(configurationConsumer);
                return this;
            }
        };
    }

    @Override
    public NodeHierarchy resolveNodeHierarchy(String contextPath)
    {
        return this.uiManager.get(contextPath)
                             .map(ReactUIInternal::asNodeHierarchy)
                             .orElseThrow(() -> new IllegalArgumentException("No UI defined for context path: " + contextPath));
    }

    @Override
    public NodeHierarchy resolveDefaultNodeHierarchy()
    {
        return this.resolveNodeHierarchy(DEFAULT_CONTEXT_PATH);
    }

    @Override
    public String renderDefaultNodeHierarchyAsStatic(NodeRenderType nodeRenderType)
    {
        return this.renderDefaultNodeHierarchyAsStatic(nodeRenderType, DEFAULT_CONTEXT_PATH);
    }

    public String renderDefaultNodeHierarchyAsStatic(NodeRenderType nodeRenderType, String contextPath)
    {
        NodeHierarchyRenderingProcessor nodeRenderingProcessor = this.nodeHierarchyStaticRenderer.newNodeRenderingProcessor();
        this.uiManager.get(contextPath)
                      .orElseThrow(() -> new IllegalArgumentException("No UI defined for context path: " + contextPath))
                      .collectNodeRenderers(nodeRenderingProcessor)
                      .collectEventHandlers(this.eventHandlerRegistry);

        return nodeRenderingProcessor.render(this.resolveNodeHierarchy(contextPath), nodeRenderType);
    }

    @Override
    public ReactUIService configureHomePage(Consumer<HomePageConfiguration> configurationConsumer)
    {
        Optional.ofNullable(configurationConsumer)
                .ifPresent(consumer -> consumer.accept(this.homePageConfigurationService));
        return this;
    }

    protected abstract class AbstractMarkdownComponentFactory<U> implements MarkdownComponentFactory<U>
    {
        private ContentService contentService;

        public AbstractMarkdownComponentFactory(ContentService contentService)
        {
            super();
            this.contentService = contentService;
        }

        @Override
        public U fromContentFile(String identifier)
        {
            return this.from(ReactUIServiceImpl.this.contentService.findContentMarkdownFile(identifier)
                                                                   .map(ContentFile::asString)
                                                                   .orElse(""));
        }

    }
}
