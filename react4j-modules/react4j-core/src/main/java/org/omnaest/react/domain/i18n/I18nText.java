package org.omnaest.react.domain.i18n;

import java.util.Map;

import org.omnaest.react.domain.Location;
import org.omnaest.react.domain.Locations;

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
