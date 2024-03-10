package org.omnaest.react4j.service.internal.service.internal;

import java.util.List;

import org.omnaest.react4j.domain.Anker;
import org.omnaest.react4j.domain.AnkerButton;
import org.omnaest.react4j.domain.BlockQuote;
import org.omnaest.react4j.domain.Button;
import org.omnaest.react4j.domain.Card;
import org.omnaest.react4j.domain.Composite;
import org.omnaest.react4j.domain.Form;
import org.omnaest.react4j.domain.GridContainer;
import org.omnaest.react4j.domain.Heading;
import org.omnaest.react4j.domain.IFrame;
import org.omnaest.react4j.domain.Icon;
import org.omnaest.react4j.domain.Image;
import org.omnaest.react4j.domain.ImageIndex;
import org.omnaest.react4j.domain.IntervalRerenderingContainer;
import org.omnaest.react4j.domain.Jumbotron;
import org.omnaest.react4j.domain.LineBreak;
import org.omnaest.react4j.domain.NativeHtml;
import org.omnaest.react4j.domain.NavigationBar;
import org.omnaest.react4j.domain.PaddingContainer;
import org.omnaest.react4j.domain.Paragraph;
import org.omnaest.react4j.domain.ProgressBar;
import org.omnaest.react4j.domain.RatioContainer;
import org.omnaest.react4j.domain.RerenderingContainer;
import org.omnaest.react4j.domain.SVGContainer;
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
import org.omnaest.react4j.domain.i18n.UILocale;
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
import org.omnaest.react4j.service.internal.component.NativeHtmlImpl;
import org.omnaest.react4j.service.internal.component.NavigationBarImpl;
import org.omnaest.react4j.service.internal.component.PaddingContainerImpl;
import org.omnaest.react4j.service.internal.component.ParagraphImpl;
import org.omnaest.react4j.service.internal.component.ProgressBarImpl;
import org.omnaest.react4j.service.internal.component.RatioContainerImpl;
import org.omnaest.react4j.service.internal.component.RerenderingContainerImpl;
import org.omnaest.react4j.service.internal.component.SVGContainerImpl;
import org.omnaest.react4j.service.internal.component.ScrollbarContainerImpl;
import org.omnaest.react4j.service.internal.component.SizedContainerImpl;
import org.omnaest.react4j.service.internal.component.TableImpl;
import org.omnaest.react4j.service.internal.component.TextAlignmentContainerImpl;
import org.omnaest.react4j.service.internal.component.TextImpl;
import org.omnaest.react4j.service.internal.component.ToasterImpl;
import org.omnaest.react4j.service.internal.component.UnsortedListImpl;
import org.omnaest.react4j.service.internal.component.VerticalContentSwitcherImpl;
import org.omnaest.react4j.service.internal.handler.EventHandlerRegistry;
import org.omnaest.react4j.service.internal.service.ContentService;
import org.omnaest.react4j.service.internal.service.ContentService.ContentFile;
import org.omnaest.react4j.service.internal.service.ContextFactory;
import org.omnaest.react4j.service.internal.service.LocalizedTextResolverService;
import org.omnaest.react4j.service.internal.service.MarkdownService;
import org.omnaest.react4j.service.internal.service.UIComponentFactoryService;
import org.omnaest.utils.ListUtils;
import org.omnaest.utils.element.cached.CachedElement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UIComponentFactoryServiceImpl implements UIComponentFactoryService
{
    @Autowired
    protected ContentService contentService;

    @Autowired
    protected MarkdownService markdownService;

    @Autowired
    protected LocalizedTextResolverService textResolver;

    @Autowired
    protected EventHandlerRegistry eventHandlerRegistry;

    @Autowired
    protected ContextFactory contextFactory;

    @Override
    public UIComponentFactory newInstanceFor(UILocale locale)
    {
        CachedElement<UIComponentFactory> factoryHolder = CachedElement.of(() -> null);
        UIComponentFactoryImpl uiComponentFactoryImpl = new UIComponentFactoryImpl(new ComponentContext(locale, this.textResolver, this.eventHandlerRegistry,
                                                                                                        factoryHolder, this.contextFactory),
                                                                                   this.contentService, this.markdownService);
        return factoryHolder.setAndGet(uiComponentFactoryImpl);
    }

    private static class UIComponentFactoryImpl implements UIComponentFactory
    {
        private ContentService   contentService;
        private MarkdownService  markdownService;
        private ComponentContext context;

        public UIComponentFactoryImpl(ComponentContext context, ContentService contentService, MarkdownService markdownService)
        {
            super();
            this.contentService = contentService;
            this.markdownService = markdownService;
            this.context = context;
        }

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
            return this.markdownService.interpreterWith(this)
                                       .parseMarkdownElements(markdown);
        }

        @Override
        public List<UIComponent<?>> newMarkdownTextFromContent(String identifier)
        {
            return this.newMarkdownText(this.contentService.findContentMarkdownFile(identifier)
                                                           .map(ContentFile::asString)
                                                           .orElse(""));
        }

        @Override
        public List<Card> newMarkdownCardsFromContent(String identifier)
        {
            return this.newMarkdownCards(this.contentService.findContentMarkdownFile(identifier)
                                                            .map(ContentFile::asString)
                                                            .orElse(""));
        }

        @Override
        public List<Card> newMarkdownCards(String markdown)
        {
            return this.markdownService.interpreterWith(this)
                                       .newMarkdownCards(markdown);
        }

        @Override
        public Card newMarkdownCard(String markdown)
        {
            return ListUtils.first(this.newMarkdownCards(markdown));
        }

        @Override
        public Card newMarkdownCardFromContent(String identifier)
        {
            return this.newMarkdownCard(this.contentService.findContentMarkdownFile(identifier)
                                                           .map(ContentFile::asString)
                                                           .orElse(""));
        }

        @Override
        public MarkdownComponentChoice newMarkdown()
        {
            ContentService contentService = this.contentService;
            return new MarkdownComponentChoice()
            {

                @Override
                public MarkdownComponentFactory<List<UIComponent<?>>> texts()
                {
                    return new AbstractMarkdownComponentFactory<List<UIComponent<?>>>(contentService)
                    {
                        @Override
                        public List<UIComponent<?>> from(String markdown)
                        {
                            return UIComponentFactoryImpl.this.newMarkdownText(markdown);
                        }
                    };
                }

                @Override
                public MarkdownComponentFactory<List<Card>> cards()
                {
                    return new AbstractMarkdownComponentFactory<List<Card>>(contentService)
                    {
                        @Override
                        public List<Card> from(String markdown)
                        {
                            return UIComponentFactoryImpl.this.newMarkdownCards(markdown);
                        }
                    };
                }

                @Override
                public MarkdownComponentFactory<Card> card()
                {
                    return new AbstractMarkdownComponentFactory<Card>(contentService)
                    {
                        @Override
                        public Card from(String markdown)
                        {
                            return UIComponentFactoryImpl.this.newMarkdownCard(markdown);
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
        public U fromContentFile(String identifier)
        {
            return this.from(this.contentService.findContentMarkdownFile(identifier)
                                                .map(ContentFile::asString)
                                                .orElse(""));
        }

    }
}
