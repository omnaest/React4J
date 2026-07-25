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

import org.omnaest.react4j.domain.Collapse;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.context.data.Data;
import org.omnaest.react4j.domain.i18n.I18nText;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer;
import org.omnaest.react4j.domain.rendering.components.LocationSupport;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.domain.rendering.node.NodeRendererRegistry;
import org.omnaest.react4j.domain.support.UIComponentProvider;
import org.omnaest.react4j.service.internal.nodes.CollapseNode;
import org.omnaest.react4j.service.internal.service.LocalizedTextResolverService;

public class CollapseImpl extends AbstractUIComponentAndContentHolder<Collapse> implements Collapse
{
    private UIComponent<?> content;
    private I18nText       toggleLabel;
    private boolean        initiallyOpen;

    public CollapseImpl(ComponentContext context)
    {
        super(context);
    }

    public CollapseImpl(ComponentContext context, UIComponent<?> content, I18nText toggleLabel, boolean initiallyOpen)
    {
        super(context);
        this.content = content;
        this.toggleLabel = toggleLabel;
        this.initiallyOpen = initiallyOpen;
    }

    @Override
    public Collapse withContent(UIComponent<?> component)
    {
        this.content = component;
        return this;
    }

    @Override
    public Collapse withToggleLabel(String toggleLabel)
    {
        this.toggleLabel = this.toI18nText(toggleLabel);
        return this;
    }

    @Override
    public Collapse withInitiallyOpen(boolean initiallyOpen)
    {
        this.initiallyOpen = initiallyOpen;
        return this;
    }

    @Override
    public UIComponentRenderer asRenderer()
    {
        return new UIComponentRenderer() {
            @Override
            public Location getLocation(LocationSupport locationSupport)
            {
                return locationSupport.createLocation(CollapseImpl.this.getId());
            }

            @Override
            public Node render(RenderingProcessor renderingProcessor, Location location, Optional<Data> data)
            {
                LocalizedTextResolverService textResolver = CollapseImpl.this.getTextResolver();
                return new CollapseNode().setContent(renderingProcessor.process(CollapseImpl.this.content, location))
                                         .setToggleLabel(CollapseImpl.this.toggleLabel != null ? textResolver.apply(CollapseImpl.this.toggleLabel, location)
                                                 : null)
                                         .setInitiallyOpen(CollapseImpl.this.initiallyOpen);
            }

            @Override
            public void manageNodeRenderers(NodeRendererRegistry registry)
            {
            }

            @Override
            public void manageEventHandler(EventHandlerRegistrationSupport eventHandlerRegistrationSupport)
            {
            }

            @Override
            public Stream<ParentLocationAndComponent> getSubComponents(Location parentLocation)
            {
                return Stream.of(ParentLocationAndComponent.of(parentLocation, CollapseImpl.this.content));
            }

        };
    }

    @Override
    public UIComponentProvider<Collapse> asTemplateProvider()
    {
        return () -> new CollapseImpl(this.context, this.content, this.toggleLabel, this.initiallyOpen);
    }
}
