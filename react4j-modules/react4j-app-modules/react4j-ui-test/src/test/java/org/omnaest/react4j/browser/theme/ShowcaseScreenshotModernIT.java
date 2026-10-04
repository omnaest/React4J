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
 * plan-274 S5: full-page screenshot of the showcase under the DEFAULT theme (modern, light), plus its load-bearing computed values: the light colour
 * mode on {@code <html>}, the modern page background {@code #f7f8fa} and the modern accent on the primary button.
 */
@Tag("browser")
@SpringBootTest(classes = MockApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles(ShowcaseScreenshotSupport.ISOLATED_CONTEXT_PROFILE)
public class ShowcaseScreenshotModernIT extends ShowcaseScreenshotSupport
{
    @Test
    public void testModernShowcaseIsCapturedInLightModeWithTheModernBackgroundAndAccent() throws IOException
    {
        Path screenshot = this.openShowcaseAndPersistScreenshot("showcase-modern.png");

        assertTrue(Files.size(screenshot) > 10_000, "screenshot is suspiciously small: " + screenshot);
        assertEquals("light", this.htmlColorMode());
        assertEquals("rgb(247, 248, 250)", this.bodyBackgroundColor());
        assertEquals("rgb(79, 70, 229)", computedStyle(this.openModalButton(), "background-color"));
    }
}
