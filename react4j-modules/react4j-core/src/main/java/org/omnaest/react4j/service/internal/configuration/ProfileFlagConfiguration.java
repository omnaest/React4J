package org.omnaest.react4j.service.internal.configuration;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class ProfileFlagConfiguration
{
    private static final String PROFILE_NAME_NO_UI_CACHE = "React4JNoUICache";

    @Qualifier
    public static @interface UICacheEnabledFlag
    {
    }

    @Bean
    @Profile(PROFILE_NAME_NO_UI_CACHE)
    @UICacheEnabledFlag
    public boolean uiCacheDisabledFlag()
    {
        return false;
    }

    @Bean
    @Profile("!" + PROFILE_NAME_NO_UI_CACHE)
    @UICacheEnabledFlag
    public boolean uiCacheEnabledFlag()
    {
        return true;
    }

}
