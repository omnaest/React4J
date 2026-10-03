package org.omnaest.react4j.browser.theme;

import org.omnaest.react4j.service.ReactUIService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * plan-274 S4: the test-only way to run {@link org.omnaest.react4j.MockApplication} with design tokens, selected by activating the Spring profile
 * {@value #PROFILE}. A teal primary colour and a 12px base radius are configured through the public
 * {@link ReactUIService#configureTheme(java.util.function.Consumer)} seam like an application would; no second {@code ReactUIProvider} is added.
 */
@Configuration
@Profile(TokenThemeTestConfiguration.PROFILE)
public class TokenThemeTestConfiguration
{
    public static final String PROFILE = "theme-tokens";

    public static final String PRIMARY = "#0f766e";
    public static final String RADIUS  = "12px";

    @Bean
    InitializingBean tokenTheme(ReactUIService reactUIService)
    {
        return () -> reactUIService.configureTheme(theme -> theme.primaryColor(PRIMARY)
                                                                 .borderRadius(RADIUS));
    }
}
