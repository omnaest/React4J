package org.omnaest.react4j.service.internal.service.internal.translation.component;

import java.util.Locale;
import java.util.Set;

public interface TranslationPersistence
{
    public void persist(String key, Locale sourceLocale, String text, Locale targetLocale, String translatedText);

    public Set<Locale> getAvailableLocales();
}
