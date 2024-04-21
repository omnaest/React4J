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
package org.omnaest.react4j.component.listview.internal;

import java.util.function.Consumer;

import org.omnaest.react4j.component.listview.ListView;
import org.omnaest.react4j.component.listview.internal.data.ListViewData;
import org.omnaest.react4j.component.listview.internal.data.ListViewElementData;
import org.omnaest.react4j.component.listview.internal.renderer.ListViewElementRenderer;
import org.omnaest.react4j.component.listview.internal.renderer.ListViewRenderer;
import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer;
import org.omnaest.react4j.domain.support.UIComponentProvider;
import org.omnaest.react4j.service.internal.component.AbstractUIComponent;
import org.omnaest.react4j.service.internal.component.ComponentContext;

public class ListViewImpl extends AbstractUIComponent<ListView> implements ListView
{
    private ListViewData.ListViewDataBuilder data = ListViewData.builder();

    public ListViewImpl(ComponentContext context)
    {
        super(context);
    }

    public ListViewImpl(ComponentContext context, ListViewData.ListViewDataBuilder data)
    {
        super(context);
        this.data = data;
    }

    @Override
    public UIComponentRenderer asRenderer()
    {
        return new ListViewRenderer(this.data.build(), this::getId);
    }

    @Override
    public UIComponentProvider<ListView> asTemplateProvider()
    {
        return () -> new ListViewImpl(this.context, this.data.build()
                                                             .toBuilder());
    }

    @Override
    public ListView withSource(DataSource source)
    {
        // TODO Auto-generated method stub
        return this;
    }

    @Override
    public ListView withElement(Consumer<ListViewElement> elementConsumer)
    {
        if (elementConsumer != null)
        {
            ListViewElementImpl element = new ListViewElementImpl(this.context);
            elementConsumer.accept(element);
            this.data.element(element);
        }
        return this;
    }

    private static class ListViewElementImpl extends AbstractUIComponent<ListViewElement> implements ListViewElement
    {
        private ListViewElementData.ListViewElementDataBuilder data = ListViewElementData.builder();

        public ListViewElementImpl(ComponentContext context)
        {
            super(context);
        }

        public ListViewElementImpl(ComponentContext context, ListViewElementData.ListViewElementDataBuilder data)
        {
            super(context);
            this.data = data;
        }

        @Override
        public UIComponentProvider<ListViewElement> asTemplateProvider()
        {
            return () -> new ListViewElementImpl(this.context, this.data.build()
                                                                        .toBuilder());
        }

        @Override
        public UIComponentRenderer asRenderer()
        {
            return new ListViewElementRenderer(this.data.build(), this::getId);
        }

        @Override
        public ListViewElement withContent(UIComponent<?> uiComponent)
        {
            if (uiComponent != null)
            {
                this.data.content(uiComponent);
            }
            return this;
        }

    }
}
