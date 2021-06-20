package org.omnaest.react4j.service.i18n;

import java.util.Locale;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.omnaest.deepl.rest.DeeplRestUtils;
import org.omnaest.deepl.rest.DeeplRestUtils.License;
import org.omnaest.deepl.rest.DeeplRestUtils.Translation;
import org.omnaest.deepl.rest.DeeplRestUtils.TranslationResponse;
import org.omnaest.deepl.rest.DeeplRestUtils.Usage;
import org.omnaest.utils.ListUtils;
import org.omnaest.utils.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class DeeplTranslationProvider implements TranslationProvider
{
    private static Logger LOG = LoggerFactory.getLogger(DeeplTranslationProvider.class);

    @Value("${react4j.deepl.api_key}")
    private String authorizationKey;

    @Value("${react4j.deepl.license}")
    private Optional<String> license;

    @Override
    public Optional<Translator> getTranslator(Locale sourceLocale, Locale targetLocale)
    {
        return Optional.of(new Translator()
        {
            @Override
            public Optional<String> apply(String key, String text)
            {
                String sourceLanguage = sourceLocale.getLanguage();
                String targetLanguage = targetLocale.getLanguage();
                if (StringUtils.equals(sourceLanguage, targetLanguage))
                {
                    return Optional.ofNullable(text);
                }
                else
                {
                    try
                    {
                        License license = DeeplTranslationProvider.this.license.map(License::valueOf)
                                                                               .orElse(License.FREE);
                        TranslationResponse response = DeeplRestUtils.translate(license, DeeplTranslationProvider.this.authorizationKey, text, sourceLanguage,
                                                                                targetLanguage);

                        Optional<String> result = Optional.ofNullable(response)
                                                          .map(TranslationResponse::getTranslations)
                                                          .filter(ListUtils::isNotEmpty)
                                                          .flatMap(ListUtils::optionalFirst)
                                                          .map(Translation::getText);

                        LOG.info("Resolved translation from Deepl: " + key + " " + sourceLanguage + "->" + targetLanguage + " " + text + " -> "
                                + result.orElse(""));

                        this.resolveAndLogUsedDeeplCapacity(license);

                        return result;
                    }
                    catch (Exception e)
                    {
                        LOG.error("Exception during deepl API request for " + key + " " + sourceLanguage + "->" + targetLanguage + " " + text, e);
                        return Optional.empty();
                    }
                }
            }

            private void resolveAndLogUsedDeeplCapacity(License license)
            {
                Usage usage = DeeplRestUtils.getUsage(license, DeeplTranslationProvider.this.authorizationKey);
                double alreadyUsedCapacity = usage.getCharacterCount() / (1.0 * usage.getCharacterLimit());
                LOG.info("Already used translation capacity of Deepl: " + NumberUtils.formatter()
                                                                                     .asPercentage()
                                                                                     .format(alreadyUsedCapacity)
                        + "% ( " + usage.getCharacterCount() + "/" + usage.getCharacterLimit() + " )");
            }
        });
    }

    @Override
    public boolean isSlow()
    {
        return true;
    }

}
