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
package org.omnaest.react4j.service.internal.service.internal.translation.component;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

/**
 * the {@link LocaleService} manages the user request {@link Locale} context
 * 
 * @author omnaest
 */
@Service
public class LocaleService
{
    private static final Logger LOG = LoggerFactory.getLogger(LocaleService.class);

    @Autowired
    private TranslationPersistence translationPersistence;

    public Optional<Locale> getRequestLocale()
    {
        return Optional.ofNullable(LocaleContextHolder.getLocale());
    }

    public void setRequestLocale(Locale locale)
    {
        LocaleContextHolder.setLocale(locale);
    }

    public void setRequestLocaleByLanguageTag(String languageTag)
    {
        if (StringUtils.isNotEmpty(languageTag))
        {
            try
            {
                this.setRequestLocale(Locale.forLanguageTag(languageTag));
            }
            catch (Exception e)
            {
                LOG.error("Unable to identify a locale for " + languageTag, e);
            }
        }
    }

    public Set<Locale> getAvailableLocales()
    {
        return this.translationPersistence.getAvailableLocales();
    }
}
