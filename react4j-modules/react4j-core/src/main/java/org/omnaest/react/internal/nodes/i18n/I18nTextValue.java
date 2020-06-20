package org.omnaest.react.internal.nodes.i18n;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public class I18nTextValue
{
    private Map<String, String> localeToText;

    public I18nTextValue()
    {
        super();
        this.localeToText = new HashMap<>();
    }

    @JsonCreator
    public I18nTextValue(Map<String, String> localeToText)
    {
        super();
        this.localeToText = localeToText;
    }

    @JsonValue
    public Map<String, String> getLocaleToText()
    {
        return this.localeToText;
    }

    @Override
    public String toString()
    {
        return "I18nTextValue [localeToText=" + this.localeToText + "]";
    }

}
