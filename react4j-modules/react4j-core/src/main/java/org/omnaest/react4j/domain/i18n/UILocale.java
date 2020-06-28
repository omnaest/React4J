package org.omnaest.react4j.domain.i18n;

import java.util.Locale;

public interface UILocale
{
    public Locale asLocale();

    /**
     * Returns e.g. en_US
     * 
     * @return
     */
    public String asLanguageTag();

    public static UILocale of(Locale locale)
    {
        return new UILocale()
        {
            @Override
            public Locale asLocale()
            {
                return locale;
            }

            @Override
            public String asLanguageTag()
            {
                return this.asLocale()
                           .toLanguageTag();
            }
        };
    }

}
