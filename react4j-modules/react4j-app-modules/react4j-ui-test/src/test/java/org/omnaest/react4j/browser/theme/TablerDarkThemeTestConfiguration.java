package org.omnaest.react4j.browser.theme;

import org.omnaest.react4j.domain.configuration.ThemeConfiguration.ColorMode;
import org.omnaest.react4j.domain.configuration.ThemeConfiguration.ThemePreset;
import org.omnaest.react4j.service.ReactUIService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * plan-277 T4: the dark variant of {@link TablerThemeTestConfiguration}: the Tabler preset in {@link ColorMode#DARK}, selected by activating the Spring
 * profile {@value #PROFILE}. The Tabler counterpart of {@link DarkThemeTestConfiguration}, which stays untouched.
 */
@Configuration
@Profile(TablerDarkThemeTestConfiguration.PROFILE)
public class TablerDarkThemeTestConfiguration
{
    public static final String PROFILE = "theme-tabler-dark";

    @Bean
    InitializingBean tablerDarkTheme(ReactUIService reactUIService)
    {
        return () -> reactUIService.configureTheme(theme -> theme.preset(ThemePreset.TABLER)
                                                                 .colorMode(ColorMode.DARK));
    }
}
