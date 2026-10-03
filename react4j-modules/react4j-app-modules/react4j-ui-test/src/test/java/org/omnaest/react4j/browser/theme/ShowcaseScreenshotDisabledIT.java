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
 * plan-274 S5: full-page screenshot of the showcase with the theme DISABLED (stock Bootstrap only), plus its load-bearing computed values: no colour
 * mode attribute at all, a white page and the stock Bootstrap primary.
 */
@Tag("browser")
@SpringBootTest(classes = MockApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles(DisabledThemeTestConfiguration.PROFILE)
public class ShowcaseScreenshotDisabledIT extends ShowcaseScreenshotSupport
{
    @Test
    public void testDisabledShowcaseIsCapturedWithoutColorModeOnAWhitePageInStockPrimary() throws IOException
    {
        Path screenshot = this.openShowcaseAndPersistScreenshot("showcase-disabled.png");

        assertTrue(Files.size(screenshot) > 10_000, "screenshot is suspiciously small: " + screenshot);
        assertNull(this.htmlColorMode());
        assertEquals("rgb(255, 255, 255)", this.bodyBackgroundColor());
        assertEquals("rgb(13, 110, 253)", computedStyle(this.openModalButton(), "background-color"));
    }
}
