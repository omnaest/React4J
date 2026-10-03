package org.omnaest.react4j.browser.theme;

import org.omnaest.react4j.domain.configuration.ThemeConfiguration;
import org.omnaest.react4j.service.ReactUIService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * plan-274 S5: the test-only way to run {@link org.omnaest.react4j.MockApplication} with the modern theme in DARK colour mode, selected by activating
 * the Spring profile {@value #PROFILE}. Configures through the public {@link ReactUIService#configureTheme(java.util.function.Consumer)} seam like
 * an application would, and deliberately adds no second {@code ReactUIProvider}.
 */
@Configuration
@Profile(DarkThemeTestConfiguration.PROFILE)
public class DarkThemeTestConfiguration
{
    public static final String PROFILE = "theme-dark";

    @Bean
    InitializingBean darkTheme(ReactUIService reactUIService)
    {
        return () -> reactUIService.configureTheme(theme -> theme.colorMode(ThemeConfiguration.ColorMode.DARK));
    }
}
