package org.omnaest.react4j.browser.theme;

import org.junit.jupiter.api.Tag;
import org.omnaest.react4j.MockApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;

/**
 * plan-277 F3: the showcase coverage checks ({@link ShowcaseCoverageChecks}) under the Tabler preset ({@link TablerThemeTestConfiguration}).
 */
@Tag("browser")
@SpringBootTest(classes = MockApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles(TablerThemeTestConfiguration.PROFILE)
public class TablerShowcaseCoverageIT extends ShowcaseCoverageChecks
{
}
