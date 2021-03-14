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

import java.util.function.Function;

import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.context.data.Data;

/**
 * @see UIComponent
 * @see UIComponentProvider
 * @author omnaest
 */
public interface UIComponentProviderWithData<UIC extends UIComponent<?>> extends Function<Data, UIC>
{
    public static <UIC extends UIComponent<?>> UIComponentProviderWithData<UIC> empty()
    {
        return data -> null;
    }

    public static <UIC extends UIComponent<?>> UIComponentProviderWithData<UIC> of(UIC component)
    {
        return data -> component;
    }
}
