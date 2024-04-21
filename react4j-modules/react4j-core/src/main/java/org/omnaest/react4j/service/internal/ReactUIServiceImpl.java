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
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.NavigationBar;
import org.omnaest.react4j.domain.NavigationBar.NavigationBarConsumer;
import org.omnaest.react4j.domain.NavigationBar.NavigationBarProvider;
import org.omnaest.react4j.domain.ReactUI;
import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.UIComponentFactory;
import org.omnaest.react4j.domain.configuration.HomePageConfiguration;
import org.omnaest.react4j.domain.context.data.source.DataSource;
import org.omnaest.react4j.domain.context.data.source.registry.DataSourceRegistry;
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
import org.omnaest.react4j.service.internal.service.DataService;
import org.omnaest.react4j.service.internal.service.HomePageConfigurationService;
import org.omnaest.react4j.service.internal.service.MarkdownService;
import org.omnaest.react4j.service.internal.service.NodeHierarchyStaticRenderer;
import org.omnaest.react4j.service.internal.service.NodeHierarchyStaticRenderer.NodeHierarchyRenderingProcessor;
import org.omnaest.react4j.service.internal.service.ReactUIContextManager;
import org.omnaest.react4j.service.internal.service.ReactUIContextManager.ReactUIInternalProvider;
import org.omnaest.react4j.service.internal.service.UIComponentFactoryService;
import org.omnaest.react4j.service.internal.service.internal.LocationSupportImpl;
import org.omnaest.react4j.service.internal.service.internal.RenderingProcessorImpl;
import org.omnaest.utils.StreamUtils;
import org.omnaest.utils.element.bi.BiElement;
import org.omnaest.utils.stream.FilterMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ReactUIServiceImpl implements ReactUIService, RootNodeResolverService
{

    @Autowired
    protected EventHandlerRegistry eventHandlerRegistry;

    @Autowired
    protected DataService dataService;

    @Autowired
    protected RerenderingNodeProviderRegistry rerenderingNodeProviderRegistry;

    @Autowired
    protected HomePageConfigurationService homePageConfigurationService;

    @Autowired
    protected NodeHierarchyStaticRenderer nodeHierarchyStaticRenderer;

    @Autowired
    protected ContentService contentService;

    @Autowired
    protected MarkdownService markdownService;

    @Autowired
    protected UIComponentFactoryService uiComponentFactoryService;

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
                return ReactUIServiceImpl.this.uiComponentFactoryService.newInstanceFor(this.defaultLocale);
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

            @Override
            @SuppressWarnings("rawtypes")
            public ReactUIInternal collectDataSources(DataSourceRegistry registry)
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
                                        .manageDataSources(registry, location);
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
                             .orElseThrow(() -> new IllegalArgumentException("No UI defined for context path: " + contextPath))
                             .collectEventHandlers(this.eventHandlerRegistry)
                             .collectDataSources(this.createDataSourceRegistry())
                             .asNodeHierarchy();
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
                      .collectEventHandlers(this.eventHandlerRegistry)
                      .collectDataSources(this.createDataSourceRegistry());

        return nodeRenderingProcessor.render(this.resolveNodeHierarchy(contextPath), nodeRenderType);
    }

    private DataSourceRegistry createDataSourceRegistry()
    {
        return new DataSourceRegistry()
        {
            @Override
            public void register(Location location, DataSource dataSource)
            {
                if (dataSource != null && location != null)
                {
                    ReactUIServiceImpl.this.dataService.registerDataSource(Target.from(location), dataSource);
                }
            }
        };
    }

    @Override
    public ReactUIService configureHomePage(Consumer<HomePageConfiguration> configurationConsumer)
    {
        Optional.ofNullable(configurationConsumer)
                .ifPresent(consumer -> consumer.accept(this.homePageConfigurationService));
        return this;
    }

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
}
