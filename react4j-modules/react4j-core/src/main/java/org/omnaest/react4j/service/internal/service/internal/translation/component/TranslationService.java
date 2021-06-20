package org.omnaest.react4j.service.internal.service.internal.translation.component;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiFunction;

import javax.annotation.PreDestroy;

import org.apache.commons.lang3.StringUtils;
import org.omnaest.react4j.service.i18n.TranslationProvider;
import org.omnaest.utils.MapperUtils;
import org.omnaest.utils.SetUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TranslationService
{
    private static final Logger LOG = LoggerFactory.getLogger(TranslationService.class);

    @Autowired
    private List<TranslationProvider> translationProviders;

    @Autowired
    private TranslationPersistence translationPersistence;

    private ExecutorService translationExecutorService           = Executors.newSingleThreadExecutor();
    private ExecutorService preemptiveTranslationExecutorService = Executors.newSingleThreadExecutor();

    @PreDestroy
    private void destroy()
    {
        this.translationExecutorService.shutdownNow();
        this.preemptiveTranslationExecutorService.shutdownNow();
    }

    public Optional<String> translate(Locale sourceLocale, String key, String text, Locale targetLocale)
    {
        String effectiveText = StringUtils.trim(text);

        BiFunction<Locale, ExecutorService, Optional<String>> translationOperation = this.createTranslationOperation(sourceLocale, key, effectiveText);
        Optional<String> translatedTextByCache = translationOperation.apply(targetLocale, this.translationExecutorService);

        this.startAlternativeLocaleTranslationIfNoCacheEntryAvailable(translationOperation, translatedTextByCache, SetUtils.toSet(sourceLocale, targetLocale));

        String leftPadding = org.omnaest.utils.StringUtils.leftPadding(text);
        String rightPadding = org.omnaest.utils.StringUtils.rightPadding(text);

        return translatedTextByCache.map(value -> Optional.ofNullable(leftPadding)
                                                          .orElse("")
                + value + Optional.ofNullable(rightPadding)
                                  .orElse(""));
    }

    private void startAlternativeLocaleTranslationIfNoCacheEntryAvailable(BiFunction<Locale, ExecutorService, Optional<String>> translationOperation,
                                                                          Optional<String> translatedTextByCache, Set<Locale> ignoredLocales)
    {
        if (!translatedTextByCache.isPresent())
        {
            this.translationPersistence.getAvailableLocales()
                                       .stream()
                                       .filter(locale -> !ignoredLocales.contains(locale))
                                       .forEach(alternativeTargetLocale -> translationOperation.apply(alternativeTargetLocale,
                                                                                                      this.preemptiveTranslationExecutorService));
        }
    }

    private BiFunction<Locale, ExecutorService, Optional<String>> createTranslationOperation(Locale sourceLocale, String key, String effectiveText)
    {
        return (targetLocale, translationExecutorService) ->
        {
            Optional<String> translatedTextByCache = this.translationProviders.stream()
                                                                              .filter(TranslationProvider::isFast)
                                                                              .map(provider -> provider.getTranslator(sourceLocale, targetLocale))
                                                                              .filter(Optional::isPresent)
                                                                              .map(Optional::get)
                                                                              .map(translator -> translator.apply(key, effectiveText))
                                                                              .filter(Optional::isPresent)
                                                                              .findFirst()
                                                                              .flatMap(MapperUtils.identity());

            if (!translatedTextByCache.isPresent())
            {
                translationExecutorService.submit(() ->
                {
                    LOG.info("Starting translation for " + key + " from " + sourceLocale + " to " + targetLocale + " " + effectiveText);
                    this.translationProviders.stream()
                                             .map(provider -> provider.getTranslator(sourceLocale, targetLocale))
                                             .filter(Optional::isPresent)
                                             .map(Optional::get)
                                             .map(translator -> translator.apply(key, effectiveText))
                                             .filter(Optional::isPresent)
                                             .findFirst()
                                             .flatMap(MapperUtils.identity())
                                             .ifPresent(translatedTextValue -> this.translationPersistence.persist(key, sourceLocale, effectiveText,
                                                                                                                   targetLocale, translatedTextValue));
                });
            }

            return translatedTextByCache;
        };
    }
}
