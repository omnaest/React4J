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
package org.omnaest.react4j.service.internal.nodes.i18n;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public class I18nTextValue
{
    private Map<String, String> localeToText;

    public I18nTextValue()
    {
        super();
        this.localeToText = new HashMap<>();
    }

    @JsonCreator
    public I18nTextValue(Map<String, String> localeToText)
    {
        super();
        this.localeToText = localeToText;
    }

    @JsonValue
    public Map<String, String> getLocaleToText()
    {
        return this.localeToText;
    }

    @Override
    public String toString()
    {
        return "I18nTextValue [localeToText=" + this.localeToText + "]";
    }

}
