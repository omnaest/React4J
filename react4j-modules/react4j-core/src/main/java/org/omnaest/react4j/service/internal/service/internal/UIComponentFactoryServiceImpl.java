package org.omnaest.react4j.service.internal.service.internal;

import java.util.List;

import org.omnaest.react4j.component.anker.Anker;
import org.omnaest.react4j.component.ankerbutton.AnkerButton;
import org.omnaest.react4j.component.form.Form;
import org.omnaest.react4j.component.listview.ListView;
import org.omnaest.react4j.component.listview.internal.ListViewImpl;
import org.omnaest.react4j.component.master.MasterDetails;
import org.omnaest.react4j.component.master.internal.MasterDetailsImpl;
import org.omnaest.react4j.component.table.Table;
import org.omnaest.react4j.component.treetable.TreeTable;
import org.omnaest.react4j.domain.Accordion;
import org.omnaest.react4j.domain.Alert;
import org.omnaest.react4j.domain.Badge;
import org.omnaest.react4j.domain.BlockQuote;
import org.omnaest.react4j.domain.Breadcrumb;
import org.omnaest.react4j.domain.Button;
import org.omnaest.react4j.domain.Card;
import org.omnaest.react4j.domain.Carousel;
import org.omnaest.react4j.domain.Collapse;
import org.omnaest.react4j.domain.Composite;
import org.omnaest.react4j.domain.Dropdown;
import org.omnaest.react4j.domain.Figure;
import org.omnaest.react4j.domain.GridContainer;
import org.omnaest.react4j.domain.Heading;
import org.omnaest.react4j.domain.IFrame;
import org.omnaest.react4j.domain.Icon;
import org.omnaest.react4j.domain.Image;
import org.omnaest.react4j.domain.ImageIndex;
import org.omnaest.react4j.domain.IntervalRerenderingContainer;
import org.omnaest.react4j.domain.Jumbotron;
import org.omnaest.react4j.domain.LineBreak;
import org.omnaest.react4j.domain.Modal;
import org.omnaest.react4j.domain.NativeHtml;
import org.omnaest.react4j.domain.NavigationBar;
import org.omnaest.react4j.domain.Offcanvas;
import org.omnaest.react4j.domain.PaddingContainer;
import org.omnaest.react4j.domain.Pagination;
import org.omnaest.react4j.domain.Paragraph;
import org.omnaest.react4j.domain.Placeholder;
import org.omnaest.react4j.domain.Popover;
import org.omnaest.react4j.domain.ProgressBar;
import org.omnaest.react4j.domain.RatioContainer;
import org.omnaest.react4j.domain.RerenderingContainer;
import org.omnaest.react4j.domain.SVGContainer;
import org.omnaest.react4j.domain.ScrollbarContainer;
import org.omnaest.react4j.domain.SizedContainer;
import org.omnaest.react4j.domain.Spinner;
import org.omnaest.react4j.domain.SplitButton;
import org.omnaest.react4j.domain.Stack;
import org.omnaest.react4j.domain.Tabs;
import org.omnaest.react4j.domain.Text;
import org.omnaest.react4j.domain.TextAlignmentContainer;
import org.omnaest.react4j.domain.Toaster;
import org.omnaest.react4j.domain.ToggleButton;
import org.omnaest.react4j.domain.Tooltip;
import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.UIComponentFactory;
import org.omnaest.react4j.domain.UIComponentFactory.MarkdownComponentFactory;
import org.omnaest.react4j.domain.UnsortedList;
import org.omnaest.react4j.domain.markdown.MarkdownIssue;
import org.omnaest.react4j.domain.VerticalContentSwitcher;
import org.omnaest.react4j.domain.i18n.UILocale;
import org.omnaest.react4j.service.internal.component.AccordionImpl;
import org.omnaest.react4j.service.internal.component.AlertImpl;
import org.omnaest.react4j.service.internal.component.BadgeImpl;
import org.omnaest.react4j.service.internal.component.BlockQuoteImpl;
import org.omnaest.react4j.service.internal.component.BreadcrumbImpl;
import org.omnaest.react4j.service.internal.component.ButtonImpl;
import org.omnaest.react4j.service.internal.component.CardImpl;
import org.omnaest.react4j.service.internal.component.CarouselImpl;
import org.omnaest.react4j.service.internal.component.CollapseImpl;
import org.omnaest.react4j.service.internal.component.ComponentContext;
import org.omnaest.react4j.service.internal.component.CompositeImpl;
import org.omnaest.react4j.service.internal.component.DropdownImpl;
import org.omnaest.react4j.service.internal.component.FigureImpl;
import org.omnaest.react4j.service.internal.component.GridContainerImpl;
import org.omnaest.react4j.service.internal.component.HeadingImpl;
import org.omnaest.react4j.service.internal.component.IFrameImpl;
import org.omnaest.react4j.service.internal.component.IconImpl;
import org.omnaest.react4j.service.internal.component.ImageImpl;
import org.omnaest.react4j.service.internal.component.ImageIndexImpl;
import org.omnaest.react4j.service.internal.component.IntervalRerenderingContainerImpl;
import org.omnaest.react4j.service.internal.component.JumbotronImpl;
import org.omnaest.react4j.service.internal.component.LineBreakImpl;
import org.omnaest.react4j.service.internal.component.ModalImpl;
import org.omnaest.react4j.service.internal.component.NativeHtmlImpl;
import org.omnaest.react4j.service.internal.component.NavigationBarImpl;
import org.omnaest.react4j.service.internal.component.OffcanvasImpl;
import org.omnaest.react4j.service.internal.component.PaddingContainerImpl;
import org.omnaest.react4j.service.internal.component.PaginationImpl;
import org.omnaest.react4j.service.internal.component.ParagraphImpl;
import org.omnaest.react4j.service.internal.component.PlaceholderImpl;
import org.omnaest.react4j.service.internal.component.PopoverImpl;
import org.omnaest.react4j.service.internal.component.ProgressBarImpl;
import org.omnaest.react4j.service.internal.component.RatioContainerImpl;
import org.omnaest.react4j.service.internal.component.RerenderingContainerImpl;
import org.omnaest.react4j.service.internal.component.SVGContainerImpl;
import org.omnaest.react4j.service.internal.component.ScrollbarContainerImpl;
import org.omnaest.react4j.service.internal.component.SizedContainerImpl;
import org.omnaest.react4j.service.internal.component.SpinnerImpl;
import org.omnaest.react4j.service.internal.component.SplitButtonImpl;
import org.omnaest.react4j.service.internal.component.StackImpl;
import org.omnaest.react4j.service.internal.component.TabsImpl;
import org.omnaest.react4j.service.internal.component.TextAlignmentContainerImpl;
import org.omnaest.react4j.service.internal.component.TextImpl;
import org.omnaest.react4j.service.internal.component.ToasterImpl;
import org.omnaest.react4j.service.internal.component.ToggleButtonImpl;
import org.omnaest.react4j.service.internal.component.TooltipImpl;
import org.omnaest.react4j.service.internal.component.UnsortedListImpl;
import org.omnaest.react4j.service.internal.component.VerticalContentSwitcherImpl;
import org.omnaest.react4j.service.internal.handler.EventHandlerRegistry;
import org.omnaest.react4j.service.internal.service.ContentService;
import org.omnaest.react4j.service.internal.service.ContentService.ContentFile;
import org.omnaest.react4j.service.internal.service.ContextFactory;
import org.omnaest.react4j.service.internal.service.LocalizedTextResolverService;
import org.omnaest.react4j.service.internal.service.MarkdownService;
import org.omnaest.react4j.service.internal.service.UIComponentFactoryService;
import org.omnaest.react4j.service.internal.upload.UploadChannelRegistry;
import org.omnaest.utils.ListUtils;
import org.omnaest.utils.element.cached.CachedElement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
public class UIComponentFactoryServiceImpl implements UIComponentFactoryService
{
    @Autowired
    protected ContentService                  contentService;

    @Autowired
    protected MarkdownService                 markdownService;

    @Autowired
    protected LocalizedTextResolverService    textResolver;

    @Autowired
    protected EventHandlerRegistry            eventHandlerRegistry;

    @Autowired
    protected UploadChannelRegistry           uploadChannelRegistry;

    @Autowired
    protected ContextFactory                  contextFactory;

    @Autowired
    protected CustomUIComponentFactoryManager customUIComponentFactoryManager;

    @Override
    public UIComponentFactory newInstanceFor(UILocale locale)
    {
        CachedElement<UIComponentFactory> factoryHolder = CachedElement.of(() -> null);
        UIComponentFactoryImpl uiComponentFactoryImpl = new UIComponentFactoryImpl(new ComponentContext(locale, this.textResolver, this.eventHandlerRegistry,
                                                                                                        this.uploadChannelRegistry, factoryHolder,
                                                                                                        this.contextFactory),
                                                                                   this.contentService, this.markdownService,
                                                                                   this.customUIComponentFactoryManager);
        return factoryHolder.setAndGet(uiComponentFactoryImpl);
    }

    @RequiredArgsConstructor
    private static class UIComponentFactoryImpl implements UIComponentFactory
    {
        private final ComponentContext                context;
        private final ContentService                  contentService;
        private final MarkdownService                 markdownService;
        private final CustomUIComponentFactoryManager customUIComponentFactoryManager;

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
            return this.newComponent(Anker.class);
        }

        public <UIC extends UIComponent<UIC>> UIC newComponent(Class<UIC> type)
        {
            return this.customUIComponentFactoryManager.newInstance(type, this.context)
                                                       .get();
        }

        @Override
        public AnkerButton newAnkerButton()
        {
            return this.newComponent(AnkerButton.class);
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
            return this.newComponent(Table.class);
        }

        @Override
        public TreeTable newTreeTable()
        {
            return this.newComponent(TreeTable.class);
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
            return this.newComponent(Form.class);
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
            return this.newMarkdownText(markdown, null);
        }

        /**
         * @param markdown
         * @param source
         *            origin of the markdown, e.g. the content file identifier, which any found {@link MarkdownIssue} is attributed to. Can be null.
         * @return
         */
        protected List<UIComponent<?>> newMarkdownText(String markdown, String source)
        {
            return this.markdownService.interpreterWith(this)
                                       .withSource(source)
                                       .parseMarkdownElements(markdown);
        }

        @Override
        public List<UIComponent<?>> newMarkdownTextFromContent(String identifier)
        {
            return this.newMarkdownText(this.readContentMarkdownFile(identifier), identifier);
        }

        @Override
        public List<Card> newMarkdownCardsFromContent(String identifier)
        {
            return this.newMarkdownCards(this.readContentMarkdownFile(identifier), identifier);
        }

        @Override
        public List<Card> newMarkdownCards(String markdown)
        {
            return this.newMarkdownCards(markdown, null);
        }

        /**
         * @see #newMarkdownText(String, String)
         * @param markdown
         * @param source
         * @return
         */
        protected List<Card> newMarkdownCards(String markdown, String source)
        {
            return this.markdownService.interpreterWith(this)
                                       .withSource(source)
                                       .newMarkdownCards(markdown);
        }

        @Override
        public Card newMarkdownCard(String markdown)
        {
            return this.newMarkdownCard(markdown, null);
        }

        /**
         * @see #newMarkdownText(String, String)
         * @param markdown
         * @param source
         * @return
         */
        protected Card newMarkdownCard(String markdown, String source)
        {
            return ListUtils.first(this.newMarkdownCards(markdown, source));
        }

        @Override
        public Card newMarkdownCardFromContent(String identifier)
        {
            return this.newMarkdownCard(this.readContentMarkdownFile(identifier), identifier);
        }

        private String readContentMarkdownFile(String identifier)
        {
            return this.contentService.findContentMarkdownFile(identifier)
                                      .map(ContentFile::asString)
                                      .orElse("");
        }

        @Override
        public MarkdownComponentChoice newMarkdown()
        {
            ContentService contentService = this.contentService;
            return new MarkdownComponentChoice() {

                @Override
                public MarkdownComponentFactory<List<UIComponent<?>>> texts()
                {
                    return new AbstractMarkdownComponentFactory<List<UIComponent<?>>>(contentService) {
                        @Override
                        protected List<UIComponent<?>> from(String markdown, String source)
                        {
                            return UIComponentFactoryImpl.this.newMarkdownText(markdown, source);
                        }
                    };
                }

                @Override
                public MarkdownComponentFactory<List<Card>> cards()
                {
                    return new AbstractMarkdownComponentFactory<List<Card>>(contentService) {
                        @Override
                        protected List<Card> from(String markdown, String source)
                        {
                            return UIComponentFactoryImpl.this.newMarkdownCards(markdown, source);
                        }
                    };
                }

                @Override
                public MarkdownComponentFactory<Card> card()
                {
                    return new AbstractMarkdownComponentFactory<Card>(contentService) {
                        @Override
                        protected Card from(String markdown, String source)
                        {
                            return UIComponentFactoryImpl.this.newMarkdownCard(markdown, source);
                        }
                    };
                }
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
        public NativeHtml newNativeHtml()
        {
            return new NativeHtmlImpl(this.context);
        }

        @Override
        public SVGContainer newSVGContainer()
        {
            return new SVGContainerImpl(this.context);
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

        @Override
        public MasterDetails newMasterDetails()
        {
            return new MasterDetailsImpl(this.context);
        }

        @Override
        public ListView newListView()
        {
            return new ListViewImpl(this.context);
        }

        @Override
        public Badge newBadge()
        {
            return new BadgeImpl(this.context);
        }

        @Override
        public Spinner newSpinner()
        {
            return new SpinnerImpl(this.context);
        }

        @Override
        public Placeholder newPlaceholder()
        {
            return new PlaceholderImpl(this.context);
        }

        @Override
        public Alert newAlert()
        {
            return new AlertImpl(this.context);
        }

        @Override
        public Breadcrumb newBreadcrumb()
        {
            return new BreadcrumbImpl(this.context);
        }

        @Override
        public Pagination newPagination()
        {
            return new PaginationImpl(this.context);
        }

        @Override
        public Stack newStack()
        {
            return new StackImpl(this.context);
        }

        @Override
        public Figure newFigure()
        {
            return new FigureImpl(this.context);
        }

        @Override
        public Tabs newTabs()
        {
            return new TabsImpl(this.context);
        }

        @Override
        public Accordion newAccordion()
        {
            return new AccordionImpl(this.context);
        }

        @Override
        public Modal newModal()
        {
            return new ModalImpl(this.context);
        }

        @Override
        public Offcanvas newOffcanvas()
        {
            return new OffcanvasImpl(this.context);
        }

        @Override
        public Tooltip newTooltip()
        {
            return new TooltipImpl(this.context);
        }

        @Override
        public Popover newPopover()
        {
            return new PopoverImpl(this.context);
        }

        @Override
        public Collapse newCollapse()
        {
            return new CollapseImpl(this.context);
        }

        @Override
        public ToggleButton newToggleButton()
        {
            return new ToggleButtonImpl(this.context);
        }

        @Override
        public Dropdown newDropdown()
        {
            return new DropdownImpl(this.context);
        }

        @Override
        public SplitButton newSplitButton()
        {
            return new SplitButtonImpl(this.context);
        }

        @Override
        public Carousel newCarousel()
        {
            return new CarouselImpl(this.context);
        }

    }

    protected static abstract class AbstractMarkdownComponentFactory<U> implements MarkdownComponentFactory<U>
    {
        private ContentService contentService;

        public AbstractMarkdownComponentFactory(ContentService contentService)
        {
            super();
            this.contentService = contentService;
        }

        @Override
        public U from(String markdown)
        {
            String source = null;
            return this.from(markdown, source);
        }

        @Override
        public U fromContentFile(String identifier)
        {
            return this.from(this.contentService.findContentMarkdownFile(identifier)
                                                .map(ContentFile::asString)
                                                .orElse(""),
                             identifier);
        }

        /**
         * @param markdown
         * @param source
         *            origin of the markdown, e.g. the content file identifier, which any found {@link MarkdownIssue} is attributed to. Can be null.
         * @return
         */
        protected abstract U from(String markdown, String source);

    }
}
