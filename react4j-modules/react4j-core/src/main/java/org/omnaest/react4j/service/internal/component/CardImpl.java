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

import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.omnaest.react4j.domain.Card;
import org.omnaest.react4j.domain.Image;
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
import org.omnaest.react4j.service.internal.nodes.CardNode;
import org.omnaest.react4j.service.internal.nodes.ImageNode;
import org.omnaest.utils.template.TemplateUtils;

public class CardImpl extends AbstractUIComponentAndContentHolder<Card> implements Card
{
    private I18nText        featuredTitle;
    private I18nText        title;
    private I18nText        subTitle;
    private Optional<Image> image  = Optional.empty();
    private String          locator;
    private UIComponent<?>  content;
    private boolean         adjust = false;

    public CardImpl(ComponentContext context)
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
                return locationSupport.createLocation(CardImpl.this.getId());
            }

            @Override
            public Node render(RenderingProcessor renderingProcessor, Location location)
            {
                return new CardNode().setFeaturedTitle(Optional.ofNullable(CardImpl.this.featuredTitle)
                                                               .map(featuredTitle -> CardImpl.this.getTextResolver()
                                                                                                  .apply(featuredTitle, location))
                                                               .orElse(null))
                                     .setTitle(Optional.ofNullable(CardImpl.this.title)
                                                       .map(title -> CardImpl.this.getTextResolver()
                                                                                  .apply(title, location))
                                                       .orElse(null))
                                     .setSubTitle(Optional.ofNullable(CardImpl.this.subTitle)
                                                          .map(subTitle -> CardImpl.this.getTextResolver()
                                                                                        .apply(subTitle, location))
                                                          .orElse(null))
                                     .setImage(CardImpl.this.image.map(image -> (ImageNode) renderingProcessor.process(image, location))
                                                                  .orElse(null))
                                     .setLocator(CardImpl.this.locator)
                                     .setAdjust(CardImpl.this.adjust)
                                     .setContent(Optional.ofNullable(CardImpl.this.content)
                                                         .map(content -> renderingProcessor.process(content, location))
                                                         .orElse(null));
            }

            @Override
            public void manageNodeRenderers(NodeRendererRegistry registry)
            {
                registry.register(CardNode.class, NodeRenderType.HTML, new NodeRenderer<CardNode>()
                {
                    @Override
                    public String render(CardNode node, NodeRenderingProcessor nodeRenderingProcessor)
                    {
                        return TemplateUtils.builder()
                                            .useTemplateClassResource(this.getClass(), "/render/templates/html/card.html")
                                            .add("locator", node.getLocator())
                                            .add("title", nodeRenderingProcessor.render(node.getTitle()))
                                            .add("content", Optional.ofNullable(node.getContent())
                                                                    .map(nodeRenderingProcessor::render)
                                                                    .orElse(""))
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
                return Stream.of(CardImpl.this.content);
            }

        };
    }

    @Override
    public Card withTitle(String title)
    {
        if (this.image.isPresent())
        {
            this.title = this.toI18nText(title);
        }
        else
        {
            this.featuredTitle = this.toI18nText(title);
        }
        return this;
    }

    @Override
    public Card withSubTitle(String subTitle)
    {
        this.subTitle = this.toI18nText(subTitle);
        return this;
    }

    @Override
    public Card withImage(Consumer<Image> imageConsumer)
    {
        Image newImage = this.getUiComponentFactory()
                             .newImage();
        imageConsumer.accept(newImage);
        this.image = Optional.of(newImage);

        if (this.featuredTitle != null && this.title == null)
        {
            this.title = this.featuredTitle;
            this.featuredTitle = null;
        }

        return this;
    }

    @Override
    public Card withLinkLocator(String locator)
    {
        this.locator = locator;
        return this;
    }

    @Override
    public Card withContent(UIComponent<?> component)
    {
        this.content = component;
        component.registerParent(this);
        return this;
    }

    @Override
    public Card withAdjustment(boolean value)
    {
        this.adjust = value;
        return this;
    }

}
