package org.omnaest.react4j.browser.theme;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.omnaest.react4j.MockApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;

/**
 * plan-277 T4: full-page screenshot of the showcase under the Tabler preset in DARK colour mode ({@link TablerDarkThemeTestConfiguration#PROFILE}), plus
 * its load-bearing computed values: {@code data-bs-theme="dark"} on {@code <html>}, a dark page background and Tabler's own stylesheet in use.
 * Persisted as {@code docs/theme-screenshots/tabler/showcase-tabler-dark.png}.
 */
@Tag("browser")
@SpringBootTest(classes = MockApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles({TablerDarkThemeTestConfiguration.PROFILE, ShowcaseScreenshotSupport.ISOLATED_CONTEXT_PROFILE})
public class ShowcaseScreenshotTablerDarkIT extends ShowcaseScreenshotSupport
{
    @Test
    public void testTablerDarkShowcaseIsCapturedInDarkModeOnADarkPage() throws IOException
    {
        Path screenshot = this.openShowcaseAndPersistScreenshot("tabler/showcase-tabler-dark.png");

        String background = this.bodyBackgroundColor();
        assertTrue(Files.size(screenshot) > 10_000, "screenshot is suspiciously small: " + screenshot);
        assertEquals("dark", this.htmlColorMode());
        assertTrue(BrowserColors.luminance(BrowserColors.srgbBytes(this.page, background)) < 0.05, "expected a dark page background but was " + background);
        assertTrue(this.fetchServedIndexHtml()
                       .contains("/css/theme/react4j-tabler.css?"),
                   "freshness: the served page must link the Tabler sheet");
    }
}
