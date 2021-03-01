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
package org.omnaest.react4j.service.internal.service.internal;

import java.util.Map;
import java.util.Optional;

import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.i18n.I18nText;
import org.omnaest.react4j.service.internal.nodes.i18n.I18nTextValue;
import org.omnaest.react4j.service.internal.service.LocalizedTextResolverService;
import org.omnaest.utils.MapUtils;
import org.springframework.stereotype.Service;

@Service
public class LocalizedTextResolverServiceImpl implements LocalizedTextResolverService
{
    @Override
    public I18nTextValue apply(I18nText i18nText, Location location)
    {
        Map<String, String> localeToText = MapUtils.builder()
                                                   .put(DEFAULT_LOCALE_KEY, Optional.ofNullable(i18nText)
                                                                                    .map(I18nText::getDefaultText)
                                                                                    .orElse(""))
                                                   .build();
        return new I18nTextValue(localeToText);
    }
}
