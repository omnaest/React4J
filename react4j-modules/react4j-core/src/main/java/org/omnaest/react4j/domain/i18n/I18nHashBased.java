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
import java.util.stream.Collectors;

import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.Locations;

/**
 * @see I18nText#of(Locations, String, UILocale)
 * @author omnaest
 */
class I18nHashBased implements I18nText
{
    private String    text;
    private UILocale  locale;
    private Locations locations;

    public I18nHashBased(String text, Locations location, UILocale locale)
    {
        super();
        this.text = text;
        this.locations = location;
        this.locale = locale;
    }

    @Override
    public Map<Location, String> getKeys()
    {
        return this.locations.get()
                             .stream()
                             .collect(Collectors.toMap(location -> location, location -> location.get()
                                                                                                 .stream()
                                                                                                 .map(token -> token.replaceAll("[^a-zA-Z0-9]", "_"))
                                                                                                 .collect(Collectors.joining("."))
                                     + "." + this.text.hashCode()));
    }

    @Override
    public String getDefaultText()
    {
        return this.text;
    }

    @Override
    public UILocale getDefaultLocale()
    {
        return this.locale;
    }

    @Override
    public String toString()
    {
        return "I18nHashBased [text=" + this.text + ", locale=" + this.locale + ", locations=" + this.locations + "]";
    }

}
