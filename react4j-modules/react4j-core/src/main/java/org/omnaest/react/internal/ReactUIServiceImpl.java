package org.omnaest.react.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import org.omnaest.react.ReactUIService;
import org.omnaest.react.domain.Anker;
import org.omnaest.react.domain.BlockQuote;
import org.omnaest.react.domain.Button;
import org.omnaest.react.domain.Card;
import org.omnaest.react.domain.Composite;
import org.omnaest.react.domain.ContainerGrid;
import org.omnaest.react.domain.Form;
import org.omnaest.react.domain.Heading;
import org.omnaest.react.domain.Image;
import org.omnaest.react.domain.ImageIndex;
import org.omnaest.react.domain.Jumbotron;
import org.omnaest.react.domain.NavigationBar;
import org.omnaest.react.domain.NavigationBar.NavigationBarConsumer;
import org.omnaest.react.domain.NavigationBar.NavigationBarProvider;
import org.omnaest.react.domain.Paragraph;
import org.omnaest.react.domain.ReactUI;
import org.omnaest.react.domain.ScrollbarContainer;
import org.omnaest.react.domain.Table;
import org.omnaest.react.domain.Text;
import org.omnaest.react.domain.UIComponent;
import org.omnaest.react.domain.UIComponentFactory;
import org.omnaest.react.domain.UnsortedList;
import org.omnaest.react.domain.VerticalContentSwitcher;
import org.omnaest.react.domain.i18n.UILocale;
import org.omnaest.react.domain.raw.Node;
import org.omnaest.react.domain.support.UIComponentFactoryFunction;
import org.omnaest.react.domain.support.UIComponentProvider;
import org.omnaest.react.internal.component.AnkerImpl;
import org.omnaest.react.internal.component.BlockQuoteImpl;
import org.omnaest.react.internal.component.ButtonImpl;
import org.omnaest.react.internal.component.CardImpl;
import org.omnaest.react.internal.component.ComponentContext;
import org.omnaest.react.internal.component.CompositeImpl;
import org.omnaest.react.internal.component.ContainerGridImpl;
import org.omnaest.react.internal.component.FormImpl;
import org.omnaest.react.internal.component.HeadingImpl;
import org.omnaest.react.internal.component.ImageImpl;
import org.omnaest.react.internal.component.ImageIndexImpl;
import org.omnaest.react.internal.component.JumbotronImpl;
import org.omnaest.react.internal.component.NavigationBarImpl;
import org.omnaest.react.internal.component.ParagraphImpl;
import org.omnaest.react.internal.component.ScrollbarContainerImpl;
import org.omnaest.react.internal.component.TableImpl;
import org.omnaest.react.internal.component.TextImpl;
import org.omnaest.react.internal.component.UnsortedListImpl;
import org.omnaest.react.internal.component.VerticalContentSwitcherImpl;
import org.omnaest.react.internal.domain.ReactUIInternal;
import org.omnaest.react.internal.handler.EventHandlerRegistry;
import org.omnaest.react.internal.nodes.CompositeNode;
import org.omnaest.react.internal.nodes.HomePageNode;
import org.omnaest.react.internal.nodes.NodeHierarchy;
import org.omnaest.react.internal.nodes.service.RootNodeResolverService;
import org.omnaest.react.internal.service.LocalizedTextResolverService;
import org.omnaest.react.internal.service.ReactUIContextManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ReactUIServiceImpl implements ReactUIService, RootNodeResolverService
{
    private static final String DEFAULT_CONTEXT_PATH = "/";

    @Autowired
    protected LocalizedTextResolverService textResolver;

    @Autowired
    protected EventHandlerRegistry eventHandlerRegistry;

    protected ReactUIContextManager uiManager = new ReactUIContextManager();

    @Override
    public ReactUI createDefaultRoot()
    {
        return this.createRoot(DEFAULT_CONTEXT_PATH);
    }

    @Override
    public ReactUI getOrCreateDefaultRoot()
    {
        return this.getOrCreateRoot(DEFAULT_CONTEXT_PATH);
    }

    @Override
    public ReactUI getOrCreateRoot(String contextPath)
    {
        return this.uiManager.computeIfAbsent(contextPath, () -> this.createRootInternal(contextPath));
    }

    @Override
    public ReactUI createRoot(String contextPath)
    {
        return this.uiManager.putAndGet(contextPath, () -> this.createRootInternal(contextPath));
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
                                                                            ReactUIServiceImpl.this.eventHandlerRegistry, this);

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
                    public ContainerGrid newContainerGrid()
                    {
                        return new ContainerGridImpl(this.context);
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

                };
            }

            @Override
            public NodeHierarchy asNodeHierarchy()
            {
                List<Node> elements = this.components.stream()
                                                     .map(component -> component.asRenderer()
                                                                                .render())
                                                     .collect(Collectors.toList());
                return new NodeHierarchy(new HomePageNode().setNavigation(Optional.ofNullable(this.navigationBar)
                                                                                  .map(nb -> nb.asRenderer()
                                                                                               .render())
                                                                                  .orElse(null))
                                                           .setBody(new CompositeNode().setElements(elements)));
            }
        };
    }

    @Override
    public NodeHierarchy resolveNodeHierarchy(String contextPath)
    {
        return this.uiManager.get(contextPath)
                             .map(ui -> ui.asNodeHierarchy())
                             .orElseThrow(() -> new IllegalArgumentException("No UI defined for context path: " + contextPath));
    }

    @Override
    public NodeHierarchy resolveDefaultNodeHierarchy()
    {
        return this.resolveNodeHierarchy(DEFAULT_CONTEXT_PATH);
    }

}
