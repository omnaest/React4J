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
import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.UIComponentFactory;
import org.omnaest.react4j.domain.VerticalContentSwitcher;
import org.omnaest.react4j.domain.context.data.Data;
import org.omnaest.react4j.domain.i18n.I18nText;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer;
import org.omnaest.react4j.domain.rendering.components.LocationSupport;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.domain.rendering.node.NodeRendererRegistry;
import org.omnaest.react4j.domain.support.UIComponentProvider;
import org.omnaest.react4j.service.internal.nodes.VerticalContentSwitcherNode;
import org.omnaest.react4j.service.internal.service.LocalizedTextResolverService;

public class VerticalContentSwitcherImpl extends AbstractUIComponent<VerticalContentSwitcher> implements VerticalContentSwitcher
{
    private List<VerticalContentImpl> elements = new ArrayList<>();

    public VerticalContentSwitcherImpl(ComponentContext context)
    {
        super(context);
    }

    public VerticalContentSwitcherImpl(ComponentContext context, List<VerticalContentImpl> elements)
    {
        super(context);
        this.elements = elements;
    }

    @Override
    public UIComponentRenderer asRenderer()
    {
        return new UIComponentRenderer()
        {

            @Override
            public Location getLocation(LocationSupport locationSupport)
            {
                return locationSupport.createLocation(VerticalContentSwitcherImpl.this.getId());
            }

            @Override
            public Node render(RenderingProcessor renderingProcessor, Location location, Optional<Data> data)
            {
                LocalizedTextResolverService textResolver = VerticalContentSwitcherImpl.this.getTextResolver();
                return new VerticalContentSwitcherNode().setElements(VerticalContentSwitcherImpl.this.elements.stream()
                                                                                                              .map(element -> new VerticalContentSwitcherNode.ContentElement().setTitle(textResolver.apply(element.getTitle(),
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
                return VerticalContentSwitcherImpl.this.elements.stream()
                                                                .map(VerticalContentImpl::getComponent)
                                                                .map(component -> ParentLocationAndComponent.of(parentLocation, component));
            }

        };
    }

    @Override
    public VerticalContentSwitcher addContentEntry(Consumer<VerticalContent> contentConsumer)
    {
        VerticalContentImpl verticalContent = new VerticalContentImpl(this.getUiComponentFactory(), this.i18nTextMapper());
        contentConsumer.accept(verticalContent);
        this.elements.add(verticalContent);
        return this;
    }

    private static class VerticalContentImpl extends AbstractUIContentHolder<VerticalContent> implements VerticalContent
    {
        private final Function<String, I18nText> i18nTextMapper;

        private UIComponent<?> component;
        private State          state;
        private I18nText       title;

        public VerticalContentImpl(UIComponentFactory uiComponentFactory, Function<String, I18nText> i18nTextMapper)
        {
            super(uiComponentFactory);
            this.i18nTextMapper = i18nTextMapper;
        }

        @Override
        public VerticalContent withTitle(String title)
        {
            this.title = this.i18nTextMapper.apply(title);
            return this;
        }

        @Override
        public VerticalContent withState(State state)
        {
            this.state = state;
            return this;
        }

        @Override
        public VerticalContent withContent(UIComponent<?> component)
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
    public UIComponentProvider<VerticalContentSwitcher> asTemplateProvider()
    {
        return () -> new VerticalContentSwitcherImpl(this.context, this.elements.stream()
                                                                                .collect(Collectors.toList()));
    }

}
