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
package org.omnaest.react4j.domain;

import java.util.function.BiConsumer;

import org.omnaest.react4j.domain.context.data.Data;
import org.omnaest.react4j.domain.context.data.DefineableDataContext;
import org.omnaest.react4j.domain.context.data.TypedDataContext;
import org.omnaest.react4j.domain.context.ui.UIContext;
import org.omnaest.react4j.domain.support.UIComponentProvider;
import org.omnaest.utils.functional.TriConsumer;

public interface UIComponent<UIC extends UIComponent<?>>
{
    public String getId();

    public Locations getLocations();

    /**
     * @see DefineableDataContext
     * @param dataContextConsumer
     * @return
     */
    public UIC withDataContext(BiConsumer<UIC, DefineableDataContext> dataContextConsumer);

    public UIC withUIContext(UIContextConsumer<UIC> uiContextConsumer);

    public UIC withUIContext(UIContextAndDataConsumer<UIC> uiContextConsumer);

    public RerenderingContainer withRerenderingUIContext(UIContextAndDataConsumer<UIC> rerenderingUiContextConsumer);

    public <T> UIC withDataContext(Class<T> type, BiConsumer<UIC, TypedDataContext<T>> dataContextConsumer);

    public UIC registerParent(UIComponent<?> parent);

    public static interface UIContextConsumer<UIC extends UIComponent<?>> extends BiConsumer<UIC, UIContext>
    {
    }

    public static interface UIContextAndDataConsumer<UIC extends UIComponent<?>> extends TriConsumer<UIC, UIContext, Data>
    {
    }

    public UIComponentProvider<UIC> asTemplateProvider();
}
