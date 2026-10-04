package org.omnaest.react4j.browser.theme;

import org.junit.jupiter.api.Tag;
import org.omnaest.react4j.MockApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;

/**
 * plan-277 F3: the DiagramViewer dark band checks ({@link DiagramViewerDarkBandChecks}) under the modern theme in dark colour mode
 * ({@link DarkThemeTestConfiguration#PROFILE}).
 */
@Tag("browser")
@SpringBootTest(classes = MockApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles(DarkThemeTestConfiguration.PROFILE)
public class ModernDarkDiagramViewerBandIT extends DiagramViewerDarkBandChecks
{
}
