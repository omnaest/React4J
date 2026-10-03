package org.omnaest.react4j.browser.theme;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
 * plan-274 S5: full-page screenshot of the showcase under the modern theme in DARK colour mode ({@link DarkThemeTestConfiguration#PROFILE}), plus its
 * load-bearing computed values: {@code data-bs-theme="dark"} on {@code <html>} and a page background that is dark and differs from the light one.
 */
@Tag("browser")
@SpringBootTest(classes = MockApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles(DarkThemeTestConfiguration.PROFILE)
public class ShowcaseScreenshotModernDarkIT extends ShowcaseScreenshotSupport
{
    @Test
    public void testModernDarkShowcaseIsCapturedInDarkModeOnADarkPage() throws IOException
    {
        Path screenshot = this.openShowcaseAndPersistScreenshot("showcase-modern-dark.png");

        String background = this.bodyBackgroundColor();
        assertTrue(Files.size(screenshot) > 10_000, "screenshot is suspiciously small: " + screenshot);
        assertEquals("dark", this.htmlColorMode());
        assertTrue(!"rgb(247, 248, 250)".equals(background) && channelSum(background) < 200, "expected a dark page background but was " + background);
    }
}
