package org.omnaest.react4j.browser.theme;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.omnaest.react4j.MockApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ActiveProfiles;

/**
 * plan-277 T4: the same showcase page under the MODERN and the TABLER theme, at the same viewport, captured in ONE browser session and stitched side by
 * side (MODERN left, TABLER right). Writes {@code docs/theme-screenshots/tabler/showcase-modern.png}, {@code showcase-tabler.png} and the composite
 * {@code showcase-modern-vs-tabler.png}. The composite is put together on the pixels ({@link #persistSideBySide}), because the two images are taller than a
 * browser can render into one screenshot.
 * <p>
 * This context is the MODERN one, so the class pins a profile of its own ({@value #MODERN_PINNED_PROFILE}, which no configuration reacts to): an
 * {@link ActiveProfiles} declaration takes precedence over an external {@code -Dspring.profiles.active}, so the whole-suite run on Tabler cannot turn the
 * left half into a second Tabler. The TABLER half is a second, in-process {@link MockApplication} started with {@link TablerThemeTestConfiguration#PROFILE},
 * so neither image depends on another test class having run first. Both halves assert one load-bearing computed value so that this is not a
 * screenshot-only test.
 */
@Tag("browser")
@SpringBootTest(classes = MockApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles({ShowcaseScreenshotTablerComparisonIT.MODERN_PINNED_PROFILE, ShowcaseScreenshotSupport.ISOLATED_CONTEXT_PROFILE})
public class ShowcaseScreenshotTablerComparisonIT extends ShowcaseScreenshotSupport
{
    static final String MODERN_PINNED_PROFILE = "theme-modern-pinned";

    @Test
    public void testModernAndTablerShowcaseAreCapturedSideBySideAtTheSameViewport() throws IOException
    {
        Path modern = this.openShowcaseAndPersistScreenshot("tabler/showcase-modern.png");
        assertEquals("rgb(79, 70, 229)", computedStyle(this.openModalButton(), "background-color"), "the left half must be the MODERN theme");
        assertTrue(this.fetchServedIndexHtml()
                       .contains("/css/theme/react4j-modern.css?"));

        Path tabler;
        try (ConfigurableApplicationContext tablerApplication = new SpringApplicationBuilder(MockApplication.class).profiles(TablerThemeTestConfiguration.PROFILE)
                                                                                                                   .properties("server.port=0")
                                                                                                                   .run())
        {
            String tablerBaseUrl = "http://localhost:" + tablerApplication.getEnvironment()
                                                                          .getProperty("local.server.port");
            tabler = persistShowcaseScreenshot(this.page, tablerBaseUrl, "tabler/showcase-tabler.png");
            assertTrue(this.page.request()
                                .get(tablerBaseUrl + "/")
                                .text()
                                .contains("/css/theme/react4j-tabler.css?"),
                       "the right half must be served the TABLER sheet");
            assertTrue(!"rgb(79, 70, 229)".equals(computedStyle(this.openModalButton(), "background-color")), "the right half must not carry the MODERN accent");
        }

        Path composite = persistSideBySide(modern, "MODERN (default)", tabler, "TABLER", "tabler/showcase-modern-vs-tabler.png");

        assertTrue(Files.size(modern) > 10_000 && Files.size(tabler) > 10_000 && Files.size(composite) > Files.size(modern), "screenshots are suspiciously small");
    }
}
