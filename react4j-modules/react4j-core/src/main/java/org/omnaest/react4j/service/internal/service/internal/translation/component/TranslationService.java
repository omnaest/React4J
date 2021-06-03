package org.omnaest.react4j.service.internal.service.internal.translation.component;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.PreDestroy;

import org.apache.commons.lang3.StringUtils;
import org.omnaest.react4j.service.i18n.TranslationProvider;
import org.omnaest.utils.MapperUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TranslationService
{
    @Autowired
    private List<TranslationProvider> translationProviders;

    @Autowired
    private TranslationPersistence translationPersistence;

    private ExecutorService translationExecutorService = Executors.newSingleThreadExecutor();

    @PreDestroy
    private void destroy()
    {
        this.translationExecutorService.shutdownNow();
    }

    public Optional<String> translate(Locale sourceLocale, String key, String text, Locale targetLocale)
    {
        String effectiveText = StringUtils.trim(text);

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
            this.translationExecutorService.submit(() ->
            {
                this.translationProviders.stream()
                                         .map(provider -> provider.getTranslator(sourceLocale, targetLocale))
                                         .filter(Optional::isPresent)
                                         .map(Optional::get)
                                         .map(translator -> translator.apply(key, effectiveText))
                                         .filter(Optional::isPresent)
                                         .findFirst()
                                         .flatMap(MapperUtils.identity())
                                         .ifPresent(translatedTextValue -> this.translationPersistence.persist(key, sourceLocale, effectiveText, targetLocale,
                                                                                                               translatedTextValue));
            });
        }

        String leftPadding = org.omnaest.utils.StringUtils.leftPadding(text);
        String rightPadding = org.omnaest.utils.StringUtils.rightPadding(text);

        return translatedTextByCache.map(value -> Optional.ofNullable(leftPadding)
                                                          .orElse("")
                + value + Optional.ofNullable(rightPadding)
                                  .orElse(""));
    }
}
