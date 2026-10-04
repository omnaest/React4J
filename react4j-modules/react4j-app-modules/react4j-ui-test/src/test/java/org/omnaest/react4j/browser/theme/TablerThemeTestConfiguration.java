package org.omnaest.react4j.browser.theme;

import org.omnaest.react4j.domain.configuration.ThemeConfiguration.ThemePreset;
import org.omnaest.react4j.service.ReactUIService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * plan-277 T4: the test-only way to run {@link org.omnaest.react4j.MockApplication} on the Tabler preset in its default (light) colour mode with no
 * design tokens at all, selected by activating the Spring profile {@value #PROFILE}. Configures through the public
 * {@link ReactUIService#configureTheme(java.util.function.Consumer)} seam like an application would and deliberately adds no second
 * {@code ReactUIProvider}. Because it sets no token, what the browser renders is Tabler's own look. Activating it suite-wide
 * ({@code -Dspring.profiles.active=theme-tabler}) is how the whole IT suite is run on Tabler.
 */
@Configuration
@Profile(TablerThemeTestConfiguration.PROFILE)
public class TablerThemeTestConfiguration
{
    public static final String PROFILE = "theme-tabler";

    @Bean
    InitializingBean tablerTheme(ReactUIService reactUIService)
    {
        return () -> reactUIService.configureTheme(theme -> theme.preset(ThemePreset.TABLER));
    }
}
