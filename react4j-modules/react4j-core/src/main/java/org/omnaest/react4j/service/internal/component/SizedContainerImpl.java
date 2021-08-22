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

import org.apache.commons.lang3.StringUtils;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.SizedContainer;
import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.context.data.Data;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer;
import org.omnaest.react4j.domain.rendering.components.LocationSupport;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.domain.rendering.node.NodeRenderType;
import org.omnaest.react4j.domain.rendering.node.NodeRenderer;
import org.omnaest.react4j.domain.rendering.node.NodeRendererRegistry;
import org.omnaest.react4j.domain.rendering.node.NodeRenderingProcessor;
import org.omnaest.react4j.domain.support.UIComponentProvider;
import org.omnaest.react4j.service.internal.nodes.JumbotronNode;
import org.omnaest.react4j.service.internal.nodes.SizedContainerNode;
import org.omnaest.utils.NumberUtils;
import org.omnaest.utils.template.TemplateUtils;

public class SizedContainerImpl extends AbstractUIComponentAndContentHolder<SizedContainer> implements SizedContainer
{
    private UIComponent<?> content;
    private String         height = "auto";
    private String         width  = "auto";

    public SizedContainerImpl(ComponentContext context)
    {
        super(context);
    }

    public SizedContainerImpl(ComponentContext context, UIComponent<?> content, String height, String width)
    {
        super(context);
        this.content = content;
        this.height = height;
        this.width = width;
    }

    @Override
    public UIComponentRenderer asRenderer()
    {
        return new UIComponentRenderer()
        {

            @Override
            public Location getLocation(LocationSupport locationSupport)
            {
                return locationSupport.createLocation(SizedContainerImpl.this.getId());
            }

            @Override
            public Node render(RenderingProcessor renderingProcessor, Location location, Optional<Data> data)
            {
                return new SizedContainerNode().setContent(renderingProcessor.process(SizedContainerImpl.this.content, location))
                                               .setHeight(SizedContainerImpl.this.height)
                                               .setWidth(SizedContainerImpl.this.width);
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
            public Stream<ParentLocationAndComponent> getSubComponents(Location parentLocation)
            {
                return Stream.of(ParentLocationAndComponent.of(parentLocation, SizedContainerImpl.this.content));
            }

        };
    }

    @Override
    public SizedContainer withContent(UIComponent<?> component)
    {
        this.content = component;
        return this;
    }

    @Override
    public UIComponentProvider<SizedContainer> asTemplateProvider()
    {
        return () -> new SizedContainerImpl(this.context, this.content, this.height, this.width);
    }

    @Override
    public SizedContainer withFullWidth()
    {
        return this.withWidthRatio(1.0);
    }

    @Override
    public SizedContainer withFullHeight()
    {
        return this.withHeightRatio(1.0);
    }

    @Override
    public SizedContainer withWidthRatio(double ratio)
    {
        this.width = NumberUtils.formatter()
                                .asPercentage()
                                .format(ratio);
        return this;
    }

    @Override
    public SizedContainer withHeightRatio(double ratio)
    {
        this.height = NumberUtils.formatter()
                                 .asPercentage()
                                 .format(ratio);
        return this;
    }

    @Override
    public SizedContainer withWidthInPixel(int pixels)
    {
        this.width = pixels + "px";
        return this;
    }

    @Override
    public SizedContainer withHeightInPixel(int pixels)
    {
        this.height = pixels + "px";
        return this;
    }

    @Override
    public SizedContainer withWidthInViewPortRatio(double ratio)
    {
        this.width = StringUtils.removeEnd(NumberUtils.formatter()
                                                      .asPercentage()
                                                      .format(ratio),
                                           "%")
                + "vw";
        return this;
    }

    @Override
    public SizedContainer withHeightInViewPortRatio(double ratio)
    {
        this.height = StringUtils.removeEnd(NumberUtils.formatter()
                                                       .asPercentage()
                                                       .format(ratio),
                                            "%")
                + "vh";
        return this;
    }

}
