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
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.omnaest.react4j.domain.ClipboardCopyButton;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.context.data.Data;
import org.omnaest.react4j.domain.i18n.I18nText;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer;
import org.omnaest.react4j.domain.rendering.components.LocationSupport;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.domain.rendering.node.NodeRendererRegistry;
import org.omnaest.react4j.domain.support.UIComponentProvider;
import org.omnaest.react4j.service.internal.nodes.ClipboardCopyButtonNode;

/**
 * @see ClipboardCopyButton
 * @author omnaest
 */
public class ClipboardCopyButtonImpl extends AbstractUIComponent<ClipboardCopyButton> implements ClipboardCopyButton
{
    public static final int DEFAULT_CLEAR_AFTER_DURATION_MILLIS = 30000;

    private I18nText        label;
    private String          text;
    private int             clearAfterDurationMillis            = DEFAULT_CLEAR_AFTER_DURATION_MILLIS;

    public ClipboardCopyButtonImpl(ComponentContext context)
    {
        super(context);
    }

    public ClipboardCopyButtonImpl(ComponentContext context, I18nText label, String text, int clearAfterDurationMillis)
    {
        super(context);
        this.label = label;
        this.text = text;
        this.clearAfterDurationMillis = clearAfterDurationMillis;
    }

    @Override
    public ClipboardCopyButton withText(String text)
    {
        this.text = text;
        return this;
    }

    @Override
    public ClipboardCopyButton withLabel(String label)
    {
        this.label = this.toI18nText(label);
        return this;
    }

    @Override
    public ClipboardCopyButton withClearAfterDuration(int duration, TimeUnit timeUnit)
    {
        this.clearAfterDurationMillis = (int) timeUnit.toMillis(duration);
        return this;
    }

    @Override
    public UIComponentRenderer asRenderer()
    {
        return new UIComponentRenderer() {
            @Override
            public Location getLocation(LocationSupport locationSupport)
            {
                return locationSupport.createLocation(ClipboardCopyButtonImpl.this.getId());
            }

            @Override
            public Node render(RenderingProcessor renderingProcessor, Location location, Optional<Data> data)
            {
                return new ClipboardCopyButtonNode().setLabel(ClipboardCopyButtonImpl.this.getTextResolver()
                                                                                          .apply(ClipboardCopyButtonImpl.this.label, location))
                                                    .setText(ClipboardCopyButtonImpl.this.text)
                                                    .setClearAfterDurationMillis(ClipboardCopyButtonImpl.this.clearAfterDurationMillis);
            }

            @Override
            public void manageEventHandler(EventHandlerRegistrationSupport eventHandlerRegistrationSupport)
            {
                // purely client-side interaction (see class javadoc) - there is no server EventHandler to register
            }

            @Override
            public void manageNodeRenderers(NodeRendererRegistry registry)
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
    public UIComponentProvider<ClipboardCopyButton> asTemplateProvider()
    {
        return () -> new ClipboardCopyButtonImpl(this.context, this.label, this.text, this.clearAfterDurationMillis);
    }
}
