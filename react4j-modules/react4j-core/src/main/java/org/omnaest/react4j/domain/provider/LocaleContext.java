package org.omnaest.react4j.domain.provider;

import java.util.Locale;
import java.util.Set;

public interface LocaleContext
{
    /**
     * Returns all available {@link Locale}s that can be rendered
     * 
     * @return
     */
    public Set<Locale> getAvailableLocales();

    /**
     * Returns the {@link Locale} choosen by the user or the default {@link Locale}
     * 
     * @return
     */
    public Locale getCurrentUserLocale();
}
