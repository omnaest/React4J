package org.omnaest.react4j.browser.theme;

import org.junit.jupiter.api.Tag;
import org.omnaest.react4j.MockApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

/**
 * plan-277 F3: the showcase coverage checks ({@link ShowcaseCoverageChecks}) under the default theme, modern light. {@link MockApplication} configures no
 * theme.
 */
@Tag("browser")
@SpringBootTest(classes = MockApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
public class ModernShowcaseCoverageIT extends ShowcaseCoverageChecks
{
}
