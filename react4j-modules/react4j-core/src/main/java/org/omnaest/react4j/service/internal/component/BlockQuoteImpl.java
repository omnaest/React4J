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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.omnaest.react4j.domain.BlockQuote;
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
import org.omnaest.react4j.service.internal.nodes.BlockQuoteNode;
import org.omnaest.utils.template.TemplateUtils;

public class BlockQuoteImpl extends AbstractUIComponent implements BlockQuote
{
    private List<I18nText> texts = new ArrayList<>();
    private I18nText       footer;

    public BlockQuoteImpl(ComponentContext context)
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
                return locationSupport.createLocation(BlockQuoteImpl.this.getId());
            }

            @Override
            public Node render(RenderingProcessor renderingProcessor, Location location)
            {
                return new BlockQuoteNode().setTexts(BlockQuoteImpl.this.getTextResolver()
                                                                        .apply(BlockQuoteImpl.this.texts, location))
                                           .setFooter(BlockQuoteImpl.this.getTextResolver()
                                                                         .apply(BlockQuoteImpl.this.footer, location));
            }

            @Override
            public void manageNodeRenderers(NodeRendererRegistry registry)
            {
                registry.register(BlockQuoteNode.class, NodeRenderType.HTML, new NodeRenderer<BlockQuoteNode>()
                {
                    @Override
                    public String render(BlockQuoteNode node, NodeRenderingProcessor nodeRenderingProcessor)
                    {
                        return TemplateUtils.builder()
                                            .useTemplateClassResource(this.getClass(), "/render/templates/html/blockquote.html")
                                            .add("footer", nodeRenderingProcessor.render(node.getFooter()))
                                            .add("texts", node.getTexts()
                                                              .stream()
                                                              .map(nodeRenderingProcessor::render)
                                                              .collect(Collectors.toList()))
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
                return Stream.empty();
            }

        };
    }

    @Override
    public BlockQuote addText(String text)
    {
        this.texts.add(this.toI18nText(text));
        return this;
    }

    @Override
    public BlockQuote withFooter(String footer)
    {
        this.footer = this.toI18nText(footer);
        return this;
    }

}
