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
package org.omnaest.react4j.service.internal.service.internal.translation;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.i18n.I18nText;
import org.omnaest.react4j.domain.i18n.UILocale;
import org.omnaest.react4j.service.internal.nodes.i18n.I18nTextValue;
import org.omnaest.react4j.service.internal.service.LocalizedTextResolverService;
import org.omnaest.react4j.service.internal.service.internal.translation.component.LocaleService;
import org.omnaest.react4j.service.internal.service.internal.translation.component.TranslationService;
import org.omnaest.utils.MapUtils;
import org.omnaest.utils.element.bi.BiElement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LocalizedTextResolverServiceImpl implements LocalizedTextResolverService
{
    @Autowired
    private TranslationService translationService;

    @Autowired
    private LocaleService localeService;

    @Override
    public I18nTextValue apply(I18nText i18nText, Location location)
    {
        String text = Optional.ofNullable(i18nText)
                              .map(I18nText::getDefaultText)
                              .orElse("");
        Map<String, String> localeTagToText = MapUtils.builder()
                                                      .put(DEFAULT_LOCALE_KEY, text)
                                                      .build();

        if (!Optional.ofNullable(i18nText)
                     .map(I18nText::isNonTranslatable)
                     .orElse(true))
        {
            this.translateText(i18nText, location, text, localeTagToText);
        }

        return new I18nTextValue(localeTagToText);
    }

    private void translateText(I18nText i18nText, Location location, String text, Map<String, String> localeTagToText)
    {
        Optional<String> sourceLocaleTag = Optional.ofNullable(i18nText)
                                                   .map(I18nText::getDefaultLocale)
                                                   .map(UILocale::asLocale)
                                                   .map(Locale::toLanguageTag);
        sourceLocaleTag.ifPresent(tag -> localeTagToText.put(tag, text));

        if (StringUtils.isNotBlank(text) && sourceLocaleTag.isPresent())
        {
            this.localeService.getRequestLocale()
                              .flatMap(targetLocale -> this.translationService.translate(i18nText.getDefaultLocale()
                                                                                                 .asLocale(),
                                                                                         i18nText.getTextKey(location), text, targetLocale)
                                                                              .map(translatedText -> BiElement.of(targetLocale, translatedText)))
                              .ifPresent(targetLocaleAndTranslatedText ->
                              {
                                  localeTagToText.put(targetLocaleAndTranslatedText.getFirst()
                                                                                   .toLanguageTag(),
                                                      targetLocaleAndTranslatedText.getSecond());
                                  localeTagToText.put(DEFAULT_LOCALE_KEY, targetLocaleAndTranslatedText.getSecond());
                              });
        }
    }

}
