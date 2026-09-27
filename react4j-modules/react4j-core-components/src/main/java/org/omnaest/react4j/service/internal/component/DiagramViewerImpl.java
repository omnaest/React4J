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

import org.omnaest.react4j.domain.DiagramViewer;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.context.data.Data;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer;
import org.omnaest.react4j.domain.rendering.components.LocationSupport;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.domain.rendering.node.NodeRenderType;
import org.omnaest.react4j.domain.rendering.node.NodeRendererRegistry;
import org.omnaest.react4j.domain.support.UIComponentProvider;
import org.omnaest.react4j.service.internal.nodes.DiagramViewerNode;

/**
 * @see DiagramViewer
 * @author omnaest
 */
public class DiagramViewerImpl extends AbstractUIComponent<DiagramViewer> implements DiagramViewer
{
    public static final String DEFAULT_MAX_HEIGHT = "60vh";
    public static final String DEFAULT_WIDTH      = "100%";

    private String             svg;
    private String             maxHeight          = DEFAULT_MAX_HEIGHT;
    private String             width              = DEFAULT_WIDTH;
    private String             height;
    private boolean            interactive        = true;

    public DiagramViewerImpl(ComponentContext context)
    {
        super(context);
    }

    public DiagramViewerImpl(ComponentContext context, String svg, String maxHeight, String width, String height, boolean interactive)
    {
        super(context);
        this.svg = svg;
        this.maxHeight = maxHeight;
        this.width = width;
        this.height = height;
        this.interactive = interactive;
    }

    @Override
    public DiagramViewer withSvg(String svg)
    {
        this.svg = svg;
        return this;
    }

    @Override
    public DiagramViewer withMaxHeight(String cssValue)
    {
        this.maxHeight = cssValue;
        return this;
    }

    @Override
    public DiagramViewer withWidth(String cssValue)
    {
        this.width = cssValue;
        return this;
    }

    @Override
    public DiagramViewer withHeight(String cssValue)
    {
        this.height = cssValue;
        return this;
    }

    @Override
    public DiagramViewer withInteractive(boolean interactive)
    {
        this.interactive = interactive;
        return this;
    }

    @Override
    public UIComponentRenderer asRenderer()
    {
        return new UIComponentRenderer() {

            @Override
            public Location getLocation(LocationSupport locationSupport)
            {
                return locationSupport.createLocation(DiagramViewerImpl.this.getId());
            }

            @Override
            public Node render(RenderingProcessor renderingProcessor, Location location, Optional<Data> data)
            {
                return new DiagramViewerNode().setSvg(DiagramViewerImpl.this.svg)
                                              .setMaxHeight(DiagramViewerImpl.this.maxHeight)
                                              .setWidth(DiagramViewerImpl.this.width)
                                              .setHeight(DiagramViewerImpl.this.height)
                                              .setInteractive(DiagramViewerImpl.this.interactive);
            }

            @Override
            public void manageNodeRenderers(NodeRendererRegistry registry)
            {
                registry.register(DiagramViewerNode.class, NodeRenderType.HTML,
                                  (node, nodeRenderingProcessor) -> "<div style=\"width:" + node.getWidth() + ";max-height:" + node.getMaxHeight()
                                                                    + (node.getHeight() != null ? ";height:" + node.getHeight() : "") + ";overflow:hidden;\">"
                                                                    + Optional.ofNullable(node.getSvg())
                                                                              .orElse("")
                                                                    + "</div>");
            }

            @Override
            public void manageEventHandler(EventHandlerRegistrationSupport eventHandlerRegistrationSupport)
            {
                // purely client-side interaction (zoom/pan) - there is no server EventHandler to register
            }

            @Override
            public Stream<ParentLocationAndComponent> getSubComponents(Location parentLocation)
            {
                return Stream.empty();
            }

        };
    }

    @Override
    public UIComponentProvider<DiagramViewer> asTemplateProvider()
    {
        return () -> new DiagramViewerImpl(this.context, this.svg, this.maxHeight, this.width, this.height, this.interactive);
    }
}
