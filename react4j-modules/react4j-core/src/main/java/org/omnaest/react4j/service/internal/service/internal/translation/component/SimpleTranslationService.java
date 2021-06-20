package org.omnaest.react4j.service.internal.service.internal.translation.component;

import java.util.Locale;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @see LocaleService
 * @see TranslationService
 * @author omnaest
 */
@Service
public class SimpleTranslationService
{
    @Autowired
    private TranslationService translationService;

    @Autowired
    private LocaleService localeService;

    public Optional<String> translate(String key, String text)
    {
        return this.translationService.translate(Locale.US, key, text, this.localeService.getRequestLocale()
                                                                                         .orElse(Locale.US));

    }

}
