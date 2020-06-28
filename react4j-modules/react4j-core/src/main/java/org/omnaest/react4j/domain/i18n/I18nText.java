package org.omnaest.react4j.domain.i18n;

import java.util.Map;

import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.Locations;

public interface I18nText
{
    public Map<Location, String> getKeys();

    public String getDefaultText();

    public UILocale getDefaultLocale();

    public static I18nText of(Locations locations, String text, UILocale locale)
    {
        return new I18nHashBased(text, locations, locale);
    }
}
