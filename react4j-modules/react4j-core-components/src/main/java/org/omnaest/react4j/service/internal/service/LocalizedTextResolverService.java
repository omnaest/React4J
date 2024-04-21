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
package org.omnaest.react4j.service.internal.service;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.i18n.I18nText;
import org.omnaest.react4j.service.internal.nodes.i18n.I18nTextValue;

public interface LocalizedTextResolverService extends BiFunction<I18nText, Location, I18nTextValue>
{
    public static final String DEFAULT_LOCALE_KEY = "DEFAULT";

    public default List<I18nTextValue> apply(List<I18nText> texts, Location location)
    {
        return texts.stream()
                    .map(text -> this.apply(text, location))
                    .collect(Collectors.toList());
    }

    public default LocationAwareTextResolver apply(Location location)
    {
        return i18nText -> this.apply(i18nText, location);
    }

    public static interface LocationAwareTextResolver extends Function<I18nText, I18nTextValue>
    {

    }
}
