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
package org.omnaest.react4j.domain.rendering;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.UIComponentFactory;
import org.omnaest.react4j.domain.context.data.Data;

import lombok.Builder;

/**
 * Internal extension of the {@link UIComponent} which provides a {@link UIComponentRenderer} instance
 * 
 * @see #asRenderer()
 * @author omnaest
 * @param <UIC>
 */
public interface RenderableUIComponent<UIC extends UIComponent<?>> extends UIComponent<UIC>
{
    public UIComponentRenderer asRenderer();

    public Optional<UIContextData> getUIContextInitialDataIfPresent();

    @lombok.Data
    @Builder
    public static class UIContextData
    {
        private final ContextIdCreator contextIdCreator;
        private final Data             data;
        private final Data             internalData;
    }

    public static interface ContextIdCreator extends Function<Location, String>
    {
    }

    public UIComponentWrapper<UIC> getWrapper();

    public static interface UIComponentWrapper<UIC extends UIComponent<?>> extends BiFunction<UIComponentFactory, UIC, UIComponent<?>>
    {
    }
}
