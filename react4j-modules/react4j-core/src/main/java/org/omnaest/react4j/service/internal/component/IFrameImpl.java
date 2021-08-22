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

import org.omnaest.react4j.domain.IFrame;
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
import org.omnaest.react4j.service.internal.nodes.IFrameContainerNode;
import org.omnaest.utils.template.TemplateUtils;

public class IFrameImpl extends AbstractUIComponent<IFrame> implements IFrame
{
    private I18nText title;
    private String   sourceLink;
    private boolean  allowFullScreen = false;

    public IFrameImpl(ComponentContext context)
    {
        super(context);
    }

    public IFrameImpl(ComponentContext context, I18nText title, String sourceLink, boolean allowFullScreen)
    {
        super(context);
        this.title = title;
        this.sourceLink = sourceLink;
        this.allowFullScreen = allowFullScreen;
    }

    @Override
    public UIComponentRenderer asRenderer()
    {
        return new UIComponentRenderer()
        {
            @Override
            public Location getLocation(LocationSupport locationSupport)
            {
                return locationSupport.createLocation(IFrameImpl.this.getId());
            }

            @Override
            public Node render(RenderingProcessor renderingProcessor, Location location, Optional<Data> data)
            {
                return new IFrameContainerNode().setTitle(IFrameImpl.this.getTextResolver()
                                                                         .apply(IFrameImpl.this.title, location))
                                                .setSourceLink(IFrameImpl.this.sourceLink)
                                                .setAllowFullScreen(IFrameImpl.this.allowFullScreen);
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
    public IFrame withTitle(String title)
    {
        this.title = I18nText.of(this.getLocations(), title, this.getDefaultLocale());
        return this;
    }

    @Override
    public IFrame withNonTranslatedTitle(String title)
    {
        boolean isNonTranslatable = true;
        this.title = I18nText.of(this.getLocations(), title, this.getDefaultLocale(), isNonTranslatable);
        return this;
    }

    @Override
    public IFrame withSourceLink(String link)
    {
        this.sourceLink = link;
        return this;
    }

    @Override
    public UIComponentProvider<IFrame> asTemplateProvider()
    {
        return () -> new IFrameImpl(this.context, this.title, this.sourceLink, this.allowFullScreen);
    }

    @Override
    public IFrame allowFullScreen()
    {
        this.allowFullScreen = true;
        return this;
    }

    @Override
    public IFrame allowFullScreen(boolean value)
    {
        this.allowFullScreen = value;
        return this;
    }
}
