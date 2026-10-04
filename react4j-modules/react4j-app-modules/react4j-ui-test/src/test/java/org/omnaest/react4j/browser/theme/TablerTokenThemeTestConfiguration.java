package org.omnaest.react4j.browser.theme;

import org.omnaest.react4j.domain.configuration.ThemeConfiguration.ThemePreset;
import org.omnaest.react4j.service.ReactUIService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * plan-277 T3: the test-only way to run {@link org.omnaest.react4j.MockApplication} on the Tabler preset WITH design tokens, selected by activating the
 * Spring profile {@value #PROFILE}. A teal primary colour and a 12px base radius are configured through the public
 * {@link ReactUIService#configureTheme(java.util.function.Consumer)} seam like an application would; no second {@code ReactUIProvider} is added. The
 * Tabler counterpart of {@link TokenThemeTestConfiguration}, which stays untouched.
 */
@Configuration
@Profile(TablerTokenThemeTestConfiguration.PROFILE)
public class TablerTokenThemeTestConfiguration
{
    public static final String PROFILE = "theme-tabler-tokens";

    public static final String PRIMARY = "#0f766e";
    public static final String RADIUS  = "12px";

    @Bean
    InitializingBean tablerTokenTheme(ReactUIService reactUIService)
    {
        return () -> reactUIService.configureTheme(theme -> theme.preset(ThemePreset.TABLER)
                                                                 .primaryColor(PRIMARY)
                                                                 .borderRadius(RADIUS));
    }
}
