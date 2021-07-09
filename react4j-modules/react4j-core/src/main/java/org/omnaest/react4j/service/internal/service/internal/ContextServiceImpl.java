package org.omnaest.react4j.service.internal.service.internal;

import java.net.URL;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.omnaest.react4j.service.internal.controller.utils.RequestProvider;
import org.omnaest.react4j.service.internal.service.ContextService;
import org.omnaest.react4j.service.internal.service.internal.translation.component.LocaleService;
import org.omnaest.utils.URLUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ContextServiceImpl implements ContextService
{
    @Autowired
    private LocaleService localeService;

    @Autowired
    private RequestProvider requestProvider;

    @Value("${react4j.public-url:#{null}}")
    private Optional<String> publicUrl;

    @Override
    public Set<Locale> getAvailableLocales()
    {
        return this.localeService.getAvailableLocales();
    }

    @Override
    public Locale getCurrentUserLocale()
    {
        return this.localeService.getRequestLocaleOrDefault();
    }

    @Override
    public Optional<URL> getPublicDomainUrl()
    {
        return URLUtils.from(this.generatePublicRootUrl())
                       .asUrl();
    }

    private String generatePublicRootUrl()
    {
        return this.generatePublicRootUrl(this.localeService.getRequestLocaleOrDefault()
                                                            .toLanguageTag());
    }

    private String generatePublicRootUrl(String currentLanguageTag)
    {
        return this.publicUrl.orElseGet(() -> this.requestProvider.getRequestBaseUrl()
                                                                  .map(requestUrl -> StringUtils.removeEndIgnoreCase(requestUrl, currentLanguageTag))
                                                                  .orElse(""));
    }

    @Override
    public Optional<URL> getPublicDomainUrl(Locale locale)
    {
        return URLUtils.from(this.generatePublicRootUrl() + "/" + locale.toLanguageTag())
                       .asUrl();
    }
}
