package org.omnaest.react4j.service.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.omnaest.react4j.domain.Anker;
import org.omnaest.react4j.domain.BlockQuote;
import org.omnaest.react4j.domain.Button;
import org.omnaest.react4j.domain.Card;
import org.omnaest.react4j.domain.Composite;
import org.omnaest.react4j.domain.ContainerGrid;
import org.omnaest.react4j.domain.Form;
import org.omnaest.react4j.domain.Heading;
import org.omnaest.react4j.domain.Image;
import org.omnaest.react4j.domain.ImageIndex;
import org.omnaest.react4j.domain.Jumbotron;
import org.omnaest.react4j.domain.NavigationBar;
import org.omnaest.react4j.domain.Paragraph;
import org.omnaest.react4j.domain.ReactUI;
import org.omnaest.react4j.domain.ScrollbarContainer;
import org.omnaest.react4j.domain.Table;
import org.omnaest.react4j.domain.Text;
import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.UIComponentFactory;
import org.omnaest.react4j.domain.UnsortedList;
import org.omnaest.react4j.domain.VerticalContentSwitcher;
import org.omnaest.react4j.domain.NavigationBar.NavigationBarConsumer;
import org.omnaest.react4j.domain.NavigationBar.NavigationBarProvider;
import org.omnaest.react4j.domain.i18n.UILocale;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.support.UIComponentFactoryFunction;
import org.omnaest.react4j.domain.support.UIComponentProvider;
import org.omnaest.react4j.service.ReactUIService;
import org.omnaest.react4j.service.internal.component.AnkerImpl;
import org.omnaest.react4j.service.internal.component.BlockQuoteImpl;
import org.omnaest.react4j.service.internal.component.ButtonImpl;
import org.omnaest.react4j.service.internal.component.CardImpl;
import org.omnaest.react4j.service.internal.component.ComponentContext;
import org.omnaest.react4j.service.internal.component.CompositeImpl;
import org.omnaest.react4j.service.internal.component.ContainerGridImpl;
import org.omnaest.react4j.service.internal.component.FormImpl;
import org.omnaest.react4j.service.internal.component.HeadingImpl;
import org.omnaest.react4j.service.internal.component.ImageImpl;
import org.omnaest.react4j.service.internal.component.ImageIndexImpl;
import org.omnaest.react4j.service.internal.component.JumbotronImpl;
import org.omnaest.react4j.service.internal.component.NavigationBarImpl;
import org.omnaest.react4j.service.internal.component.ParagraphImpl;
import org.omnaest.react4j.service.internal.component.ScrollbarContainerImpl;
import org.omnaest.react4j.service.internal.component.TableImpl;
import org.omnaest.react4j.service.internal.component.TextImpl;
import org.omnaest.react4j.service.internal.component.UnsortedListImpl;
import org.omnaest.react4j.service.internal.component.VerticalContentSwitcherImpl;
import org.omnaest.react4j.service.internal.domain.ReactUIInternal;
import org.omnaest.react4j.service.internal.handler.EventHandlerRegistry;
import org.omnaest.react4j.service.internal.nodes.CompositeNode;
import org.omnaest.react4j.service.internal.nodes.HomePageNode;
import org.omnaest.react4j.service.internal.nodes.NodeHierarchy;
import org.omnaest.react4j.service.internal.nodes.service.RootNodeResolverService;
import org.omnaest.react4j.service.internal.service.DataContextFactory;
import org.omnaest.react4j.service.internal.service.LocalizedTextResolverService;
import org.omnaest.react4j.service.internal.service.ReactUIContextManager;
import org.omnaest.react4j.service.internal.service.ReactUIContextManager.ReactUIInternalProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ReactUIServiceImpl implements ReactUIService, RootNodeResolverService
{
    @Autowired
    protected LocalizedTextResolverService textResolver;

    @Autowired
    protected EventHandlerRegistry eventHandlerRegistry;

    @Autowired
    protected DataContextFactory dataContextFactory;

    protected ReactUIContextManager uiManager      = new ReactUIContextManager();
    protected boolean               cachingEnabled = true;

    @Override
    public ReactUIService enableCaching(boolean active)
    {
        this.cachingEnabled = active;
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
        this.uiManager.computeIfAbsent(contextPath, ReactUIInternalProvider.fromFactory(this.cachingEnabled, () ->
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
        this.uiManager.putAndGet(contextPath, ReactUIInternalProvider.fromFactory(this.cachingEnabled, () ->
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
