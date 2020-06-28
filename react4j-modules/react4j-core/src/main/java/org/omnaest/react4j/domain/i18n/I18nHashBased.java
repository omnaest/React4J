package org.omnaest.react4j.domain.i18n;

import java.util.Map;
import java.util.stream.Collectors;

import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.Locations;

/**
 * @see I18nText#of(Locations, String, UILocale)
 * @author omnaest
 */
class I18nHashBased implements I18nText
{
    private String    text;
    private UILocale  locale;
    private Locations locations;

    public I18nHashBased(String text, Locations location, UILocale locale)
    {
        super();
        this.text = text;
        this.locations = location;
        this.locale = locale;
    }

    @Override
    public Map<Location, String> getKeys()
    {
        return this.locations.get()
                             .stream()
                             .collect(Collectors.toMap(location -> location, location -> location.get()
                                                                                                 .stream()
                                                                                                 .map(token -> token.replaceAll("[^a-zA-Z0-9]", "_"))
                                                                                                 .collect(Collectors.joining("."))
                                     + "." + this.text.hashCode()));
    }

    @Override
    public String getDefaultText()
    {
        return this.text;
    }

    @Override
    public UILocale getDefaultLocale()
    {
        return this.locale;
    }

    @Override
    public String toString()
    {
        return "I18nHashBased [text=" + this.text + ", locale=" + this.locale + ", locations=" + this.locations + "]";
    }

}