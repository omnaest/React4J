package org.omnaest.react4j.browser.theme;

import org.omnaest.react4j.domain.configuration.ThemeConfiguration;
import org.omnaest.react4j.service.ReactUIService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * plan-274 S3: the test-only way to run {@link org.omnaest.react4j.MockApplication} with the theme DISABLED, selected by activating the Spring
 * profile {@value #PROFILE} on a test class. It configures through the public {@link ReactUIService#configureTheme(java.util.function.Consumer)}
 * seam, exactly like an application would; it deliberately does not add a second {@code ReactUIProvider} (a second provider on the same path is
 * silently dropped, first writer wins).
 * <br>
 * <br>
 * A stylesheet and a colour mode are configured BEFORE the {@code disable()} to prove in the browser tests that disabled really means "stock
 * Bootstrap only".
 */
@Configuration
@Profile(DisabledThemeTestConfiguration.PROFILE)
public class DisabledThemeTestConfiguration
{
    public static final String PROFILE               = "theme-disabled";

    /**
     * Configured, but must never reach the page while the theme is disabled
     */
    public static final String SUPPRESSED_STYLESHEET = "/css/theme-it-suppressed-sheet.css";

    @Bean
    InitializingBean disableTheme(ReactUIService reactUIService)
    {
        return () -> reactUIService.configureTheme(theme -> theme.colorMode(ThemeConfiguration.ColorMode.DARK)
                                                                 .addStylesheet(SUPPRESSED_STYLESHEET)
                                                                 .disable());
    }
}
