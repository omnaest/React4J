package org.omnaest.react4j.service.internal.service.internal.translation.component;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.apache.commons.codec.binary.StringUtils;
import org.omnaest.react4j.service.i18n.TranslationProvider;
import org.omnaest.utils.FileUtils;
import org.omnaest.utils.JSONHelper;
import org.omnaest.utils.OptionalUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Handles the translation files on disc
 * 
 * @author omnaest
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1000)
public class TranslationFileService implements TranslationProvider, TranslationPersistence
{
    @Value("${i18n.folder:i18n}")
    protected String translationFolder;

    private Map<Locale, KeyToTextStore> localeToKeyToTextStore = new ConcurrentHashMap<>();

    @Scheduled(initialDelay = 1000, fixedDelay = 5 * 1000)
    protected synchronized void synchronize()
    {
        Optional.ofNullable(this.translationFolder)
                .map(File::new)
                .ifPresent(translationDirectory ->
                {
                    FileUtils.forceMkdirSilently(translationDirectory);

                    FileUtils.listDirectoryFiles(translationDirectory, File::isDirectory)
                             .forEach(localeDirectory ->
                             {
                                 File translationFile = this.determineTranslationFile(localeDirectory);
                                 if (translationFile.exists() && translationFile.isFile())
                                 {
                                     KeyToTextStore keyToTextStore = this.getKeyToTextStore(translationFile);
                                     Locale locale = Locale.forLanguageTag(localeDirectory.getName());
                                     this.localeToKeyToTextStore.put(locale, keyToTextStore);
                                 }
                             });
                });
    }

    private KeyToTextStore getKeyToTextStore(File translationFile)
    {
        return Optional.ofNullable(FileUtils.toSupplier(translationFile)
                                            .with(JSONHelper.deserializer(KeyToTextStore.class))
                                            .get())
                       .orElse(new KeyToTextStore(new HashMap<>()));
    }

    private void writeKeyToTextStore(File translationFile, KeyToTextStore keyToTextStore)
    {
        FileUtils.toConsumer(translationFile)
                 .with(JSONHelper.serializer(KeyToTextStore.class))
                 .accept(keyToTextStore);
    }

    private File determineTranslationFile(File localeDirectory)
    {
        return new File(localeDirectory, "translation.json");
    }

    @Override
    public Optional<Translator> getTranslator(Locale sourceLocale, Locale targetLocale)
    {
        return OptionalUtils.bothElementsOfNullable(sourceLocale, targetLocale)
                            .filter(sourceAndTargetLocale -> this.localeToKeyToTextStore.containsKey(sourceAndTargetLocale.getFirst()))
                            .filter(sourceAndTargetLocale -> this.localeToKeyToTextStore.containsKey(sourceAndTargetLocale.getSecond()))
                            .map(sourceAndTargetLocale ->
                            {
                                KeyToTextStore sourceKeyToTextStore = this.localeToKeyToTextStore.get(sourceAndTargetLocale.getFirst());
                                KeyToTextStore targetKeyToTextStore = this.localeToKeyToTextStore.get(sourceAndTargetLocale.getSecond());

                                return new Translator()
                                {
                                    @Override
                                    public Optional<String> apply(String key, String text)
                                    {
                                        Optional<String> sourceText = sourceKeyToTextStore.getText(key);
                                        Optional<String> targetText = targetKeyToTextStore.getText(key);
                                        return sourceText.filter(sourceTextValue -> StringUtils.equals(sourceTextValue, text))
                                                         .flatMap(sourceTextValue -> targetText);
                                    }
                                };
                            });
    }

    public static class KeyToTextStore
    {
        private Map<String, String> keyToText;

        @JsonCreator
        public KeyToTextStore(Map<String, String> keyToText)
        {
            super();
            this.keyToText = keyToText;
        }

        @JsonIgnore
        public Optional<String> getText(String key)
        {
            return Optional.ofNullable(this.keyToText)
                           .map(keyToText -> keyToText.get(key));
        }

        @JsonValue
        protected Map<String, String> getKeyToText()
        {
            return this.keyToText;
        }

        public void put(String key, String translatedText)
        {
            if (this.keyToText == null)
            {
                this.keyToText = new HashMap<>();
            }

            this.keyToText.put(key, translatedText);
        }

    }

    @Override
    public void persist(String key, Locale sourceLocale, String text, Locale targetLocale, String translatedText)
    {
        if (this.getTranslator(sourceLocale, targetLocale)
                .flatMap(translator -> translator.apply(key, text))
                .map(currentTranslatedText -> !StringUtils.equals(currentTranslatedText, translatedText))
                .orElse(true))
        {
            this.persistChangedTranslatedText(key, sourceLocale, text);
            this.persistChangedTranslatedText(key, targetLocale, translatedText);
        }
    }

    private synchronized void persistChangedTranslatedText(String key, Locale targetLocale, String translatedText)
    {
        File localeDirectory = new File(this.translationFolder, targetLocale.toLanguageTag());
        FileUtils.forceMkdirSilently(localeDirectory);
        File translationFile = this.determineTranslationFile(localeDirectory);

        KeyToTextStore keyToTextStore = this.getKeyToTextStore(translationFile);
        keyToTextStore.put(key, translatedText);

        this.writeKeyToTextStore(translationFile, keyToTextStore);
    }

    @Override
    public boolean isSlow()
    {
        return false;
    }

    @Override
    public Set<Locale> getAvailableLocales()
    {
        return this.localeToKeyToTextStore.keySet()
                                          .stream()
                                          .collect(Collectors.toSet());
    }

}
