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
package org.omnaest.react4j.domain.support;

import java.util.List;
import java.util.function.Consumer;

import org.omnaest.react4j.domain.ScrollbarContainer;
import org.omnaest.react4j.domain.ScrollbarContainer.HorizontalBoxMode;
import org.omnaest.react4j.domain.ScrollbarContainer.VerticalBoxMode;
import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.UIComponentFactory;

public interface UIContentHolder<R>
{
    public R withContent(UIComponent<?> component);

    public R withContent(List<UIComponent<?>> components);

    public R withContent(UIComponentProvider<?> componentProvider);

    public R withContent(UIComponentFactoryFunction factoryConsumer);

    public R withContent(LayoutProvider layout, UIComponent<?> component);

    public R withContent(LayoutProvider layout, UIComponentProvider<?> componentProvider);

    public R withContent(LayoutProvider layout, UIComponentFactoryFunction factoryConsumer);

    /**
     * @see Layout
     * @author omnaest
     */
    public static interface LayoutProvider extends UIComponentWithContentFactoryFunction
    {
    }

    /**
     * @see #withScrollbar(Consumer)
     * @author omnaest
     */
    public static enum Layout implements LayoutProvider
    {
        FULL_VIEWPORT_SIZE(f -> f.newScrollbarContainer()
                                 .withVerticalBox(VerticalBoxMode.FULL_VIEWPORT_HEIGHT)
                                 .withHorizontalBox(HorizontalBoxMode.FULL_VIEWPORT_WIDTH)),
        FULL_PARENT_SIZE(f -> f.newScrollbarContainer()
                               .withVerticalBox(VerticalBoxMode.FULL_PARENT_HEIGHT)
                               .withHorizontalBox(HorizontalBoxMode.FULL_PARENT_WIDTH)),
        FULL_VIEWPORT_HEIGHT(f -> f.newScrollbarContainer()
                                   .withVerticalBox(VerticalBoxMode.FULL_VIEWPORT_HEIGHT)),
        HALF_VIEWPORT_HEIGHT(f -> f.newScrollbarContainer()
                                   .withVerticalBox(VerticalBoxMode.HALF_VIEWPORT_HEIGHT)),
        FULL_CONTENT_WIDTH(f -> f.newScrollbarContainer()
                                 .withHorizontalBox(HorizontalBoxMode.FULL_CONTENT_WIDTH));

        private LayoutProvider layoutProvider;

        private Layout(LayoutProvider layoutProvider)
        {
            this.layoutProvider = layoutProvider;
        }

        @Override
        public UIComponentWithContent<?> apply(UIComponentFactory factory)
        {
            return this.layoutProvider.apply(factory);
        }

        public static LayoutProvider withScrollbar(Consumer<ScrollbarContainer> scrollbarConsumer)
        {
            return factory ->
            {
                ScrollbarContainer scrollbarContainer = factory.newScrollbarContainer();
                scrollbarConsumer.accept(scrollbarContainer);
                return scrollbarContainer;
            };
        }
    }
}
