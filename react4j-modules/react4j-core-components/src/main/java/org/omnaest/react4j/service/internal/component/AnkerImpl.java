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

import org.omnaest.react4j.domain.Anker;
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
import org.omnaest.react4j.service.internal.nodes.AnkerNode;
import org.omnaest.react4j.service.internal.nodes.AnkerNode.Page;
import org.omnaest.utils.template.TemplateUtils;

public class AnkerImpl extends AbstractUIComponent<Anker> implements Anker
{
    private I18nText text;
    private I18nText title;
    private String   link;
    private boolean  isSamePage = false;

    public AnkerImpl(ComponentContext context)
    {
        super(context);
    }

    public AnkerImpl(ComponentContext context, I18nText text, String link)
    {
        super(context);
        this.text = text;
        this.link = link;
    }

    @Override
    public UIComponentRenderer asRenderer()
    {
        return new UIComponentRenderer()
        {
            @Override
            public Location getLocation(LocationSupport locationSupport)
            {
                return locationSupport.createLocation(AnkerImpl.this.getId());
            }

            @Override
            public Node render(RenderingProcessor renderingProcessor, Location location, Optional<Data> data)
            {
                return new AnkerNode().setText(AnkerImpl.this.getTextResolver()
                                                             .apply(AnkerImpl.this.text, location))
                                      .setTitle(AnkerImpl.this.getTextResolver()
                                                              .apply(AnkerImpl.this.title, location))
                                      .setLink(AnkerImpl.this.link)
                                      .setPage(AnkerImpl.this.isSamePage ? Page.SELF : Page.BLANK);
            }

            @Override
            public void manageNodeRenderers(NodeRendererRegistry registry)
            {
                registry.register(AnkerNode.class, NodeRenderType.HTML, new NodeRenderer<AnkerNode>()
                {
                    @Override
                    public String render(AnkerNode node, NodeRenderingProcessor nodeRenderingProcessor)
                    {
                        return TemplateUtils.builder()
                                            .useTemplateClassResource(this.getClass(), "/render/templates/html/anker.html")
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
    public Anker withText(String text)
    {
        this.text = I18nText.of(this.getLocations(), text, this.getDefaultLocale());
        return this;
    }

    @Override
    public Anker withTitle(String title)
    {
        this.title = I18nText.of(this.getLocations(), title, this.getDefaultLocale());
        return this;
    }

    @Override
    public Anker withNonTranslatedTitle(String title)
    {
        boolean isNonTranslatable = true;
        this.title = I18nText.of(this.getLocations(), title, this.getDefaultLocale(), isNonTranslatable);
        return this;
    }

    @Override
    public Anker withNonTranslatedText(String text)
    {
        boolean isNonTranslatable = true;
        this.text = I18nText.of(this.getLocations(), text, this.getDefaultLocale(), isNonTranslatable);
        return this;
    }

    @Override
    public Anker withLink(String link)
    {
        this.link = link;
        return this;
    }

    @Override
    public Anker withLocator(String locator)
    {
        return this.withLink("#" + locator)
                   .whichOpensOnSamePage();
    }

    @Override
    public Anker whichOpensOnSamePage()
    {
        this.isSamePage = true;
        return this;
    }

    @Override
    public UIComponentProvider<Anker> asTemplateProvider()
    {
        return () -> new AnkerImpl(this.context, this.text, this.link);
    }
}
