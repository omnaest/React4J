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
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.NativeHtml;
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
import org.omnaest.react4j.service.internal.nodes.NativeHtmlNode;
import org.omnaest.utils.template.TemplateUtils;

public class NativeHtmlImpl extends AbstractUIComponent<NativeHtml> implements NativeHtml
{
    private Supplier<String> sourceProvider = () -> null;

    public NativeHtmlImpl(ComponentContext context)
    {
        super(context);
    }

    public NativeHtmlImpl(ComponentContext context, Supplier<String> sourceProvider)
    {
        super(context);
        this.sourceProvider = sourceProvider;
    }

    @Override
    public NativeHtml withSource(String source)
    {
        return this.withSource(() -> source);
    }

    public NativeHtml withSource(Supplier<String> sourceProvider)
    {
        this.sourceProvider = sourceProvider;
        return this;
    }

    @Override
    public UIComponentRenderer asRenderer()
    {
        return new UIComponentRenderer()
        {
            @Override
            public Location getLocation(LocationSupport locationSupport)
            {
                return locationSupport.createLocation(NativeHtmlImpl.this.getId());
            }

            @Override
            public Node render(RenderingProcessor renderingProcessor, Location location, Optional<Data> data)
            {
                return NativeHtmlNode.builder()
                                     .source(NativeHtmlImpl.this.sourceProvider.get())
                                     .build();
            }

            @Override
            public void manageNodeRenderers(NodeRendererRegistry registry)
            {
                registry.register(NativeHtmlNode.class, NodeRenderType.HTML, new NodeRenderer<NativeHtmlNode>()
                {
                    @Override
                    public String render(NativeHtmlNode node, NodeRenderingProcessor nodeRenderingProcessor)
                    {
                        return TemplateUtils.builder()
                                            .useTemplateClassResource(this.getClass(), "/render/templates/html/native_html.html")
                                            .add("source", node.getSource())
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
    public UIComponentProvider<NativeHtml> asTemplateProvider()
    {
        return () -> new NativeHtmlImpl(this.context, this.sourceProvider);
    }
}
