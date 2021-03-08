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

import java.util.stream.Stream;

import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.Toaster;
import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.i18n.I18nText;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer;
import org.omnaest.react4j.domain.rendering.components.LocationSupport;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.domain.rendering.node.NodeRendererRegistry;
import org.omnaest.react4j.service.internal.nodes.ToasterNode;
import org.omnaest.react4j.service.internal.service.LocalizedTextResolverService;

public class ToasterImpl extends AbstractUIComponentAndContentHolder<Toaster> implements Toaster
{
    private I18nText       title;
    private UIComponent<?> content;

    public ToasterImpl(ComponentContext context)
    {
        super(context);
    }

    @Override
    public UIComponentRenderer asRenderer()
    {
        return new UIComponentRenderer()
        {
            @Override
            public Location getLocation(LocationSupport locationSupport)
            {
                return locationSupport.createLocation(ToasterImpl.this.getId());
            }

            @Override
            public Node render(RenderingProcessor renderingProcessor, Location location)
            {
                LocalizedTextResolverService textResolver = ToasterImpl.this.getTextResolver();
                return new ToasterNode().setContent(renderingProcessor.process(ToasterImpl.this.content, location))
                                        .setTitle(textResolver.apply(ToasterImpl.this.title, location));
            }

            @Override
            public void manageNodeRenderers(NodeRendererRegistry registry)
            {
                // TODO Auto-generated method stub
            }

            @Override
            public void manageEventHandler(EventHandlerRegistrationSupport eventHandlerRegistrationSupport)
            {
            }

            @Override
            public Stream<UIComponent<?>> getSubComponents()
            {
                return Stream.of(ToasterImpl.this.content);
            }

        };
    }

    @Override
    public Toaster withTitle(String title)
    {
        this.title = I18nText.of(this.getLocations(), title, this.getDefaultLocale());
        return this;
    }

    @Override
    public Toaster withContent(UIComponent<?> component)
    {
        this.content = component;
        return this;
    }

}
