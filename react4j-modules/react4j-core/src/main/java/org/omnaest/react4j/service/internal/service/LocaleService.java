package org.omnaest.react4j.service.internal.service;

import java.util.Locale;
import java.util.Optional;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

@Service
public class LocaleService
{
    public Optional<String> getRequestLocaleKey()
    {
        return Optional.ofNullable(LocaleContextHolder.getLocale())
                       .map(Locale::toLanguageTag);
    }
}
