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
package org.omnaest.react4j.domain.i18n;

import java.util.Map;

import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.Locations;

public interface I18nText
{
    public Map<Location, String> getKeys();

    public boolean isNonTranslatable();

    public String getDefaultText();

    public UILocale getDefaultLocale();

    /**
     * Returns a key representation
     * 
     * @param location
     * @return
     */
    public String getTextKey(Location location);

    public static I18nText of(Locations locations, String text, UILocale locale)
    {
        return new I18nHashBased(text, locations, locale, false);
    }

    public static I18nText of(Locations locations, String text, UILocale locale, boolean isNonTranslatable)
    {
        return new I18nHashBased(text, locations, locale, isNonTranslatable);
    }

}
