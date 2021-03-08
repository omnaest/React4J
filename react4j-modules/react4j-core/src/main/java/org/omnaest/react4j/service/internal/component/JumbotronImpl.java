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

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.omnaest.react4j.domain.Card;
import org.omnaest.react4j.domain.GridContainer;
import org.omnaest.react4j.domain.Jumbotron;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.i18n.I18nText;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer;
import org.omnaest.react4j.domain.rendering.components.LocationSupport;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.domain.rendering.node.NodeRenderType;
import org.omnaest.react4j.domain.rendering.node.NodeRenderer;
import org.omnaest.react4j.domain.rendering.node.NodeRendererRegistry;
import org.omnaest.react4j.domain.rendering.node.NodeRenderingProcessor;
import org.omnaest.react4j.service.internal.nodes.JumbotronNode;
import org.omnaest.utils.template.TemplateUtils;

public class JumbotronImpl extends AbstractUIComponentAndContentHolder<Jumbotron> implements Jumbotron
{
    private I18nText       title;
    private I18nText       subTitle;
    private UIComponent<?> content;
    private boolean        fullWidth = false;

    public JumbotronImpl(ComponentContext context)
    {
        super(context);
    }

    @Override
    public UIComponentRenderer asRenderer()
    {
        return new UIComponentRenderer()
        {

            @Override
            public Location getLocation(LocationSupport locationSupport)
            {
                return locationSupport.createLocation(JumbotronImpl.this.getId());
            }

            @Override
            public Node render(RenderingProcessor renderingProcessor, Location location)
            {
                return new JumbotronNode().setTitle(JumbotronImpl.this.getTextResolver()
                                                                      .apply(JumbotronImpl.this.title, location))
                                          .setSubTitle(JumbotronImpl.this.getTextResolver()
                                                                         .apply(JumbotronImpl.this.subTitle, location))
                                          .setContent(renderingProcessor.process(JumbotronImpl.this.content, location))
                                          .setFullWidth(JumbotronImpl.this.fullWidth);
            }

            @Override
            public void manageNodeRenderers(NodeRendererRegistry registry)
            {
                registry.register(JumbotronNode.class, NodeRenderType.HTML, new NodeRenderer<JumbotronNode>()
                {
                    @Override
                    public String render(JumbotronNode node, NodeRenderingProcessor nodeRenderingProcessor)
                    {
                        return TemplateUtils.builder()
                                            .useTemplateClassResource(this.getClass(), "/render/templates/html/jumbotron.html")
                                            .add("title", nodeRenderingProcessor.render(node.getTitle()))
                                            .add("subTitle", nodeRenderingProcessor.render(node.getSubTitle()))
                                            .add("content", nodeRenderingProcessor.render(node.getContent()))
                                            .build()
                                            .get();
                    }
                });
            }

            @Override
            public void manageEventHandler(EventHandlerRegistrationSupport eventHandlerRegistrationSupport)
            {
            }

            @Override
            public Stream<UIComponent<?>> getSubComponents()
            {
                return Stream.of(JumbotronImpl.this.content);
            }

        };
    }

    @Override
    public Jumbotron withTitle(String title)
    {
        this.title = this.toI18nText(title);
        return this;
    }

    @Override
    public Jumbotron withSubTitle(String subTitle)
    {
        this.subTitle = this.toI18nText(subTitle);
        return this;
    }

    @Override
    public Jumbotron withFullWidth()
    {
        return this.withFullWidth(true);
    }

    @Override
    public Jumbotron withFullWidth(boolean fullWidth)
    {
        this.fullWidth = fullWidth;
        return this;
    }

    @Override
    public Jumbotron withContent(UIComponent<?> component)
    {
        this.content = component;
        return this;
    }

    @Override
    public Jumbotron withGridContent(Consumer<GridContainer> gridConsumer)
    {
        GridContainer grid = this.getUiComponentFactory()
                                 .newGridContainer();
        gridConsumer.accept(grid);
        return this.withContent(grid);
    }

    @Override
    public Jumbotron withCardsContent(Card... cards)
    {
        return this.withCardsContent(Arrays.asList(cards));
    }

    @Override
    public Jumbotron withCardsContent(List<Card> cards)
    {
        return this.withGridContent(grid -> grid.addRow(row -> row.addCells(cards.stream(), (cell, card) -> cell.withColumnSpan(12 / cards.size())
                                                                                                                .withContent(card))));
    }

}
