package org.omnaest.react.internal.service.internal;

import java.util.Map;
import java.util.Optional;

import org.omnaest.react.domain.Location;
import org.omnaest.react.domain.i18n.I18nText;
import org.omnaest.react.internal.nodes.i18n.I18nTextValue;
import org.omnaest.react.internal.service.LocalizedTextResolverService;
import org.omnaest.utils.MapUtils;
import org.springframework.stereotype.Service;

@Service
public class LocalizedTextResolverServiceImpl implements LocalizedTextResolverService
{
    @Override
    public I18nTextValue apply(I18nText i18nText, Location location)
    {
        Map<String, String> localeToText = MapUtils.builder()
                                                   .put("DEFAULT", Optional.ofNullable(i18nText)
                                                                           .map(I18nText::getDefaultText)
                                                                           .orElse(""))
                                                   .build();
        return new I18nTextValue(localeToText);
    }
}
