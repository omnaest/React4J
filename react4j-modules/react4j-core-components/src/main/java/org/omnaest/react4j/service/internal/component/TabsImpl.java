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
package org.omnaest.react4j.service.internal.component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.Tabs;
import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.UIComponentFactory;
import org.omnaest.react4j.domain.context.data.Data;
import org.omnaest.react4j.domain.i18n.I18nText;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer;
import org.omnaest.react4j.domain.rendering.components.LocationSupport;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.domain.rendering.node.NodeRendererRegistry;
import org.omnaest.react4j.domain.support.UIComponentProvider;
import org.omnaest.react4j.service.internal.nodes.TabsNode;
import org.omnaest.react4j.service.internal.service.LocalizedTextResolverService;

public class TabsImpl extends AbstractUIComponent<Tabs> implements Tabs
{
    private List<TabImpl> elements = new ArrayList<>();

    public TabsImpl(ComponentContext context)
    {
        super(context);
    }

    public TabsImpl(ComponentContext context, List<TabImpl> elements)
    {
        super(context);
        this.elements = elements;
    }

    @Override
    public UIComponentRenderer asRenderer()
    {
        return new UIComponentRenderer() {

            @Override
            public Location getLocation(LocationSupport locationSupport)
            {
                return locationSupport.createLocation(TabsImpl.this.getId());
            }

            @Override
            public Node render(RenderingProcessor renderingProcessor, Location location, Optional<Data> data)
            {
                LocalizedTextResolverService textResolver = TabsImpl.this.getTextResolver();
                return new TabsNode().setElements(TabsImpl.this.elements.stream()
                                                                        .map(element -> new TabsNode.ContentElement().setTitle(textResolver.apply(element.getTitle(),
                                                                                                                                                  location))
                                                                                                                     .setActive(element.isActive())
                                                                                                                     .setDisabled(element.isDisabled())
                                                                                                                     .setContent(Optional.ofNullable(element.getComponent())
                                                                                                                                         .map(component -> renderingProcessor.process(component,
                                                                                                                                                                                      location))
                                                                                                                                         .orElse(null))

                                                                        )
                                                                        .collect(Collectors.toList()));
            }

            @Override
            public void manageNodeRenderers(NodeRendererRegistry registry)
            {
                // TODO Auto-generated method stub
            }

            @Override
            public void manageEventHandler(EventHandlerRegistrationSupport eventHandlerRegistrationSupport)
            {
            }

            @Override
            public Stream<ParentLocationAndComponent> getSubComponents(Location parentLocation)
            {
                return TabsImpl.this.elements.stream()
                                             .map(TabImpl::getComponent)
                                             .map(component -> ParentLocationAndComponent.of(parentLocation, component));
            }

        };
    }

    @Override
    public Tabs addTab(Consumer<Tab> tabConsumer)
    {
        TabImpl tab = new TabImpl(this.getUiComponentFactory(), this.i18nTextMapper());
        tabConsumer.accept(tab);
        this.elements.add(tab);
        return this;
    }

    private static class TabImpl extends AbstractUIContentHolder<Tab> implements Tab
    {
        private final Function<String, I18nText> i18nTextMapper;

        private UIComponent<?>                   component;
        private State                            state;
        private I18nText                         title;

        public TabImpl(UIComponentFactory uiComponentFactory, Function<String, I18nText> i18nTextMapper)
        {
            super(uiComponentFactory);
            this.i18nTextMapper = i18nTextMapper;
        }

        @Override
        public Tab withTitle(String title)
        {
            this.title = this.i18nTextMapper.apply(title);
            return this;
        }

        @Override
        public Tab withState(State state)
        {
            this.state = state;
            return this;
        }

        @Override
        public Tab withContent(UIComponent<?> component)
        {
            this.component = component;
            return this;
        }

        public UIComponent<?> getComponent()
        {
            return this.component;
        }

        public boolean isDisabled()
        {
            return State.DISABLED.equals(this.state);
        }

        public boolean isActive()
        {
            return State.ACTIVE.equals(this.state);
        }

        public I18nText getTitle()
        {
            return this.title;
        }

    }

    @Override
    public UIComponentProvider<Tabs> asTemplateProvider()
    {
        return () -> new TabsImpl(this.context, this.elements.stream()
                                                             .collect(Collectors.toList()));
    }

}
