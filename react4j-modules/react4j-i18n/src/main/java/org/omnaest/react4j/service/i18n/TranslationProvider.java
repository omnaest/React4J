package org.omnaest.react4j.service.i18n;

import java.util.Locale;
import java.util.Optional;
import java.util.function.BiFunction;

/**
 * Provides translations for a given target language based on a given source language.<br>
 * <br>
 * With the Order annotation the order of the {@link TranslationProvider}s can be specified.
 * 
 * @author omnaest
 */
public interface TranslationProvider
{
    /**
     * Returns a {@link Translator} for a given source and target {@link Locale} pair. Returns an {@link Optional#empty()} if there is no {@link Translator}
     * available for the given source and target pair.
     * 
     * @param sourceLocale
     * @param targetLocale
     * @return
     */
    public Optional<Translator> getTranslator(Locale sourceLocale, Locale targetLocale);

    /**
     * A {@link Translator} has a fixed source and target {@link Locale} and can try to translate a given key and text in the source locale to the text in the
     * target
     * locale. If the translation is not possible a {@link Optional#empty()} is returned.
     * 
     * @author omnaest
     */
    public static interface Translator extends BiFunction<String, String, Optional<String>>
    {
    }

    /**
     * Returns true, if this implementation of a {@link TranslationProvider} is slow. The translation service will invoke such a provider in an own thread and
     * will not block the UI, if a translation is missing.
     * 
     * @see #isFast()
     * @return
     */
    public boolean isSlow();

    /**
     * Returns true, if this implementation of a {@link TranslationProvider} is able to resolve translations very fast.
     * 
     * @return
     */
    public default boolean isFast()
    {
        return !this.isSlow();
    }
}
