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
import java.util.stream.Stream;

import org.omnaest.react4j.domain.AnkerButton;
import org.omnaest.react4j.domain.Button.Style;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.context.data.Data;
import org.omnaest.react4j.domain.i18n.I18nText;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer;
import org.omnaest.react4j.domain.rendering.components.LocationSupport;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.domain.rendering.node.NodeRenderType;
import org.omnaest.react4j.domain.rendering.node.NodeRenderer;
import org.omnaest.react4j.domain.rendering.node.NodeRendererRegistry;
import org.omnaest.react4j.domain.rendering.node.NodeRenderingProcessor;
import org.omnaest.react4j.domain.support.UIComponentProvider;
import org.omnaest.react4j.service.internal.nodes.AnkerButtonNode;
import org.omnaest.react4j.service.internal.nodes.AnkerButtonNode.Page;
import org.omnaest.utils.template.TemplateUtils;

public class AnkerButtonImpl extends AbstractUIComponent<AnkerButton> implements AnkerButton
{
    private I18nText text;
    private String   link;
    private Style    style      = Style.PRIMARY;
    private boolean  isSamePage = false;

    public AnkerButtonImpl(ComponentContext context)
    {
        super(context);
    }

    public AnkerButtonImpl(ComponentContext context, I18nText text, String link, Style style)
    {
        super(context);
        this.text = text;
        this.link = link;
        this.style = style;
    }

    @Override
    public UIComponentRenderer asRenderer()
    {
        return new UIComponentRenderer()
        {
            @Override
            public Location getLocation(LocationSupport locationSupport)
            {
                return locationSupport.createLocation(AnkerButtonImpl.this.getId());
            }

            @Override
            public Node render(RenderingProcessor renderingProcessor, Location location, Optional<Data> data)
            {
                return new AnkerButtonNode().setText(AnkerButtonImpl.this.getTextResolver()
                                                                         .apply(AnkerButtonImpl.this.text, location))
                                            .setLink(AnkerButtonImpl.this.link)
                                            .setStyle(AnkerButtonImpl.this.style.name()
                                                                                .toLowerCase())
                                            .setPage(AnkerButtonImpl.this.isSamePage ? Page.SELF : Page.BLANK);
            }

            @Override
            public void manageNodeRenderers(NodeRendererRegistry registry)
            {
                registry.register(AnkerButtonNode.class, NodeRenderType.HTML, new NodeRenderer<AnkerButtonNode>()
                {
                    @Override
                    public String render(AnkerButtonNode node, NodeRenderingProcessor nodeRenderingProcessor)
                    {
                        return TemplateUtils.builder()
                                            .useTemplateClassResource(this.getClass(), "/render/templates/html/anker_button.html")
                                            .add("link", node.getLink())
                                            .add("text", nodeRenderingProcessor.render(node.getText()))
                                            .build()
                                            .get();
                    }
                });
            }

            @Override
            public Stream<ParentLocationAndComponent> getSubComponents(Location parentLocation)
            {
                return Stream.empty();
            }

            @Override
            public void manageEventHandler(EventHandlerRegistrationSupport eventHandlerRegistrationSupport)
            {
            }

        };
    }

    @Override
    public AnkerButton withText(String text)
    {
        this.text = I18nText.of(this.getLocations(), text, this.getDefaultLocale());
        return this;
    }

    @Override
    public AnkerButton withLink(String link)
    {
        this.link = link;
        return this;
    }

    @Override
    public AnkerButton withStyle(Style style)
    {
        this.style = style;
        return this;
    }

    @Override
    public AnkerButton withLocator(String locator)
    {
        return this.withLink("#" + locator)
                   .whichOpensOnSamePage();
    }

    @Override
    public AnkerButton whichOpensOnSamePage()
    {
        this.isSamePage = true;
        return this;
    }

    @Override
    public UIComponentProvider<AnkerButton> asTemplateProvider()
    {
        return () -> new AnkerButtonImpl(this.context, this.text, this.link, this.style);
    }
}
