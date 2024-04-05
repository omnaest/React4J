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

import org.omnaest.react4j.domain.Heading;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.context.data.Data;
import org.omnaest.react4j.domain.i18n.I18nText;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer;
import org.omnaest.react4j.domain.rendering.components.LocationSupport;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.domain.rendering.node.NodeRenderType;
import org.omnaest.react4j.domain.rendering.node.NodeRendererRegistry;
import org.omnaest.react4j.domain.support.UIComponentProvider;
import org.omnaest.react4j.service.internal.nodes.HeadingNode;

public class HeadingImpl extends AbstractUIComponent<Heading> implements Heading
{
    private I18nText text;
    private int      level = 1;

    public HeadingImpl(ComponentContext context)
    {
        super(context);
    }

    public HeadingImpl(ComponentContext context, I18nText text, int level)
    {
        super(context);
        this.text = text;
        this.level = level;
    }

    @Override
    public UIComponentRenderer asRenderer()
    {
        return new UIComponentRenderer()
        {

            @Override
            public Location getLocation(LocationSupport locationSupport)
            {
                return locationSupport.createLocation(HeadingImpl.this.getId());
            }

            @Override
            public Node render(RenderingProcessor renderingProcessor, Location location, Optional<Data> data)
            {
                return new HeadingNode().setText(HeadingImpl.this.getTextResolver()
                                                                 .apply(HeadingImpl.this.text, location))
                                        .setLevel(HeadingImpl.this.level);
            }

            @Override
            public void manageNodeRenderers(NodeRendererRegistry registry)
            {
                registry.register(HeadingNode.class, NodeRenderType.HTML, (node, nodeRenderingProcessor) ->
                {
                    int level = node.getLevel();
                    String body = Optional.ofNullable(node.getText())
                                          .map(nodeRenderingProcessor::render)
                                          .orElse("");
                    return "<h" + level + ">" + body + "</h" + level + ">";
                });
            }

            @Override
            public void manageEventHandler(EventHandlerRegistrationSupport eventHandlerRegistrationSupport)
            {
            }

            @Override
            public Stream<ParentLocationAndComponent> getSubComponents(Location parentLocation)
            {
                return Stream.empty();
            }

        };
    }

    @Override
    public Heading withText(String text)
    {
        this.text = I18nText.of(this.getLocations(), text, this.getDefaultLocale());
        return this;
    }

    @Override
    public Heading withNonTranslatedText(String text)
    {
        this.text = I18nText.of(this.getLocations(), text, this.getDefaultLocale(), true);
        return this;
    }

    @Override
    public Heading withLevel(int level)
    {
        this.level = level;
        return this;
    }

    @Override
    public Heading withLevel(Level level)
    {
        return this.withLevel(level.level());
    }

    @Override
    public UIComponentProvider<Heading> asTemplateProvider()
    {
        return () -> new HeadingImpl(this.context, this.text, this.level);
    }

}
