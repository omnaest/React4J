package org.omnaest.react4j.domain.provider;

import java.net.URL;
import java.util.Locale;
import java.util.Optional;

public interface WebDomainContext
{
    /**
     * Returns the public url the users are addressing
     * 
     * @return
     */
    public Optional<URL> getPublicDomainUrl();

    /**
     * Returns the public url which is language specific
     * 
     * @param locale
     * @return
     */
    public Optional<URL> getPublicDomainUrl(Locale locale);
}
