package org.omnaest.react4j.service.internal.service;

import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.i18n.I18nText;
import org.omnaest.react4j.service.internal.nodes.i18n.I18nTextValue;

public interface LocalizedTextResolverService extends BiFunction<I18nText, Location, I18nTextValue>
{
    public static final String DEFAULT_LOCALE_KEY = "DEFAULT";

    public default List<I18nTextValue> apply(List<I18nText> texts, Location location)
    {
        return texts.stream()
                    .map(text -> apply(text, location))
                    .collect(Collectors.toList());
    }
}
