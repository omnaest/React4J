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

import org.omnaest.react4j.domain.Accordion;
import org.omnaest.react4j.domain.Location;
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
import org.omnaest.react4j.service.internal.nodes.AccordionNode;
import org.omnaest.react4j.service.internal.service.LocalizedTextResolverService;
import org.omnaest.utils.MapperUtils;

public class AccordionImpl extends AbstractUIComponent<Accordion> implements Accordion
{
    private List<AccordionPanelImpl> panels = new ArrayList<>();
    private boolean                  alwaysOpen;

    public AccordionImpl(ComponentContext context)
    {
        super(context);
    }

    public AccordionImpl(ComponentContext context, List<AccordionPanelImpl> panels, boolean alwaysOpen)
    {
        super(context);
        this.panels = panels;
        this.alwaysOpen = alwaysOpen;
    }

    @Override
    public UIComponentRenderer asRenderer()
    {
        return new UIComponentRenderer() {

            @Override
            public Location getLocation(LocationSupport locationSupport)
            {
                return locationSupport.createLocation(AccordionImpl.this.getId());
            }

            @Override
            public Node render(RenderingProcessor renderingProcessor, Location location, Optional<Data> data)
            {
                LocalizedTextResolverService textResolver = AccordionImpl.this.getTextResolver();
                return new AccordionNode().setAlwaysOpen(AccordionImpl.this.alwaysOpen)
                                          .setPanels(AccordionImpl.this.panels.stream()
                                                                              .map(MapperUtils.withIntCounter())
                                                                              .map(panelAndIndex -> new AccordionNode.Panel().setTitle(textResolver.apply(panelAndIndex.getFirst()
                                                                                                                                                                       .getTitle(),
                                                                                                                                                          location))
                                                                                                                             .setExpanded(panelAndIndex.getFirst()
                                                                                                                                                       .isExpanded())
                                                                                                                             .setContent(Optional.ofNullable(panelAndIndex.getFirst()
                                                                                                                                                                          .getComponent())
                                                                                                                                                 .map(component -> renderingProcessor.process(component,
                                                                                                                                                                                              ChildLocationSupport.indexedChildLocation(location,
                                                                                                                                                                                                                                        panelAndIndex.getSecond())))
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
                return AccordionImpl.this.panels.stream()
                                                .map(MapperUtils.withIntCounter())
                                                .map(panelAndIndex -> ParentLocationAndComponent.of(ChildLocationSupport.indexedChildLocation(parentLocation,
                                                                                                                                              panelAndIndex.getSecond()),
                                                                                                    panelAndIndex.getFirst()
                                                                                                                 .getComponent()));
            }

        };
    }

    @Override
    public Accordion addPanel(Consumer<AccordionPanel> panelConsumer)
    {
        AccordionPanelImpl panel = new AccordionPanelImpl(this.getUiComponentFactory(), this.i18nTextMapper());
        panelConsumer.accept(panel);
        this.panels.add(panel);
        return this;
    }

    @Override
    public Accordion withAlwaysOpen(boolean alwaysOpen)
    {
        this.alwaysOpen = alwaysOpen;
        return this;
    }

    private static class AccordionPanelImpl extends AbstractUIContentHolder<AccordionPanel> implements AccordionPanel
    {
        private final Function<String, I18nText> i18nTextMapper;

        private UIComponent<?>                   component;
        private boolean                          expanded;
        private I18nText                         title;

        public AccordionPanelImpl(UIComponentFactory uiComponentFactory, Function<String, I18nText> i18nTextMapper)
        {
            super(uiComponentFactory);
            this.i18nTextMapper = i18nTextMapper;
        }

        @Override
        public AccordionPanel withTitle(String title)
        {
            this.title = this.i18nTextMapper.apply(title);
            return this;
        }

        @Override
        public AccordionPanel withExpandedState(boolean expanded)
        {
            this.expanded = expanded;
            return this;
        }

        @Override
        public AccordionPanel withContent(UIComponent<?> component)
        {
            this.component = component;
            return this;
        }

        public UIComponent<?> getComponent()
        {
            return this.component;
        }

        public boolean isExpanded()
        {
            return this.expanded;
        }

        public I18nText getTitle()
        {
            return this.title;
        }

    }

    @Override
    public UIComponentProvider<Accordion> asTemplateProvider()
    {
        return () -> new AccordionImpl(this.context, this.panels.stream()
                                                                .collect(Collectors.toList()),
                                       this.alwaysOpen);
    }

}
