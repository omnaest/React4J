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

import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.RatioContainer;
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
import org.omnaest.react4j.service.internal.nodes.RatioContainerNode;
import org.omnaest.utils.EnumUtils;
import org.omnaest.utils.template.TemplateUtils;

public class RatioContainerImpl extends AbstractUIComponentAndContentHolder<RatioContainer> implements RatioContainer
{
    private UIComponent<?> content;
    private Ratio          ratio = Ratio._16x9;

    public RatioContainerImpl(ComponentContext context)
    {
        super(context);
    }

    public RatioContainerImpl(ComponentContext context, UIComponent<?> content, Ratio ratio)
    {
        super(context);
        this.content = content;
        this.ratio = ratio;
    }

    @Override
    public UIComponentRenderer asRenderer()
    {
        return new UIComponentRenderer()
        {

            @Override
            public Location getLocation(LocationSupport locationSupport)
            {
                return locationSupport.createLocation(RatioContainerImpl.this.getId());
            }

            @Override
            public Node render(RenderingProcessor renderingProcessor, Location location, Optional<Data> data)
            {
                return new RatioContainerNode().setContent(renderingProcessor.process(RatioContainerImpl.this.content, location))
                                               .setRatio(EnumUtils.mapByName(RatioContainerImpl.this.ratio,
                                                                             org.omnaest.react4j.service.internal.nodes.RatioContainerNode.Ratio.class)
                                                                  .orElse(org.omnaest.react4j.service.internal.nodes.RatioContainerNode.Ratio._16x9));
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
                return Stream.of(ParentLocationAndComponent.of(parentLocation, RatioContainerImpl.this.content));
            }

        };
    }

    @Override
    public RatioContainer withRatio(Ratio ratio)
    {
        this.ratio = ratio;
        return this;
    }

    @Override
    public RatioContainer withContent(UIComponent<?> component)
    {
        this.content = component;
        return this;
    }

    @Override
    public UIComponentProvider<RatioContainer> asTemplateProvider()
    {
        return () -> new RatioContainerImpl(this.context, this.content, this.ratio);
    }

}
