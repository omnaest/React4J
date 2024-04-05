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
import org.omnaest.react4j.domain.Range;
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
import org.omnaest.react4j.service.internal.nodes.RangeNode;
import org.omnaest.utils.template.TemplateUtils;

public class RangeImpl extends AbstractUIComponent<Range> implements Range
{
    private I18nText label;
    private double   min      = 0;
    private double   max      = 100;
    private double   step     = 1;
    private boolean  disabled = false;

    public RangeImpl(ComponentContext context)
    {
        super(context);
    }

    public RangeImpl(ComponentContext context, I18nText label, double min, double max, double step, boolean disabled)
    {
        super(context);
        this.label = label;
        this.min = min;
        this.max = max;
        this.step = step;
        this.disabled = disabled;
    }

    @Override
    public UIComponentRenderer asRenderer()
    {
        return new UIComponentRenderer()
        {
            @Override
            public Location getLocation(LocationSupport locationSupport)
            {
                return locationSupport.createLocation(RangeImpl.this.getId());
            }

            @Override
            public Node render(RenderingProcessor renderingProcessor, Location location, Optional<Data> data)
            {
                return new RangeNode().setLabel(RangeImpl.this.getTextResolver()
                                                              .apply(RangeImpl.this.label, location))
                                      .setMin(Double.toString(RangeImpl.this.min))
                                      .setMax(Double.toString(RangeImpl.this.max))
                                      .setStep(Double.toString(RangeImpl.this.step))
                                      .setDisabled(RangeImpl.this.disabled);
            }

            @Override
            public void manageNodeRenderers(NodeRendererRegistry registry)
            {
                registry.register(RangeNode.class, NodeRenderType.HTML, new NodeRenderer<RangeNode>()
                {
                    @Override
                    public String render(RangeNode node, NodeRenderingProcessor nodeRenderingProcessor)
                    {
                        return TemplateUtils.builder()
                                            .useTemplateClassResource(this.getClass(), "/render/templates/html/range.html")
                                            .add("label", nodeRenderingProcessor.render(node.getLabel()))
                                            .add("min", node.getMin())
                                            .add("max", node.getMax())
                                            .add("step", node.getStep())
                                            .add("disabled", node.isDisabled() ? "disabled" : "")
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
    public Range withLabel(String label)
    {
        this.label = I18nText.of(this.getLocations(), label, this.getDefaultLocale());
        return this;
    }

    @Override
    public UIComponentProvider<Range> asTemplateProvider()
    {
        return () -> new RangeImpl(this.context, this.label, this.min, this.max, this.step, this.disabled);
    }

}
