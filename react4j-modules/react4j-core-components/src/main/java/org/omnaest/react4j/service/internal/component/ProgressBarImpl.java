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
import org.omnaest.react4j.domain.ProgressBar;
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
import org.omnaest.react4j.service.internal.nodes.ProgressBarNode;
import org.omnaest.utils.NumberUtils;
import org.omnaest.utils.template.TemplateUtils;

public class ProgressBarImpl extends AbstractUIComponent<ProgressBar> implements ProgressBar
{
    private Supplier<I18nText> text    = () -> null;
    private double             minimum = 0.0;
    private double             maximum = 100.0;
    private double             value   = 0.0;
    private boolean            padding = true;

    public ProgressBarImpl(ComponentContext context)
    {
        super(context);
    }

    public ProgressBarImpl(ComponentContext context, Supplier<I18nText> text, double minimum, double maximum, double value)
    {
        super(context);
        this.text = text;
        this.minimum = minimum;
        this.maximum = maximum;
        this.value = value;
    }

    @Override
    public ProgressBar withoutPadding()
    {
        this.padding = false;
        return this;
    }

    @Override
    public UIComponentWrapper<ProgressBar> getWrapper()
    {
        if (this.padding)
        {
            return (factory, progressBar) -> factory.newPaddingContainer()
                                                    .withContent(progressBar);
        }
        else
        {
            return (factory, progressBar) -> progressBar;
        }
    }

    @Override
    public UIComponentRenderer asRenderer()
    {
        return new UIComponentRenderer()
        {
            @Override
            public Location getLocation(LocationSupport locationSupport)
            {
                return locationSupport.createLocation(ProgressBarImpl.this.getId());
            }

            @Override
            public Node render(RenderingProcessor renderingProcessor, Location location, Optional<Data> data)
            {
                return new ProgressBarNode().setText(ProgressBarImpl.this.getTextResolver()
                                                                         .apply(ProgressBarImpl.this.text.get(), location))
                                            .setValue(ProgressBarImpl.this.value)
                                            .setMin(ProgressBarImpl.this.minimum)
                                            .setMax(ProgressBarImpl.this.maximum);
            }

            @Override
            public void manageNodeRenderers(NodeRendererRegistry registry)
            {
                registry.register(ProgressBarNode.class, NodeRenderType.HTML, new NodeRenderer<ProgressBarNode>()
                {
                    @Override
                    public String render(ProgressBarNode node, NodeRenderingProcessor nodeRenderingProcessor)
                    {
                        return TemplateUtils.builder()
                                            .useTemplateClassResource(this.getClass(), "/render/templates/html/progressbar.html")
                                            .add("text", nodeRenderingProcessor.render(node.getText()))
                                            .add("min", node.getMin())
                                            .add("max", node.getMax())
                                            .add("value", node.getValue())
                                            .add("ratio", NumberUtils.formatter()
                                                                     .asPercentage()
                                                                     .format(node.getValue() / (node.getMax() - node.getMin())))
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
    public UIComponentProvider<ProgressBar> asTemplateProvider()
    {
        return () -> new ProgressBarImpl(this.context, () -> this.text.get(), this.minimum, this.maximum, this.value);
    }

    @Override
    public ProgressBar withText(String text)
    {
        this.text = () -> I18nText.of(this.getLocations(), text, this.getDefaultLocale());
        return this;
    }

    @Override
    public ProgressBar withNonTranslatedText(String text)
    {
        boolean isNonTranslatable = true;
        this.text = () -> I18nText.of(this.getLocations(), text, this.getDefaultLocale(), isNonTranslatable);
        return this;
    }

    @Override
    public ProgressBar withValue(double value)
    {
        this.value = value;
        return this;
    }

    @Override
    public ProgressBar withMinimum(double minimum)
    {
        this.minimum = minimum;
        return this;
    }

    @Override
    public ProgressBar withMaximum(double maximum)
    {
        this.maximum = maximum;
        return this;
    }

    @Override
    public ProgressBar withProgressRatio(double value)
    {
        return this.withMinimum(0)
                   .withMaximum(1.0)
                   .withValue(value);
    }

    @Override
    public ProgressBar withRatioText()
    {
        boolean isNonTranslatable = true;
        this.text = () ->
        {
            double ratio = this.value / (this.maximum - this.minimum);
            String text = NumberUtils.formatter()
                                     .asPercentage()
                                     .withMaximumFractionDigits(0)
                                     .format(ratio);
            return I18nText.of(this.getLocations(), text, this.getDefaultLocale(), isNonTranslatable);
        };
        return this;
    }
}
